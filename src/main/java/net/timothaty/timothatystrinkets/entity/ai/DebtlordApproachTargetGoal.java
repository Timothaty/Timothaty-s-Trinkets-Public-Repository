package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.DebtlordEntity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public final class DebtlordApproachTargetGoal extends Goal {
	private static final double STOP_DISTANCE = DebtlordHornsGoal.TRIGGER_RANGE - 0.45D;
	private static final double STOP_DISTANCE_SQR = STOP_DISTANCE * STOP_DISTANCE;
	private static final int PATH_RECALCULATION_INTERVAL_TICKS = 6;
	private static final double STABLE_FEET_DELTA = 0.55D;
	private static final int PROGRESS_SAMPLE_INTERVAL_TICKS = 10;
	private static final int STUCK_THRESHOLD_TICKS = 30;
	private static final int PATH_UNAVAILABLE_STUCK_THRESHOLD_TICKS = 20;
	private static final double MIN_PROGRESS_SQR = 0.08D * 0.08D;
	private static final double STEP_RECOVERY_MAX_HEIGHT = 1.15D;
	private static final double STEP_RECOVERY_MAX_RANGE_SQR = 6.0D * 6.0D;
	private static final int STEP_RECOVERY_COOLDOWN_TICKS = 80;

	private final DebtlordEntity debtlord;
	private final double speedModifier;
	private int pathRecalculationTicks;
	private int progressSampleTicks;
	private int stalledTicks;
	private double lastSampleX;
	private double lastSampleZ;
	private LivingEntity trackedTarget;
	private boolean lastPathRequestFailed;
	private long nextStepRecoveryGameTime;

	public DebtlordApproachTargetGoal(DebtlordEntity debtlord, double speedModifier) {
		this.debtlord = debtlord;
		this.speedModifier = speedModifier;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return shouldApproach(debtlord.getTarget());
	}

	@Override
	public boolean canContinueToUse() {
		return shouldApproach(debtlord.getTarget());
	}

	@Override
	public void start() {
		pathRecalculationTicks = 0;
		resetProgress(debtlord.getTarget());
		moveToTarget();
	}

	@Override
	public void tick() {
		LivingEntity target = debtlord.getTarget();
		if (target == null)
			return;

		debtlord.getLookControl().setLookAt(target, 20.0F, 20.0F);
		if (target != trackedTarget)
			resetProgress(target);
		tickProgressDetector(target);
		if (--pathRecalculationTicks <= 0) {
			pathRecalculationTicks = PATH_RECALCULATION_INTERVAL_TICKS;
			moveToTarget();
		}
	}

	@Override
	public void stop() {
		debtlord.getNavigation().stop();
		resetProgress(null);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	private boolean shouldApproach(LivingEntity target) {
		return target != null
			&& target.isAlive()
			&& !debtlord.isUsingAbility()
			&& !debtlord.isTouchingWaterForBossLogic()
			&& !DebtlordEntity.isEntityTouchingWater(target)
			&& !isStableAttackPosition(target);
	}

	private boolean isStableAttackPosition(LivingEntity target) {
		double dx = target.getX() - debtlord.getX();
		double dz = target.getZ() - debtlord.getZ();
		double feetDelta = target.getBoundingBox().minY - debtlord.getBoundingBox().minY;
		return dx * dx + dz * dz <= STOP_DISTANCE_SQR
			&& Math.abs(feetDelta) <= STABLE_FEET_DELTA
			&& debtlord.hasLineOfSight(target)
			&& (debtlord.getNavigation().isDone() || dx * dx + dz * dz <= STOP_DISTANCE_SQR * 0.75D);
	}

	private void moveToTarget() {
		LivingEntity target = debtlord.getTarget();
		if (target != null)
			lastPathRequestFailed = !debtlord.getNavigation().moveTo(target, speedModifier);
	}

	private void tickProgressDetector(LivingEntity target) {
		if (--progressSampleTicks > 0)
			return;
		progressSampleTicks = PROGRESS_SAMPLE_INTERVAL_TICKS;

		double dx = debtlord.getX() - lastSampleX;
		double dz = debtlord.getZ() - lastSampleZ;
		boolean madeProgress = dx * dx + dz * dz >= MIN_PROGRESS_SQR;
		if (madeProgress) {
			stalledTicks = 0;
		} else {
			stalledTicks += PROGRESS_SAMPLE_INTERVAL_TICKS;
		}
		lastSampleX = debtlord.getX();
		lastSampleZ = debtlord.getZ();

		int stuckThreshold = debtlord.getNavigation().isDone() || lastPathRequestFailed
			? PATH_UNAVAILABLE_STUCK_THRESHOLD_TICKS
			: STUCK_THRESHOLD_TICKS;
		if (stalledTicks < stuckThreshold)
			return;
		tryStepRecovery(target);
		stalledTicks = 0;
	}

	private void tryStepRecovery(LivingEntity target) {
		long gameTime = debtlord.level().getGameTime();
		if (gameTime < nextStepRecoveryGameTime)
			return;

		double feetDelta = target.getBoundingBox().minY - debtlord.getBoundingBox().minY;
		double dx = target.getX() - debtlord.getX();
		double dz = target.getZ() - debtlord.getZ();
		if (feetDelta <= STABLE_FEET_DELTA || feetDelta > STEP_RECOVERY_MAX_HEIGHT
			|| dx * dx + dz * dz > STEP_RECOVERY_MAX_RANGE_SQR
			|| !debtlord.canClawAdvanceToward(target))
			return;

		debtlord.offerClawFollowup(target, 0, DebtlordClawGoal.FOLLOWUP_WINDOW_TICKS, DebtlordClawFollowupQueue.Reason.STEP_RECOVERY);
		nextStepRecoveryGameTime = gameTime + STEP_RECOVERY_COOLDOWN_TICKS;
	}

	private void resetProgress(LivingEntity target) {
		trackedTarget = target;
		progressSampleTicks = PROGRESS_SAMPLE_INTERVAL_TICKS;
		stalledTicks = 0;
		lastSampleX = debtlord.getX();
		lastSampleZ = debtlord.getZ();
		lastPathRequestFailed = false;
	}
}
