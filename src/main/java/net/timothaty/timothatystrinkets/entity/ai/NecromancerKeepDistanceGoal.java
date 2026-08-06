package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.NecromancerEntity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class NecromancerKeepDistanceGoal extends Goal {
	private static final double MIN_TARGET_DISTANCE = 5.0D;
	private static final double MIN_TARGET_DISTANCE_SQR = MIN_TARGET_DISTANCE * MIN_TARGET_DISTANCE;

	private final NecromancerEntity necromancer;
	private final double speedModifier;
	private final int horizontalRange;
	private final int verticalRange;
	private double wantedX;
	private double wantedY;
	private double wantedZ;

	public NecromancerKeepDistanceGoal(NecromancerEntity necromancer, double speedModifier, int horizontalRange, int verticalRange) {
		this.necromancer = necromancer;
		this.speedModifier = speedModifier;
		this.horizontalRange = horizontalRange;
		this.verticalRange = verticalRange;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = necromancer.getTarget();
		if (!shouldMoveAwayFrom(target)) {
			return false;
		}

		Vec3 awayPos = DefaultRandomPos.getPosAway(necromancer, horizontalRange, verticalRange, target.position());
		if (awayPos == null) {
			return false;
		}

		wantedX = awayPos.x;
		wantedY = awayPos.y;
		wantedZ = awayPos.z;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return shouldMoveAwayFrom(necromancer.getTarget()) && !necromancer.getNavigation().isDone();
	}

	@Override
	public void start() {
		necromancer.getNavigation().moveTo(wantedX, wantedY, wantedZ, speedModifier);
	}

	@Override
	public void tick() {
		LivingEntity target = necromancer.getTarget();
		if (target != null) {
			necromancer.getLookControl().setLookAt(target, 30.0F, 30.0F);
		}
	}

	@Override
	public void stop() {
		necromancer.getNavigation().stop();
	}

	private boolean shouldMoveAwayFrom(LivingEntity target) {
		return target != null
			&& target.isAlive()
			&& !necromancer.shouldRetreat()
			&& !necromancer.isCastingAnySpell()
			&& necromancer.distanceToSqr(target) < MIN_TARGET_DISTANCE_SQR;
	}
}
