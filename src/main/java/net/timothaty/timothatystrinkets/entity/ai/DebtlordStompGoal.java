package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.DebtlordEntity;
import net.timothaty.timothatystrinkets.entity.DebtlordGroundDebrisEntity;
import net.timothaty.timothatystrinkets.entity.DebtlordPhase;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.debtlord.DebtlordDamageScaling;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DebtlordStompGoal extends Goal {
	public static final int CAST_DURATION_TICKS = 4 * 20;
	public static final float FAST_ANIMATION_SPEED = 1.25F;
	private static final float STOMP_IMPACT_TIME_SECONDS = 2.1667F;
	private static final float TICKS_PER_SECOND = 20.0F;
	private static final int COMBO_DELAY_TICKS = 10;
	public static final int COOLDOWN_TICKS = 7 * 20;
	private static final int PHASE_TWO_COOLDOWN_TICKS = 6 * 20;
	private static final int PHASE_THREE_COOLDOWN_TICKS = 5 * 20;
	public static final int STUN_DURATION_TICKS = 3 * 20;
	public static final int PLAYER_STUN_DURATION_TICKS = 2 * 20;
	public static final int SLOW_DURATION_TICKS = 4 * 20;
	public static final int SLOW_AMPLIFIER = 2;
	public static final float MAGIC_DAMAGE = 4.0F;
	public static final double EFFECT_RADIUS = 7.0D;
	private static final double ENRAGED_RADIUS_BONUS = 7.0D;
	private static final float BASE_LIFE_STEAL_RATIO = 0.2F;
	private static final float ENRAGED_LIFE_STEAL_RATIO = 0.8F;
	private static final double ENRAGED_LAUNCH_VELOCITY = 0.8D;
	private static final double VERTICAL_RANGE_BELOW = 1.5D;
	private static final double VERTICAL_RANGE_ABOVE = 3.5D;
	private static final int[][] FLAT_IMPACT_AREA_OFFSETS = {
		{0, 0},
		{1, 0},
		{-1, 0},
		{0, 1},
		{0, -1}
	};

	private final DebtlordEntity debtlord;
	private long nextAvailableGameTime;
	private LivingEntity primaryTarget;
	private int castDurationTicks;
	private long comboFinishGameTime;

	public DebtlordStompGoal(DebtlordEntity debtlord) {
		this.debtlord = debtlord;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		if (!debtlord.isAlive()
			|| debtlord.isUsingAbility()
			|| !debtlord.onGround()
			|| debtlord.isTouchingWaterForBossLogic()
			|| !debtlord.wantsCombatIntent(DebtlordEntity.CombatIntent.STOMP)
			|| debtlord.level().getGameTime() < nextAvailableGameTime)
			return false;
		if (debtlord.level() instanceof ServerLevel serverLevel && !hasFlatImpactArea(serverLevel, getLeftHoofImpactPosition(debtlord)))
			return false;

		LivingEntity target = findEligibleStompTarget();
		if (target == null)
			return false;
		if (target != debtlord.getTarget())
			debtlord.setTarget(target);
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return debtlord.isAlive() && debtlord.isStomping();
	}

	@Override
	public void start() {
		primaryTarget = debtlord.getTarget();
		boolean fast = getStompAnimationSpeed(debtlord.getPhase()) > 1.0F;
		castDurationTicks = getStompCastDurationTicks(fast);
		comboFinishGameTime = Long.MAX_VALUE;
		debtlord.startStompCast(castDurationTicks, fast);
		debtlord.level().playSound(null, debtlord.blockPosition(), TimothatysTrinketsModSounds.STOMP_PREPARATION.get(), SoundSource.HOSTILE, 1.25F, 1.0F);
	}

	@Override
	public void tick() {
		debtlord.lockAbilityPosition();
		int remainingTicks = debtlord.getStompCastTicks();
		if (remainingTicks <= 0)
			return;

		int elapsedTicks = castDurationTicks - remainingTicks + 1;
		boolean fast = debtlord.isCurrentStompFast();
		if (elapsedTicks == getStompImpactTick(fast)) {
			playImpactSound(debtlord);
			StompImpactResult result = performImpact(debtlord, primaryTarget);
			if (result.primaryTargetHit())
				comboFinishGameTime = debtlord.level().getGameTime() + COMBO_DELAY_TICKS;
		}

		if (debtlord.level().getGameTime() >= comboFinishGameTime) {
			LivingEntity hitTarget = primaryTarget;
			debtlord.finishStompCast();
			if (isEligibleStompTarget(debtlord, hitTarget)) {
				debtlord.setTarget(hitTarget);
				debtlord.offerClawFollowup(hitTarget, 0, DebtlordClawGoal.FOLLOWUP_WINDOW_TICKS, DebtlordClawFollowupQueue.Reason.STOMP_COMBO);
			}
			return;
		}

		if (remainingTicks <= 1)
			debtlord.finishStompCast();
		else
			debtlord.setStompCastTicks(remainingTicks - 1);
	}

	@Override
	public void stop() {
		debtlord.finishStompCast();
		nextAvailableGameTime = debtlord.level().getGameTime() + getCooldownTicks();
		primaryTarget = null;
		castDurationTicks = 0;
		comboFinishGameTime = Long.MAX_VALUE;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	private boolean isTargetInRange(LivingEntity target) {
		if (!isEligibleStompTarget(target))
			return false;

		double dx = target.getX() - debtlord.getX();
		double dz = target.getZ() - debtlord.getZ();
		double feetDelta = target.getBoundingBox().minY - debtlord.getBoundingBox().minY;
		double targetingRadius = getCurrentEffectRadius();
		return dx * dx + dz * dz <= targetingRadius * targetingRadius
			&& feetDelta >= -VERTICAL_RANGE_BELOW
			&& feetDelta <= VERTICAL_RANGE_ABOVE;
	}

	private LivingEntity findEligibleStompTarget() {
		LivingEntity currentTarget = debtlord.getTarget();
		if (isTargetInRange(currentTarget))
			return currentTarget;
		if (!(debtlord.level() instanceof ServerLevel serverLevel))
			return null;

		double targetingRadius = getCurrentEffectRadius();
		List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(
			LivingEntity.class,
			debtlord.getBoundingBox().inflate(targetingRadius, VERTICAL_RANGE_ABOVE, targetingRadius),
			this::isTargetInRange
		);
		LivingEntity nearest = null;
		double nearestDistanceSqr = Double.MAX_VALUE;
		for (LivingEntity candidate : nearby) {
			double distanceSqr = debtlord.distanceToSqr(candidate);
			if (distanceSqr < nearestDistanceSqr) {
				nearestDistanceSqr = distanceSqr;
				nearest = candidate;
			}
		}
		return nearest;
	}

	private double getCurrentEffectRadius() {
		return EFFECT_RADIUS + (debtlord.isEnraged() ? ENRAGED_RADIUS_BONUS : 0.0D);
	}

	private int getCooldownTicks() {
		return switch (debtlord.getPhase()) {
			case PHASE_THREE -> PHASE_THREE_COOLDOWN_TICKS;
			case PHASE_TWO -> PHASE_TWO_COOLDOWN_TICKS;
			case PHASE_ONE -> COOLDOWN_TICKS;
		};
	}

	private boolean isEligibleStompTarget(LivingEntity target) {
		return isEligibleStompTarget(debtlord, target);
	}

	private static boolean isEligibleStompTarget(DebtlordEntity debtlord, LivingEntity target) {
		return target != null
			&& target != debtlord
			&& target.isAlive()
			&& !target.isRemoved()
			&& target.level() == debtlord.level()
			&& !debtlord.isAlliedTo(target)
			&& !DebtlordEntity.isEntityTouchingWater(target)
			&& !TimothatysTrinketsStunHelper.isMechanicallyImmunePlayer(target)
			&& !target.hasEffect(TimothatysTrinketsModMobEffects.STUN_IMMUNITY)
			&& !target.getType().is(TimothatysTrinketsStunTags.STUN_IMMUNE);
	}

	public static StompImpactResult performImpact(DebtlordEntity debtlord, LivingEntity primaryTarget) {
		if (!(debtlord.level() instanceof ServerLevel serverLevel))
			return StompImpactResult.MISS;
		if (debtlord.isTouchingWaterForBossLogic())
			return StompImpactResult.MISS;

		boolean enraged = debtlord.isEnraged();
		double effectRadius = EFFECT_RADIUS + (enraged ? ENRAGED_RADIUS_BONUS : 0.0D);
		double effectRadiusSqr = effectRadius * effectRadius;
		float lifeStealRatio = enraged ? ENRAGED_LIFE_STEAL_RATIO : BASE_LIFE_STEAL_RATIO;
		Vec3 origin = getLeftHoofImpactPosition(debtlord);
		if (!hasFlatImpactArea(serverLevel, origin))
			return StompImpactResult.MISS;
		spawnImpactEffects(serverLevel, origin, effectRadius);
		spawnGroundDebrisWave(serverLevel, origin, effectRadius);

		AABB effectBounds = new AABB(
			origin.x - effectRadius,
			origin.y - VERTICAL_RANGE_BELOW,
			origin.z - effectRadius,
			origin.x + effectRadius,
			origin.y + VERTICAL_RANGE_ABOVE,
			origin.z + effectRadius
		);
		List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, effectBounds, target -> isValidImpactTarget(debtlord, target));
		boolean hitAnyTarget = false;
		boolean primaryTargetHit = false;
		for (LivingEntity target : targets) {
			double dx = target.getX() - origin.x;
			double dz = target.getZ() - origin.z;
			if (dx * dx + dz * dz > effectRadiusSqr || canDodgeByJumping(origin.y, target))
				continue;

			float appliedMultiplier = DebtlordDamageScaling.getAppliedMultiplier(target, DebtlordDamageScaling.STOMP_IRON_GOLEM_MULTIPLIER);
			float effectiveHealthBefore = DebtlordDamageScaling.getEffectiveHealth(target);
			boolean damaged = target.hurt(debtlord.damageSources().magic(), MAGIC_DAMAGE * appliedMultiplier);
			float actualDamage = DebtlordDamageScaling.getActualDamage(target, effectiveHealthBefore);
			float lifeStealDamage = DebtlordDamageScaling.normalizeDamageForLifeSteal(target, actualDamage, appliedMultiplier);
			if (lifeStealDamage > 0.0F)
				debtlord.heal(lifeStealDamage * lifeStealRatio);
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_DURATION_TICKS, SLOW_AMPLIFIER, false, true, true), debtlord);
			TimothatysTrinketsStunHelper.tryApplyStunSilently(target, debtlord, STUN_DURATION_TICKS, PLAYER_STUN_DURATION_TICKS);
			if (damaged && actualDamage > 0.0F) {
				hitAnyTarget = true;
				if (target == primaryTarget)
					primaryTargetHit = true;
			}
			if (enraged) {
				Vec3 movement = target.getDeltaMovement();
				target.setDeltaMovement(movement.x, Math.max(movement.y, ENRAGED_LAUNCH_VELOCITY), movement.z);
				target.hurtMarked = true;
			}
		}
		return new StompImpactResult(hitAnyTarget, primaryTargetHit, primaryTargetHit ? primaryTarget : null);
	}

	public static float getStompAnimationSpeed(DebtlordPhase phase) {
		return phase == DebtlordPhase.PHASE_ONE ? 1.0F : FAST_ANIMATION_SPEED;
	}

	public static int getStompCastDurationTicks(boolean fast) {
		return Math.max(1, Math.round(CAST_DURATION_TICKS / (fast ? FAST_ANIMATION_SPEED : 1.0F)));
	}

	public static int getStompImpactTick(boolean fast) {
		float speed = fast ? FAST_ANIMATION_SPEED : 1.0F;
		return Math.max(1, Math.round(STOMP_IMPACT_TIME_SECONDS * TICKS_PER_SECOND / speed));
	}

	public static void playImpactSound(DebtlordEntity debtlord) {
		Vec3 origin = getLeftHoofImpactPosition(debtlord);
		if (debtlord.level() instanceof ServerLevel serverLevel && !hasFlatImpactArea(serverLevel, origin))
			return;
		debtlord.level().playSound(null, origin.x, origin.y, origin.z, TimothatysTrinketsModSounds.HOOF_STOMP.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
	}

	private static boolean isValidImpactTarget(DebtlordEntity debtlord, LivingEntity target) {
		return isEligibleStompTarget(debtlord, target);
	}

	private static boolean canDodgeByJumping(double impactY, LivingEntity target) {
		if (target.onGround())
			return false;

		double feetAboveImpactFloor = target.getBoundingBox().minY - impactY;
		return feetAboveImpactFloor > 0.55D || target.getDeltaMovement().y > 0.08D;
	}

	private static Vec3 getLeftHoofImpactPosition(DebtlordEntity debtlord) {
		float yawRadians = debtlord.getYRot() * Mth.DEG_TO_RAD;
		double leftX = Mth.cos(yawRadians);
		double leftZ = Mth.sin(yawRadians);
		double forwardX = -Mth.sin(yawRadians);
		double forwardZ = Mth.cos(yawRadians);
		return new Vec3(
			debtlord.getX() + leftX * 0.47D + forwardX * 0.30D,
			debtlord.getY() + 0.05D,
			debtlord.getZ() + leftZ * 0.47D + forwardZ * 0.30D
		);
	}

	private static void spawnImpactEffects(ServerLevel serverLevel, Vec3 origin, double effectRadius) {
		serverLevel.sendParticles(
			TimothatysTrinketsModParticleTypes.SHOCKWAVE.get(),
			origin.x, origin.y, origin.z,
			0,
			effectRadius, 0.0D, 0.0D,
			1.0D
		);
		if (hasFlatImpactArea(serverLevel, origin)) {
			serverLevel.sendParticles(
				TimothatysTrinketsModParticleTypes.EARTH_IMPACT.get(),
				origin.x, origin.y + 0.006D, origin.z,
				0,
				effectRadius / 2.15D, serverLevel.getRandom().nextDouble() * Mth.TWO_PI, 0.0D,
				1.0D
			);
		}
		serverLevel.sendParticles(ParticleTypes.CLOUD, origin.x, origin.y + 0.08D, origin.z, 18, 0.55D, 0.08D, 0.55D, 0.035D);

		BlockPos groundPos = BlockPos.containing(origin.x, origin.y - 0.1D, origin.z);
		BlockState groundState = serverLevel.getBlockState(groundPos);
		if (!groundState.isAir()) {
			serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, groundState), origin.x, origin.y + 0.12D, origin.z, 36, 0.5D, 0.12D, 0.5D, 0.32D);
		}

	}

	private static boolean hasFlatImpactArea(ServerLevel serverLevel, Vec3 origin) {
		BlockPos center = BlockPos.containing(origin.x, origin.y - 0.1D, origin.z);
		for (int[] offset : FLAT_IMPACT_AREA_OFFSETS) {
			BlockPos groundPos = center.offset(offset[0], 0, offset[1]);
			if (!isFlatImpactGround(serverLevel, groundPos))
				return false;
		}
		return true;
	}

	private static boolean isFlatImpactGround(ServerLevel serverLevel, BlockPos groundPos) {
		BlockState groundState = serverLevel.getBlockState(groundPos);
		if (groundState.isAir() || !groundState.isFaceSturdy(serverLevel, groundPos, Direction.UP) || !serverLevel.getFluidState(groundPos).isEmpty())
			return false;

		BlockPos abovePos = groundPos.above();
		return serverLevel.getFluidState(abovePos).isEmpty()
			&& serverLevel.getBlockState(abovePos).getCollisionShape(serverLevel, abovePos).isEmpty();
	}

	private static void spawnGroundDebrisWave(ServerLevel serverLevel, Vec3 origin, double effectRadius) {
		int targetCount = effectRadius > EFFECT_RADIUS
			? 36 + serverLevel.getRandom().nextInt(7)
			: 20 + serverLevel.getRandom().nextInt(5);
		Set<BlockPos> usedPositions = new HashSet<>();
		for (int attempt = 0; attempt < targetCount * 3 && usedPositions.size() < targetCount; attempt++) {
			double angle = serverLevel.getRandom().nextDouble() * Mth.TWO_PI;
			double distance = 0.8D + Math.sqrt(serverLevel.getRandom().nextDouble()) * (effectRadius - 1.1D);
			double x = origin.x + Mth.cos((float) angle) * distance;
			double z = origin.z + Mth.sin((float) angle) * distance;
			BlockPos groundPos = findDebrisGround(serverLevel, x, origin.y, z);
			if (groundPos == null || !usedPositions.add(groundPos))
				continue;

			BlockState groundState = serverLevel.getBlockState(groundPos);
			int startDelay = Mth.floor(distance / effectRadius * 7.0D) + serverLevel.getRandom().nextInt(2);
			int animationDuration = 12 + serverLevel.getRandom().nextInt(5);
			float liftHeight = 0.45F + serverLevel.getRandom().nextFloat() * 0.65F;
			DebtlordGroundDebrisEntity debris = DebtlordGroundDebrisEntity.create(serverLevel, groundPos, groundState, startDelay, animationDuration, liftHeight);
			serverLevel.addFreshEntity(debris);
		}
	}

	private static BlockPos findDebrisGround(ServerLevel serverLevel, double x, double centerY, double z) {
		int blockX = Mth.floor(x);
		int blockZ = Mth.floor(z);
		int maxY = Mth.floor(centerY) + 2;
		int minY = Mth.floor(centerY) - 3;
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(blockX, maxY, blockZ);
		for (int y = maxY; y >= minY; y--) {
			cursor.setY(y);
			BlockState state = serverLevel.getBlockState(cursor);
			if (state.isAir() || state.hasBlockEntity() || !state.isFaceSturdy(serverLevel, cursor, Direction.UP) || !serverLevel.getFluidState(cursor).isEmpty())
				continue;

			BlockPos abovePos = cursor.above();
			if (serverLevel.getFluidState(abovePos).isEmpty() && serverLevel.getBlockState(abovePos).getCollisionShape(serverLevel, abovePos).isEmpty())
				return cursor.immutable();
		}
		return null;
	}
}
