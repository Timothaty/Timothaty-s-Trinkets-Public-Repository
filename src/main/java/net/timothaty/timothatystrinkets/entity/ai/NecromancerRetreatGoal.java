package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.NecromancerEntity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class NecromancerRetreatGoal extends Goal {
	private final NecromancerEntity necromancer;
	private final double speedModifier;
	private final int horizontalRange;
	private final int verticalRange;

	private double wantedX;
	private double wantedY;
	private double wantedZ;

	public NecromancerRetreatGoal(NecromancerEntity necromancer, double speedModifier, int horizontalRange, int verticalRange) {
		this.necromancer = necromancer;
		this.speedModifier = speedModifier;
		this.horizontalRange = horizontalRange;
		this.verticalRange = verticalRange;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		LivingEntity attacker = necromancer.getRetreatTarget();

		if (!necromancer.shouldRetreat() || attacker == null) {
			return false;
		}

		Vec3 retreatPos = DefaultRandomPos.getPosAway(necromancer, horizontalRange, verticalRange, attacker.position());

		if (retreatPos == null) {
			return false;
		}

		this.wantedX = retreatPos.x;
		this.wantedY = retreatPos.y;
		this.wantedZ = retreatPos.z;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return necromancer.shouldRetreat() && !necromancer.getNavigation().isDone();
	}

	@Override
	public void start() {
		necromancer.getNavigation().moveTo(wantedX, wantedY, wantedZ, speedModifier);
	}

	@Override
	public void stop() {
		necromancer.getNavigation().stop();
	}
}