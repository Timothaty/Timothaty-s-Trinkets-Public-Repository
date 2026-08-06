package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.DebtlordEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.debtlord.DebtlordDamageScaling;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public final class DebtlordClawGoal extends Goal {
	public enum ControlledCastStatus {
		RUNNING,
		PRIMARY_FINAL_IMPACT,
		FINISHED,
		CANCELLED
	}

	public static final int CAST_DURATION_TICKS = 3 * 20;
	public static final int FIRST_SOUND_TICK = 24;
	public static final int SECOND_SOUND_TICK = 37;
	public static final int FIRST_IMPACT_TICK = 27;
	public static final int SECOND_IMPACT_TICK = 41;
	public static final int CHAIN_START_TICK = 45;
	public static final int COOLDOWN_TICKS = 10 * 20;
	private static final int PHASE_TWO_COOLDOWN_TICKS = 8 * 20;
	private static final int PHASE_THREE_COOLDOWN_TICKS = 6 * 20;
	public static final int FOLLOWUP_WINDOW_TICKS = 5 * 20;
	public static final double ADVANCE_FRACTION = 0.85D;
	public static final double ENRAGED_ADVANCE_FRACTION = 0.95D;
	private static final double ATTACK_RANGE = 3.5D;
	private static final double ATTACK_RANGE_SQR = ATTACK_RANGE * ATTACK_RANGE;
	private static final double MAX_VERTICAL_DISTANCE = 2.75D;
	private static final double MAX_APPROACH_UPHILL_DELTA = 1.15D;
	private static final double MAX_APPROACH_DOWNHILL_DELTA = 2.4D;
	private static final float FIRST_DAMAGE = 7.0F;
	private static final float SECOND_DAMAGE = 6.0F;
	private static final float BASE_LIFE_STEAL_RATIO = 0.2F;
	private static final float ENRAGED_LIFE_STEAL_RATIO = 0.8F;
	private static final double SPLASH_HALF_WIDTH = 1.5D;
	private static final double SPLASH_BACK_REACH = 0.45D;
	private static final int BONUS_CAST_BLOOD_PARTICLES = 72;
	private static final int MOB_STUN_DURATION_TICKS = 20;
	private static final int PLAYER_STUN_DURATION_TICKS = 2 * 20;

	private final DebtlordEntity debtlord;
	private LivingEntity target;
	private long nextAvailableGameTime;
	private int successfulStrikes;
	private int castNumber;
	private int maxCastCount;
	private DebtlordClawFollowupQueue.Entry claimedFollowup;
	private boolean controlledMode;
	private boolean controlledFinalPrimaryHit;

	public DebtlordClawGoal(DebtlordEntity debtlord) {
		this.debtlord = debtlord;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		DebtlordClawFollowupQueue.Entry followup = debtlord.peekReadyClawFollowup();
		LivingEntity currentTarget = followup != null ? followup.target() : debtlord.getTarget();
		boolean hasFollowupWindow = followup != null;
		if (!isValidTarget(currentTarget)
			|| !debtlord.isAlive()
			|| !debtlord.onGround()
			|| debtlord.isUsingAbility()
			|| (!hasFollowupWindow && debtlord.level().getGameTime() < nextAvailableGameTime)
			|| !canApproachTarget(currentTarget, followup != null && followup.reason() == DebtlordClawFollowupQueue.Reason.PILLAR_RECOVERY))
			return false;

		return hasFollowupWindow
			|| debtlord.wantsCombatIntent(DebtlordEntity.CombatIntent.CLAWS);
	}

	@Override
	public boolean canContinueToUse() {
		return debtlord.isAlive()
			&& isValidTarget(target)
			&& !debtlord.isTouchingWaterForBossLogic()
			&& debtlord.isUsingClaws();
	}

	@Override
	public void start() {
		controlledMode = false;
		claimedFollowup = debtlord.consumeReadyClawFollowup();
		target = claimedFollowup != null ? claimedFollowup.target() : debtlord.getTarget();
		if (claimedFollowup != null)
			debtlord.setTarget(target);
		successfulStrikes = 0;
		castNumber = 1;
		maxCastCount = claimedFollowup != null && claimedFollowup.reason() == DebtlordClawFollowupQueue.Reason.PILLAR_RECOVERY
			? 1
			: getAllowedCastCount();
		startCast();
	}

	@Override
	public void tick() {
		if (debtlord.isUsingClaws())
			tickCast();
	}

	@Override
	public void stop() {
		debtlord.getNavigation().stop();
		if (debtlord.isUsingClaws()) {
			debtlord.finishClawCast();
			startCooldown();
		}
		target = null;
		successfulStrikes = 0;
		castNumber = 0;
		maxCastCount = 1;
		claimedFollowup = null;
		controlledMode = false;
		controlledFinalPrimaryHit = false;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	private void startCast() {
		debtlord.getNavigation().stop();
		if (controlledMode)
			debtlord.startControlledClawCast(target, getCastDurationTicks(castNumber), castNumber);
		else
			debtlord.startClawCast(target, getCastDurationTicks(castNumber), castNumber);
	}

	private void tickCast() {
		if (!controlledMode && (debtlord.isTouchingWaterForBossLogic() || DebtlordEntity.isEntityTouchingWater(target))) {
			debtlord.finishClawCast();
			if (!controlledMode) {
				startCooldown();
				target = null;
			}
			return;
		}

		debtlord.lockAbilityPositionFacing(target);
		int remainingTicks = debtlord.getClawCastTicks();
		int castDurationTicks = getCastDurationTicks(castNumber);
		int elapsedTicks = castDurationTicks - remainingTicks + 1;
		if (elapsedTicks == getScaledTick(FIRST_SOUND_TICK, castNumber) || elapsedTicks == getScaledTick(SECOND_SOUND_TICK, castNumber))
			playClawSound();
		if (elapsedTicks == getScaledTick(FIRST_IMPACT_TICK, castNumber)) {
			if (performSweep(FIRST_DAMAGE))
				successfulStrikes++;
			debtlord.advanceClawToward(target);
		}
		if (elapsedTicks == getScaledTick(SECOND_IMPACT_TICK, castNumber)) {
			if (performSweep(SECOND_DAMAGE))
				successfulStrikes++;
			debtlord.advanceClawToward(target);
		}
		if (!controlledMode)
			maxCastCount = Math.max(maxCastCount, getAllowedCastCount());
		if (!controlledMode
			&& elapsedTicks == getScaledTick(CHAIN_START_TICK, castNumber)
			&& castNumber < maxCastCount
			&& successfulStrikes >= 1
			&& isTargetInAttackRange(target)) {
			castNumber++;
			successfulStrikes = 0;
			debtlord.startClawCast(target, getCastDurationTicks(castNumber), castNumber);
			return;
		}

		if (remainingTicks > 1) {
			debtlord.setClawCastTicks(remainingTicks - 1);
			return;
		}

		debtlord.finishClawCast();
		if (!controlledMode) {
			startCooldown();
			target = null;
		}
	}

	private boolean performSweep(float damage) {
		if (!(debtlord.level() instanceof ServerLevel serverLevel) || !isTargetInAttackRange(target))
			return false;

		float lifeStealRatio = debtlord.isEnraged()
			? ENRAGED_LIFE_STEAL_RATIO
			: BASE_LIFE_STEAL_RATIO;
		AABB searchBounds = debtlord.getBoundingBox().inflate(ATTACK_RANGE, MAX_VERTICAL_DISTANCE, ATTACK_RANGE);
		List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, searchBounds, this::isInClawSweep);
		boolean anyVictimDamaged = false;
		for (LivingEntity victim : victims) {
			boolean damaged = damageVictim(victim, damage, lifeStealRatio);
			if (damaged)
				anyVictimDamaged = true;
		}
		return anyVictimDamaged;
	}

	private boolean damageVictim(LivingEntity victim, float damage, float lifeStealRatio) {
		float appliedMultiplier = DebtlordDamageScaling.getAppliedMultiplier(victim, DebtlordDamageScaling.DIRECT_ATTACK_IRON_GOLEM_MULTIPLIER);
		float effectiveHealthBefore = DebtlordDamageScaling.getEffectiveHealth(victim);
		victim.invulnerableTime = 0;
		if (!victim.hurt(debtlord.damageSources().mobAttack(debtlord), damage * appliedMultiplier))
			return false;

		float actualDamage = DebtlordDamageScaling.getActualDamage(victim, effectiveHealthBefore);
		if (actualDamage <= 0.0F)
			return false;

		float lifeStealDamage = DebtlordDamageScaling.normalizeDamageForLifeSteal(victim, actualDamage, appliedMultiplier);
		debtlord.heal(lifeStealDamage * lifeStealRatio);
		TimothatysTrinketsStunHelper.tryApplyStunSilently(victim, debtlord, MOB_STUN_DURATION_TICKS, PLAYER_STUN_DURATION_TICKS);
		if (controlledMode && victim == target && damage == SECOND_DAMAGE)
			controlledFinalPrimaryHit = true;
		if (victim == target && castNumber >= 2)
			spawnBonusCastBlood(victim);
		return true;
	}

	private boolean isInClawSweep(LivingEntity candidate) {
		if (!isValidTargetForCurrentMode(candidate))
			return false;
		if (candidate == target)
			return isTargetInAttackRange(candidate);

		Vec3 look = debtlord.getLookAngle();
		Vec3 forward = new Vec3(look.x, 0.0D, look.z).normalize();
		Vec3 sideways = new Vec3(-forward.z, 0.0D, forward.x);
		Vec3 offset = candidate.position().subtract(debtlord.position());
		double forwardDistance = offset.x * forward.x + offset.z * forward.z;
		double sideDistance = Math.abs(offset.x * sideways.x + offset.z * sideways.z);
		return forwardDistance >= -SPLASH_BACK_REACH
			&& forwardDistance <= ATTACK_RANGE
			&& sideDistance <= SPLASH_HALF_WIDTH + candidate.getBbWidth() * 0.5D
			&& Math.abs(offset.y) <= MAX_VERTICAL_DISTANCE;
	}

	private void spawnBonusCastBlood(LivingEntity victim) {
		if (!(debtlord.level() instanceof ServerLevel serverLevel))
			return;

		for (int i = 0; i < BONUS_CAST_BLOOD_PARTICLES; i++) {
			double angle = serverLevel.getRandom().nextDouble() * Math.PI * 2.0D;
			double speed = 0.45D + serverLevel.getRandom().nextDouble() * 0.75D;
			double x = victim.getX() + (serverLevel.getRandom().nextDouble() - 0.5D) * victim.getBbWidth();
			double y = victim.getY() + victim.getBbHeight() * (0.2D + serverLevel.getRandom().nextDouble() * 0.65D);
			double z = victim.getZ() + (serverLevel.getRandom().nextDouble() - 0.5D) * victim.getBbWidth();
			serverLevel.sendParticles(
				TimothatysTrinketsModParticleTypes.BLOOD_BIT.get(),
				x, y, z,
				0,
				Math.cos(angle) * speed,
				0.2D + serverLevel.getRandom().nextDouble() * 0.7D,
				Math.sin(angle) * speed,
				1.0D
			);
		}
	}

	private int getAllowedCastCount() {
		return switch (debtlord.getPhase()) {
			case PHASE_THREE -> 3;
			case PHASE_TWO -> 2;
			case PHASE_ONE -> 1;
		};
	}

	public static float getAnimationSpeed(int castNumber) {
		if (castNumber >= 3)
			return 3.0F;
		if (castNumber == 2)
			return 2.5F;
		return 1.0F;
	}

	public static int getCastDurationTicks(int castNumber) {
		return Math.max(1, Math.round(CAST_DURATION_TICKS / getAnimationSpeed(castNumber)));
	}

	private static int getScaledTick(int baseTick, int castNumber) {
		return Math.max(1, Math.round(baseTick / getAnimationSpeed(castNumber)));
	}

	private void playClawSound() {
		debtlord.level().playSound(null, debtlord.blockPosition(), TimothatysTrinketsModSounds.FEAR_MY_CLAWS.get(), SoundSource.HOSTILE, 1.3F, 1.0F);
	}

	private void startCooldown() {
		nextAvailableGameTime = debtlord.level().getGameTime() + getCooldownTicks();
	}

	private int getCooldownTicks() {
		return switch (debtlord.getPhase()) {
			case PHASE_THREE -> PHASE_THREE_COOLDOWN_TICKS;
			case PHASE_TWO -> PHASE_TWO_COOLDOWN_TICKS;
			case PHASE_ONE -> COOLDOWN_TICKS;
		};
	}

	private boolean isTargetInAttackRange(LivingEntity candidate) {
		if (!isValidTargetForCurrentMode(candidate))
			return false;

		double dx = candidate.getX() - debtlord.getX();
		double dz = candidate.getZ() - debtlord.getZ();
		return dx * dx + dz * dz <= ATTACK_RANGE_SQR
			&& Math.abs(candidate.getY() - debtlord.getY()) <= MAX_VERTICAL_DISTANCE;
	}

	private boolean canApproachTarget(LivingEntity candidate, boolean allowHighRecovery) {
		if (!isValidTarget(candidate) || debtlord.isTouchingWaterForBossLogic() || DebtlordEntity.isEntityTouchingWater(candidate))
			return false;

		if (!isTargetInAttackRange(candidate)) {
			double feetDelta = candidate.getBoundingBox().minY - debtlord.getBoundingBox().minY;
			if ((!allowHighRecovery && feetDelta > MAX_APPROACH_UPHILL_DELTA) || feetDelta < -MAX_APPROACH_DOWNHILL_DELTA)
				return false;
		}
		return debtlord.canClawAdvanceToward(candidate);
	}

	private boolean isValidTarget(LivingEntity candidate) {
		return isValidTarget(candidate, false);
	}

	private boolean isValidControlledTarget(LivingEntity candidate) {
		return isValidTarget(candidate, true);
	}

	private boolean isValidTarget(LivingEntity candidate, boolean allowWaterTarget) {
		return candidate != null
			&& candidate != debtlord
			&& candidate.isAlive()
			&& (allowWaterTarget || !DebtlordEntity.isEntityTouchingWater(candidate))
			&& !debtlord.isAlliedTo(candidate)
			&& !TimothatysTrinketsStunHelper.isMechanicallyImmunePlayer(candidate);
	}

	private boolean isValidTargetForCurrentMode(LivingEntity candidate) {
		return controlledMode && candidate == target
			? isValidControlledTarget(candidate)
			: isValidTarget(candidate);
	}

	public boolean beginControlledSingleCast(LivingEntity controlledTarget) {
		return beginControlledSingleCast(controlledTarget, 1);
	}

	public boolean beginControlledSingleCast(LivingEntity controlledTarget, int controlledCastIndex) {
		if (!isValidControlledTarget(controlledTarget))
			return false;
		target = controlledTarget;
		successfulStrikes = 0;
		castNumber = Mth.clamp(controlledCastIndex, 1, 3);
		maxCastCount = 1;
		controlledMode = true;
		controlledFinalPrimaryHit = false;
		startCast();
		return debtlord.isUsingClaws();
	}

	public ControlledCastStatus tickControlledSingleCast() {
		if (!controlledMode)
			return ControlledCastStatus.CANCELLED;
		if (!isValidControlledTarget(target)) {
			cancelControlledSingleCast();
			return ControlledCastStatus.CANCELLED;
		}

		controlledFinalPrimaryHit = false;
		tickCast();
		if (controlledFinalPrimaryHit)
			return ControlledCastStatus.PRIMARY_FINAL_IMPACT;
		return debtlord.isUsingClaws() ? ControlledCastStatus.RUNNING : ControlledCastStatus.FINISHED;
	}

	public void cancelControlledSingleCast() {
		if (controlledMode && debtlord.isUsingClaws())
			debtlord.finishClawCast();
		target = null;
		successfulStrikes = 0;
		castNumber = 0;
		maxCastCount = 1;
		controlledMode = false;
		controlledFinalPrimaryHit = false;
	}
}
