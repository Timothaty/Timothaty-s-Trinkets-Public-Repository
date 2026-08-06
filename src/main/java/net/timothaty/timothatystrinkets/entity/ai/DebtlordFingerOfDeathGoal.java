package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.DebtlordEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.debtlord.DebtlordDamageScaling;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsDamageSources;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class DebtlordFingerOfDeathGoal extends Goal {
	public static final int CHARGE_DURATION_TICKS = 70;
	public static final int LASER_DURATION_TICKS = 8 * 20;
	public static final int SHOT_DURATION_TICKS = 20;
	public static final double LASER_RANGE = 48.0D;
	private static final int COOLDOWN_TICKS = 18 * 20;
	private static final int PHASE_TWO_COOLDOWN_TICKS = 16 * 20;
	private static final int PHASE_THREE_COOLDOWN_TICKS = 12 * 20;
	private static final double MAX_CAST_RANGE = 36.0D;
	private static final double MAX_CAST_RANGE_SQR = MAX_CAST_RANGE * MAX_CAST_RANGE;
	private static final float CHARGE_YAW_STEP = 24.0F;
	private static final float CHARGE_PITCH_STEP = 14.0F;
	private static final float LASER_TRACK_SPEED_BLOCKS_PER_TICK = 0.30F;
	private static final float LASER_MIN_ANGLE_STEP = 0.45F;
	private static final float LASER_MAX_ANGLE_STEP = 2.35F;
	private static final int SMOKE_INTERVAL_TICKS = 2;
	private static final int DAMAGE_INTERVAL_TICKS = 5;
	private static final int LASER_SOUND_INTERVAL_TICKS = 10;
	private static final float LASER_DAMAGE = 3.0F;
	private static final double LASER_DAMAGE_RADIUS = 0.18D;
	private static final double LASER_START_GRACE_DISTANCE = 0.18D;
	private static final double LASER_GEOMETRY_EPSILON = 1.0E-7D;

	private final DebtlordEntity debtlord;
	private LivingEntity target;
	private long nextAvailableGameTime;

	public DebtlordFingerOfDeathGoal(DebtlordEntity debtlord) {
		this.debtlord = debtlord;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		LivingEntity currentTarget = debtlord.getTarget();
		if (!isValidTarget(currentTarget)
			|| !debtlord.isAlive()
			|| !debtlord.isEnraged()
			|| (!debtlord.onGround() && !debtlord.isTouchingWaterForBossLogic())
			|| debtlord.isUsingAbility()
			|| !debtlord.wantsCombatIntent(DebtlordEntity.CombatIntent.FINGER_OF_DEATH)
			|| debtlord.level().getGameTime() < nextAvailableGameTime
			|| debtlord.distanceToSqr(currentTarget) > MAX_CAST_RANGE_SQR
			|| !debtlord.hasLineOfSight(currentTarget))
			return false;

		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return debtlord.isAlive() && debtlord.isUsingFingerOfDeath();
	}

	@Override
	public void start() {
		target = debtlord.getTarget();
		debtlord.getNavigation().stop();
		debtlord.startFingerOfDeathCharge(target);
	}

	@Override
	public void tick() {
		if (!debtlord.isUsingFingerOfDeath())
			return;

		if (!isValidTarget(target))
			target = debtlord.getTarget();

		int remainingTicks = debtlord.getFingerOfDeathCastTicks();
		int phase = debtlord.getFingerOfDeathPhase();
		if (phase == DebtlordEntity.FINGER_OF_DEATH_PHASE_CHARGE) {
			tickCharge(remainingTicks);
		} else if (phase == DebtlordEntity.FINGER_OF_DEATH_PHASE_IDLE) {
			tickLaser(remainingTicks);
		} else if (phase == DebtlordEntity.FINGER_OF_DEATH_PHASE_SHOT) {
			tickShot(remainingTicks);
		}
	}

	@Override
	public void stop() {
		debtlord.getNavigation().stop();
		if (debtlord.isUsingFingerOfDeath()) {
			debtlord.finishFingerOfDeathCast();
			startCooldown();
		}
		target = null;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	private void tickCharge(int remainingTicks) {
		if (isValidTarget(target))
			debtlord.lockAbilityPositionFacingSlowly(target, CHARGE_YAW_STEP, CHARGE_PITCH_STEP);
		else
			debtlord.lockAbilityPosition();

		debtlord.setFingerOfDeathLaserRotation(debtlord.getYRot(), debtlord.getXRot());
		if (remainingTicks > 1) {
			debtlord.setFingerOfDeathCastTicks(remainingTicks - 1);
			return;
		}

		debtlord.startFingerOfDeathIdle(target);
	}

	private void tickLaser(int remainingTicks) {
		if (isValidTarget(target)) {
			trackLaserTarget(target);
		} else {
			debtlord.lockAbilityPosition();
			debtlord.setFingerOfDeathLaserRotation(debtlord.getYRot(), debtlord.getXRot());
		}

		LaserTrace trace = updateLaserTrace();
		playLaserSound(remainingTicks);
		spawnBlockContactSmoke(trace);
		damageLaserTargets(trace);
		if (remainingTicks > 1) {
			debtlord.setFingerOfDeathCastTicks(remainingTicks - 1);
			return;
		}

		debtlord.startFingerOfDeathShot(target);
	}

	private void tickShot(int remainingTicks) {
		if (isValidTarget(target)) {
			trackLaserTarget(target);
		} else {
			debtlord.lockAbilityPosition();
			debtlord.setFingerOfDeathLaserRotation(debtlord.getYRot(), debtlord.getXRot());
		}

		if (remainingTicks > 1) {
			debtlord.setFingerOfDeathCastTicks(remainingTicks - 1);
			return;
		}

		debtlord.finishFingerOfDeathCast();
		startCooldown();
		target = null;
	}

	private void playLaserSound(int remainingTicks) {
		int elapsedTicks = LASER_DURATION_TICKS - remainingTicks;
		if (elapsedTicks % LASER_SOUND_INTERVAL_TICKS != 0)
			return;

		debtlord.level().playSound(
			null,
			debtlord.getX(),
			debtlord.getY() + debtlord.getBbHeight() * 0.5D,
			debtlord.getZ(),
			TimothatysTrinketsModSounds.DEBTLORD_LASER.get(),
			SoundSource.HOSTILE,
			1.35F,
			1.0F
		);
	}

	private void trackLaserTarget(LivingEntity target) {
		Vec3 source = debtlord.getFingerOfDeathServerLaserSource();
		Vec3 aimPoint = getLaserAimPoint(target);
		double distance = Math.max(0.001D, source.distanceTo(aimPoint));
		float angleStep = (float) (360.0D * LASER_TRACK_SPEED_BLOCKS_PER_TICK / (distance * Math.PI * 2.0D));
		angleStep = Mth.clamp(angleStep, LASER_MIN_ANGLE_STEP, LASER_MAX_ANGLE_STEP);
		debtlord.lockAbilityPosition();
		aimLaserFromSourceAt(aimPoint, angleStep);
		debtlord.lockAbilityPositionFacingFingerOfDeathLaser();
	}

	private Vec3 getLaserAimPoint(LivingEntity target) {
		return target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
	}

	private void aimLaserFromSourceAt(Vec3 aimPoint, float maxAngleStep) {
		Vec3 source = debtlord.getFingerOfDeathServerLaserSource();
		Vec3 offset = aimPoint.subtract(source);
		double horizontalDistance = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
		float targetYaw = (float) (Mth.atan2(offset.z, offset.x) * Mth.RAD_TO_DEG) - 90.0F;
		float targetPitch = (float) -(Mth.atan2(offset.y, horizontalDistance) * Mth.RAD_TO_DEG);
		float nextYaw = approachDegrees(debtlord.getFingerOfDeathLaserYaw(), targetYaw, maxAngleStep);
		float nextPitch = approachDegrees(debtlord.getFingerOfDeathLaserPitch(), Mth.clamp(targetPitch, -70.0F, 55.0F), maxAngleStep);
		debtlord.setFingerOfDeathLaserRotation(nextYaw, nextPitch);
	}

	private static float approachDegrees(float current, float target, float maxChange) {
		float delta = Mth.wrapDegrees(target - current);
		return current + Mth.clamp(delta, -maxChange, maxChange);
	}

	private LaserTrace updateLaserTrace() {
		if (!(debtlord.level() instanceof ServerLevel serverLevel)) {
			Vec3 start = debtlord.getFingerOfDeathServerLaserSource();
			return new LaserTrace(start, start.add(debtlord.getFingerOfDeathLaserDirection().scale(LASER_RANGE)), false);
		}

		LaserTrace trace = getLaserTrace(serverLevel);
		debtlord.setFingerOfDeathLaserTarget(trace.end());
		return trace;
	}

	private void spawnBlockContactSmoke(LaserTrace trace) {
		if (debtlord.tickCount % SMOKE_INTERVAL_TICKS != 0 || !(debtlord.level() instanceof ServerLevel serverLevel))
			return;

		if (!trace.hitBlock())
			return;

		Vec3 position = trace.end();
		serverLevel.sendParticles(
			ParticleTypes.SMOKE,
			position.x, position.y, position.z,
			5,
			0.055D, 0.055D, 0.055D,
			0.01D
		);
	}

	private void damageLaserTargets(LaserTrace trace) {
		if (debtlord.tickCount % DAMAGE_INTERVAL_TICKS != 0 || !(debtlord.level() instanceof ServerLevel serverLevel))
			return;

		AABB searchArea = new AABB(trace.start(), trace.end()).inflate(LASER_DAMAGE_RADIUS + 0.35D);
		for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, searchArea, this::canLaserDamage)) {
			if (!isTouchingLaser(trace.start(), trace.end(), victim))
				continue;

			victim.invulnerableTime = 0;
			float damage = DebtlordDamageScaling.scaleDamage(victim, LASER_DAMAGE, DebtlordDamageScaling.DIRECT_ATTACK_IRON_GOLEM_MULTIPLIER);
			victim.hurt(TimothatysTrinketsDamageSources.debtlordLaser(victim.level(), debtlord), damage);
		}
	}

	private boolean canLaserDamage(LivingEntity candidate) {
		return candidate != debtlord
			&& candidate.isAlive()
			&& !debtlord.isAlliedTo(candidate)
			&& (!(candidate instanceof Player player) || (!player.isCreative() && !player.isSpectator()));
	}

	private boolean isTouchingLaser(Vec3 start, Vec3 end, LivingEntity victim) {
		Vec3 segment = end.subtract(start);
		double length = segment.length();
		if (length < 1.0E-5D)
			return false;

		Vec3 effectiveStart = start.add(segment.scale(Math.min(LASER_START_GRACE_DISTANCE / length, 0.95D)));
		AABB damageBox = victim.getBoundingBox().inflate(LASER_DAMAGE_RADIUS);
		return intersectsSegment(damageBox, effectiveStart, end);
	}

	private boolean intersectsSegment(AABB box, Vec3 start, Vec3 end) {
		double[] interval = new double[] {0.0D, 1.0D};
		return clipSlab(interval, start.x, end.x - start.x, box.minX, box.maxX)
			&& clipSlab(interval, start.y, end.y - start.y, box.minY, box.maxY)
			&& clipSlab(interval, start.z, end.z - start.z, box.minZ, box.maxZ);
	}

	private boolean clipSlab(double[] interval, double origin, double delta, double min, double max) {
		if (Math.abs(delta) < LASER_GEOMETRY_EPSILON)
			return origin >= min && origin <= max;

		double first = (min - origin) / delta;
		double second = (max - origin) / delta;
		if (first > second) {
			double swap = first;
			first = second;
			second = swap;
		}

		interval[0] = Math.max(interval[0], first);
		interval[1] = Math.min(interval[1], second);
		return interval[1] >= interval[0];
	}

	private LaserTrace getLaserTrace(ServerLevel serverLevel) {
		Vec3 start = debtlord.getFingerOfDeathServerLaserSource();
		Vec3 end = start.add(debtlord.getFingerOfDeathLaserDirection().scale(LASER_RANGE));
		HitResult hit = serverLevel.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, debtlord));
		return hit.getType() == HitResult.Type.BLOCK ? new LaserTrace(start, hit.getLocation(), true) : new LaserTrace(start, end, false);
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

	private boolean isValidTarget(LivingEntity candidate) {
		return candidate != null
			&& candidate != debtlord
			&& candidate.isAlive()
			&& debtlord.distanceToSqr(candidate) <= MAX_CAST_RANGE_SQR
			&& !debtlord.isAlliedTo(candidate)
			&& (!(candidate instanceof Player player) || (!player.isCreative() && !player.isSpectator()));
	}

	private record LaserTrace(Vec3 start, Vec3 end, boolean hitBlock) {
	}
}
