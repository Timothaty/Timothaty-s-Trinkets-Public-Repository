package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.DebtlordEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class DebtlordChainsGoal extends Goal {
	public enum CaptureResult {
		CAPTURED,
		INVALID_TARGET,
		OUT_OF_RANGE,
		NO_LINE_OF_SIGHT,
		OUTSIDE_CORRIDOR,
		BLOCKED_CHAIN_PATH,
		INEFFECTIVE_PULL
	}

	public static final int CAST_DURATION_TICKS = 2 * 20;
	public static final int SUCCESS_DURATION_TICKS = 25;
	public static final int FAILED_DURATION_TICKS = 15;
	public static final int RELEASE_TICK = 34;
	public static final int PULL_TICK = 5;
	private static final int PULL_DURATION_TICKS = 10;
	private static final double PULL_ANCHOR_FORWARD_DISTANCE = 2.75D;
	private static final double PULL_ANCHOR_HEIGHT_FACTOR = 0.62D;
	private static final double PULL_ANCHOR_STOP_DISTANCE = 1.10D;
	private static final double PULL_ANCHOR_HORIZONTAL_TOLERANCE = 1.0D;
	private static final double PULL_ANCHOR_VERTICAL_TOLERANCE = 1.0D;
	private static final double MIN_PULL_SPEED = 0.55D;
	private static final double MAX_PULL_SPEED = 2.4D;
	private static final double PULL_SPEED_PER_BLOCK = 0.2D;
	private static final double MAX_UPWARD_PULL_SPEED = 0.55D;
	private static final double MAX_DOWNWARD_PULL_SPEED = 0.90D;
	private static final double MIN_PROGRESS_MOVEMENT = 0.60D;
	private static final double MIN_PROGRESS_TOWARD_BOSS = 0.25D;
	private static final double MIN_ANCHOR_DISTANCE_REDUCTION = 0.75D;
	private static final double MIN_FEET_HEIGHT_REDUCTION = 0.75D;
	private static final double MELEE_RANGE = DebtlordHornsGoal.TRIGGER_RANGE;
	private static final double MELEE_RANGE_SQR = MELEE_RANGE * MELEE_RANGE;
	private static final double MELEE_MAX_VERTICAL_DISTANCE = 2.75D;
	private static final int FAR_TARGET_REQUIRED_TICKS = 10;
	private static final int PHASE_TWO_FAR_TARGET_REQUIRED_TICKS = 8;
	private static final int PHASE_THREE_FAR_TARGET_REQUIRED_TICKS = 6;
	private static final int WATER_TARGET_REQUIRED_TICKS = 4;
	private static final int BASE_COOLDOWN_TICKS = 12 * 20;
	private static final int PHASE_TWO_COOLDOWN_TICKS = 7 * 20;
	private static final int PHASE_THREE_COOLDOWN_TICKS = 5 * 20;
	private static final double PHASE_ONE_TRIGGER_RANGE = 14.0D;
	private static final double PHASE_TWO_TRIGGER_RANGE = 13.0D;
	private static final double PHASE_THREE_TRIGGER_RANGE = 11.0D;
	private static final double WATER_TARGET_MIN_RANGE = 4.0D;
	private static final double WATER_TARGET_MIN_RANGE_SQR = WATER_TARGET_MIN_RANGE * WATER_TARGET_MIN_RANGE;
	private static final double HIGH_TARGET_VERTICAL_TRIGGER = 3.0D;
	private static final double MAX_RANGE = 30.0D;
	private static final double MAX_RANGE_SQR = MAX_RANGE * MAX_RANGE;
	private static final double CORRIDOR_HALF_WIDTH = 2.0D;
	private static final double MAX_VERTICAL_DISTANCE = 12.0D;

	private final DebtlordEntity debtlord;
	private final DebtlordAntiPillarGoal antiPillarGoal;
	private LivingEntity target;
	private long nextAvailableGameTime;
	private int farTargetTicks;
	private int farTargetId = -1;
	private boolean released;
	private boolean captured;
	private boolean failedResolutionReported;
	private boolean pullMadeProgress;
	private int pullTargetId = -1;
	private Vec3 pullAnchor;
	private Vec3 initialPullTargetPosition;
	private double initialAnchorDistance;
	private double initialBossDistance;
	private double initialFeetHeightDifference;
	private CaptureResult captureResult = CaptureResult.INVALID_TARGET;

	public DebtlordChainsGoal(DebtlordEntity debtlord, DebtlordAntiPillarGoal antiPillarGoal) {
		this.debtlord = debtlord;
		this.antiPillarGoal = antiPillarGoal;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		LivingEntity currentTarget = debtlord.getTarget();
		if (!isValidTarget(currentTarget)
			|| !debtlord.isAlive()
			|| (!debtlord.onGround() && !debtlord.isTouchingWaterForBossLogic())
			|| debtlord.isUsingAbility()
			|| !debtlord.wantsCombatIntent(DebtlordEntity.CombatIntent.CHAINS)
			|| debtlord.level().getGameTime() < nextAvailableGameTime) {
			resetFarTargetTimer();
			return false;
		}

		double distanceSqr = debtlord.distanceToSqr(currentTarget);
		boolean waterTarget = isWaterTarget(currentTarget, distanceSqr);
		if (distanceSqr > MAX_RANGE_SQR || (!isFarTarget(distanceSqr) && !isHighTarget(currentTarget) && !waterTarget)) {
			resetFarTargetTimer();
			return false;
		}

		if (farTargetId != currentTarget.getId()) {
			farTargetId = currentTarget.getId();
			farTargetTicks = 0;
		}
		farTargetTicks++;
		int requiredTicks = getRequiredTargetTicks(waterTarget);
		return farTargetTicks >= requiredTicks;
	}

	@Override
	public boolean canContinueToUse() {
		return debtlord.isAlive() && debtlord.isUsingChains();
	}

	@Override
	public void start() {
		target = debtlord.getTarget();
		released = false;
		captured = false;
		failedResolutionReported = false;
		clearPullTracking();
		captureResult = CaptureResult.INVALID_TARGET;
		farTargetTicks = 0;
		farTargetId = -1;
		debtlord.startChainCast(target);
	}

	@Override
	public void tick() {
		if (!debtlord.isUsingChains())
			return;

		debtlord.lockAbilityPositionFacing(target);
		int remainingTicks = debtlord.getChainCastTicks();
		int elapsedTicks = debtlord.getCurrentChainPhaseDuration() - remainingTicks + 1;

		if (debtlord.getChainPhase() == DebtlordEntity.CHAIN_PHASE_CAST) {
			tickCast(elapsedTicks, remainingTicks);
			return;
		}

		if (debtlord.getChainPhase() == DebtlordEntity.CHAIN_PHASE_SUCCESS) {
			tickSuccess(elapsedTicks, remainingTicks);
			return;
		}

		tickFailed(remainingTicks);
	}

	@Override
	public void stop() {
		if (debtlord.isUsingChains()) {
			debtlord.finishChainCast();
			startCooldown();
		}
		target = null;
		released = false;
		captured = false;
		failedResolutionReported = false;
		clearPullTracking();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	private void tickCast(int elapsedTicks, int remainingTicks) {
		if (!released && elapsedTicks >= RELEASE_TICK) {
			released = true;
			playChainLaunchSound();
			captureResult = getCaptureResult(target);
			captured = captureResult == CaptureResult.CAPTURED;
			if (captured)
				debtlord.markChainCaptured(target);
		}

		if (remainingTicks > 1) {
			debtlord.setChainCastTicks(remainingTicks - 1);
			return;
		}

		if (captured) {
			playChainCaughtSound();
			startPullTracking(target);
			debtlord.startChainSuccess(target);
		} else {
			reportFailedResolution(captureResult);
			debtlord.startChainFailed(target);
		}
	}

	private void tickSuccess(int elapsedTicks, int remainingTicks) {
		if (elapsedTicks >= PULL_TICK && elapsedTicks < PULL_TICK + PULL_DURATION_TICKS) {
			pullTarget();
			updatePullProgress();
		}

		if (elapsedTicks >= PULL_TICK + PULL_DURATION_TICKS && shouldResolveAsIneffectivePull()) {
			captureResult = CaptureResult.INEFFECTIVE_PULL;
			captured = false;
			reportFailedResolution(captureResult);
			debtlord.startChainFailed(target);
			return;
		}

		if (remainingTicks > 1) {
			debtlord.setChainCastTicks(remainingTicks - 1);
			return;
		}

		debtlord.finishChainCast();
		startCooldown();
		target = null;
	}

	private void tickFailed(int remainingTicks) {
		if (remainingTicks > 1) {
			debtlord.setChainCastTicks(remainingTicks - 1);
			return;
		}

		debtlord.finishChainCast();
		startCooldown();
		target = null;
	}

	private void pullTarget() {
		if (!isTrackedPullTargetValid())
			return;

		Vec3 pullMotion = pullAnchor.subtract(target.position());
		double horizontalDistanceSqr = pullMotion.x * pullMotion.x + pullMotion.z * pullMotion.z;
		double distance = pullMotion.length();
		boolean reachedAnchor = distance <= PULL_ANCHOR_STOP_DISTANCE
			|| (horizontalDistanceSqr <= PULL_ANCHOR_HORIZONTAL_TOLERANCE * PULL_ANCHOR_HORIZONTAL_TOLERANCE
				&& Math.abs(pullMotion.y) <= PULL_ANCHOR_VERTICAL_TOLERANCE);
		if (reachedAnchor) {
			Vec3 currentMotion = target.getDeltaMovement();
			target.setDeltaMovement(currentMotion.scale(0.15D));
		} else if (distance > 1.0E-6D) {
			double pullSpeed = Mth.clamp((distance - PULL_ANCHOR_STOP_DISTANCE) * PULL_SPEED_PER_BLOCK, MIN_PULL_SPEED, MAX_PULL_SPEED);
			Vec3 pullVelocity = pullMotion.scale(pullSpeed / distance);
			double verticalVelocity = Mth.clamp(pullVelocity.y, -MAX_DOWNWARD_PULL_SPEED, MAX_UPWARD_PULL_SPEED);
			target.setDeltaMovement(pullVelocity.x, verticalVelocity, pullVelocity.z);
		}
		target.hurtMarked = true;
		target.hasImpulse = true;
	}

	private CaptureResult getCaptureResult(LivingEntity candidate) {
		if (!isValidTarget(candidate))
			return CaptureResult.INVALID_TARGET;
		if (debtlord.distanceToSqr(candidate) > MAX_RANGE_SQR)
			return CaptureResult.OUT_OF_RANGE;
		if (!debtlord.hasLineOfSight(candidate))
			return CaptureResult.NO_LINE_OF_SIGHT;

		Vec3 look = debtlord.getLookAngle();
		Vec3 forward = new Vec3(look.x, 0.0D, look.z).normalize();
		Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
		Vec3 offset = candidate.position().subtract(debtlord.position());
		double forwardDistance = offset.dot(forward);
		double sideDistance = Math.abs(offset.dot(right));
		boolean insideCorridor = forwardDistance >= 0.0D
			&& forwardDistance <= MAX_RANGE
			&& sideDistance <= CORRIDOR_HALF_WIDTH + candidate.getBbWidth() * 0.5D
			&& Math.abs(candidate.getY() - debtlord.getY()) <= MAX_VERTICAL_DISTANCE;
		if (!insideCorridor)
			return CaptureResult.OUTSIDE_CORRIDOR;
		return DebtlordChainTrajectory.hasClearPath(debtlord, candidate)
			? CaptureResult.CAPTURED
			: CaptureResult.BLOCKED_CHAIN_PATH;
	}

	private void startPullTracking(LivingEntity candidate) {
		float bodyYaw = debtlord.yBodyRot * Mth.DEG_TO_RAD;
		pullAnchor = new Vec3(
			debtlord.getX() - Mth.sin(bodyYaw) * PULL_ANCHOR_FORWARD_DISTANCE,
			debtlord.getBoundingBox().minY + debtlord.getBbHeight() * PULL_ANCHOR_HEIGHT_FACTOR,
			debtlord.getZ() + Mth.cos(bodyYaw) * PULL_ANCHOR_FORWARD_DISTANCE);
		initialPullTargetPosition = candidate.position();
		initialAnchorDistance = Math.sqrt(candidate.distanceToSqr(pullAnchor));
		initialBossDistance = Math.sqrt(debtlord.distanceToSqr(candidate));
		initialFeetHeightDifference = getFeetHeightDifference(candidate);
		pullTargetId = candidate.getId();
		pullMadeProgress = isInNormalMeleeArea(candidate);
	}

	private void updatePullProgress() {
		if (pullMadeProgress || !isTrackedPullTargetValid())
			return;

		double anchorDistance = Math.sqrt(target.distanceToSqr(pullAnchor));
		double bossDistance = Math.sqrt(debtlord.distanceToSqr(target));
		double feetHeightDifference = getFeetHeightDifference(target);
		double movementSqr = target.position().distanceToSqr(initialPullTargetPosition);
		boolean movedTowardBoss = movementSqr >= MIN_PROGRESS_MOVEMENT * MIN_PROGRESS_MOVEMENT
			&& initialBossDistance - bossDistance >= MIN_PROGRESS_TOWARD_BOSS;
		pullMadeProgress = initialAnchorDistance - anchorDistance >= MIN_ANCHOR_DISTANCE_REDUCTION
			|| movedTowardBoss
			|| initialFeetHeightDifference - feetHeightDifference >= MIN_FEET_HEIGHT_REDUCTION
			|| isInNormalMeleeArea(target);
	}

	private boolean shouldResolveAsIneffectivePull() {
		updatePullProgress();
		return isTrackedPullTargetValid()
			&& getFeetHeightDifference(target) >= HIGH_TARGET_VERTICAL_TRIGGER
			&& !isInNormalMeleeArea(target);
	}

	private void reportFailedResolution(CaptureResult result) {
		if (failedResolutionReported)
			return;
		failedResolutionReported = true;
		antiPillarGoal.reportFailedChainResolution(target, result);
	}

	private boolean isTrackedPullTargetValid() {
		return pullAnchor != null && initialPullTargetPosition != null
			&& target != null && target.getId() == pullTargetId && isValidTarget(target);
	}

	private boolean isInNormalMeleeArea(LivingEntity candidate) {
		double dx = candidate.getX() - debtlord.getX();
		double dz = candidate.getZ() - debtlord.getZ();
		return dx * dx + dz * dz <= MELEE_RANGE_SQR
			&& Math.abs(getFeetHeightDifference(candidate)) <= MELEE_MAX_VERTICAL_DISTANCE;
	}

	private double getFeetHeightDifference(LivingEntity candidate) {
		return candidate.getBoundingBox().minY - debtlord.getBoundingBox().minY;
	}

	private void clearPullTracking() {
		pullAnchor = null;
		initialPullTargetPosition = null;
		initialAnchorDistance = 0.0D;
		initialBossDistance = 0.0D;
		initialFeetHeightDifference = 0.0D;
		pullTargetId = -1;
		pullMadeProgress = false;
	}

	private void startCooldown() {
		int cooldown = switch (debtlord.getPhase()) {
			case PHASE_THREE -> PHASE_THREE_COOLDOWN_TICKS;
			case PHASE_TWO -> PHASE_TWO_COOLDOWN_TICKS;
			case PHASE_ONE -> BASE_COOLDOWN_TICKS;
		};
		nextAvailableGameTime = debtlord.level().getGameTime() + cooldown;
	}

	private int getRequiredTargetTicks(boolean waterTarget) {
		if (waterTarget)
			return WATER_TARGET_REQUIRED_TICKS;

		return switch (debtlord.getPhase()) {
			case PHASE_THREE -> PHASE_THREE_FAR_TARGET_REQUIRED_TICKS;
			case PHASE_TWO -> PHASE_TWO_FAR_TARGET_REQUIRED_TICKS;
			case PHASE_ONE -> FAR_TARGET_REQUIRED_TICKS;
		};
	}

	private void playChainLaunchSound() {
		debtlord.level().playSound(null, debtlord.blockPosition(), TimothatysTrinketsModSounds.CHAINS_LAUNCH.get(), SoundSource.HOSTILE, 1.25F, 1.0F);
	}

	private void playChainCaughtSound() {
		if (target != null && target.isAlive()) {
			debtlord.level().playSound(null, target.blockPosition(), TimothatysTrinketsModSounds.CHAINS_CAUGHT.get(), SoundSource.HOSTILE, 1.35F, 1.0F);
			return;
		}
		debtlord.level().playSound(null, debtlord.blockPosition(), TimothatysTrinketsModSounds.CHAINS_CAUGHT.get(), SoundSource.HOSTILE, 1.35F, 1.0F);
	}

	private void resetFarTargetTimer() {
		farTargetTicks = 0;
		farTargetId = -1;
	}

	private boolean isFarTarget(double distanceSqr) {
		return distanceSqr >= getTriggerRangeSqr();
	}

	private double getTriggerRangeSqr() {
		double range = switch (debtlord.getPhase()) {
			case PHASE_THREE -> PHASE_THREE_TRIGGER_RANGE;
			case PHASE_TWO -> PHASE_TWO_TRIGGER_RANGE;
			case PHASE_ONE -> PHASE_ONE_TRIGGER_RANGE;
		};
		return range * range;
	}

	private boolean isHighTarget(LivingEntity candidate) {
		return candidate.getBoundingBox().minY - debtlord.getBoundingBox().minY >= HIGH_TARGET_VERTICAL_TRIGGER;
	}

	private boolean isWaterTarget(LivingEntity candidate, double distanceSqr) {
		return DebtlordEntity.isEntityTouchingWater(candidate)
			&& distanceSqr >= WATER_TARGET_MIN_RANGE_SQR;
	}

	private boolean isValidTarget(LivingEntity candidate) {
		return candidate != null
			&& candidate != debtlord
			&& candidate.isAlive()
			&& !debtlord.isAlliedTo(candidate)
			&& (!(candidate instanceof Player player) || (!player.isCreative() && !player.isSpectator()))
			&& !TimothatysTrinketsStunHelper.isMechanicallyImmunePlayer(candidate);
	}
}
