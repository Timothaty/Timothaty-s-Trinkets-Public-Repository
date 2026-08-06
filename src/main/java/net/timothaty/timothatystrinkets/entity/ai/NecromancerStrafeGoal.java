package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.NecromancerEntity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class NecromancerStrafeGoal extends Goal {
	private static final double ACTIVE_RANGE = 24.0D;
	private static final double ACTIVE_RANGE_SQR = ACTIVE_RANGE * ACTIVE_RANGE;
	private static final double STRAFE_RANGE = 16.0D;
	private static final double STRAFE_RANGE_SQR = STRAFE_RANGE * STRAFE_RANGE;
	private static final double TOO_CLOSE_RANGE = 6.0D;
	private static final double TOO_CLOSE_RANGE_SQR = TOO_CLOSE_RANGE * TOO_CLOSE_RANGE;
	private static final double TOO_FAR_FOR_BACKPEDAL_RANGE = STRAFE_RANGE * 0.75D;
	private static final double TOO_FAR_FOR_BACKPEDAL_RANGE_SQR = TOO_FAR_FOR_BACKPEDAL_RANGE * TOO_FAR_FOR_BACKPEDAL_RANGE;
	private static final int REQUIRED_SEE_TIME_TICKS = 20;
	private static final int STRAFE_SWITCH_INTERVAL_TICKS = 20;

	private final NecromancerEntity necromancer;
	private final double speedModifier;
	private final float strafeSpeed;
	private LivingEntity target;
	private int seeTime;
	private int strafingTime = -1;
	private boolean strafingClockwise;
	private boolean strafingBackwards;

	public NecromancerStrafeGoal(NecromancerEntity necromancer, double speedModifier, float strafeSpeed) {
		this.necromancer = necromancer;
		this.speedModifier = speedModifier;
		this.strafeSpeed = strafeSpeed;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public boolean canUse() {
		LivingEntity currentTarget = necromancer.getTarget();
		if (!canStrafeAround(currentTarget)) {
			return false;
		}

		target = currentTarget;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return canStrafeAround(target);
	}

	@Override
	public void start() {
		seeTime = 0;
		strafingTime = -1;
	}

	@Override
	public void stop() {
		target = null;
		seeTime = 0;
		strafingTime = -1;
		necromancer.stopControlledMovement();
	}

	@Override
	public void tick() {
		if (target == null) {
			return;
		}

		if (necromancer.isCastingAnySpell()) {
			necromancer.stopControlledMovement();
			return;
		}

		double distanceSqr = necromancer.distanceToSqr(target);
		boolean hasLineOfSight = necromancer.getSensing().hasLineOfSight(target);
		seeTime = hasLineOfSight ? Math.min(seeTime + 1, REQUIRED_SEE_TIME_TICKS) : Math.max(seeTime - 1, -REQUIRED_SEE_TIME_TICKS);

		necromancer.getLookControl().setLookAt(target, 30.0F, 30.0F);

		if (distanceSqr > STRAFE_RANGE_SQR || seeTime < REQUIRED_SEE_TIME_TICKS) {
			strafingTime = -1;
			necromancer.getNavigation().moveTo(target, speedModifier);
			return;
		}

		necromancer.getNavigation().stop();
		strafingTime++;
		if (strafingTime >= STRAFE_SWITCH_INTERVAL_TICKS) {
			if (necromancer.getRandom().nextFloat() < 0.3F) {
				strafingClockwise = !strafingClockwise;
			}
			if (necromancer.getRandom().nextFloat() < 0.3F) {
				strafingBackwards = !strafingBackwards;
			}
			strafingTime = 0;
		}

		if (distanceSqr > TOO_FAR_FOR_BACKPEDAL_RANGE_SQR) {
			strafingBackwards = false;
		} else if (distanceSqr < TOO_CLOSE_RANGE_SQR) {
			strafingBackwards = true;
		}

		float forward = strafingBackwards ? -strafeSpeed : strafeSpeed;
		float sideways = strafingClockwise ? strafeSpeed : -strafeSpeed;
		necromancer.getMoveControl().strafe(forward, sideways);
		necromancer.lookAt(target, 30.0F, 30.0F);
	}

	private boolean canStrafeAround(LivingEntity candidate) {
		return candidate != null
			&& candidate.isAlive()
			&& !necromancer.shouldRetreat()
			&& !necromancer.isCastingAnySpell()
			&& necromancer.distanceToSqr(candidate) <= ACTIVE_RANGE_SQR;
	}
}
