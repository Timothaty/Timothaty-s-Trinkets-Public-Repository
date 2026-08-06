package net.timothaty.timothatystrinkets.entity.ai;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;

import net.timothaty.timothatystrinkets.entity.UndeadKnightEntity;

import java.util.EnumSet;

public class UndeadKnightMeleeAttackGoal extends Goal {
	private final UndeadKnightEntity mob;
	private final double speedModifier;
	private final boolean followingTargetEvenIfNotSeen;
	private final int attackIntervalTicks;
	private Path path;
	private double pathedTargetX;
	private double pathedTargetY;
	private double pathedTargetZ;
	private int ticksUntilNextPathRecalculation;
	private int ticksUntilNextAttack;
	private long lastCanUseCheck;
	private int failedPathFindingPenalty;
	private boolean canPenalize;

	public UndeadKnightMeleeAttackGoal(UndeadKnightEntity mob, double speedModifier, boolean followingTargetEvenIfNotSeen, int attackIntervalTicks) {
		this.mob = mob;
		this.speedModifier = speedModifier;
		this.followingTargetEvenIfNotSeen = followingTargetEvenIfNotSeen;
		this.attackIntervalTicks = attackIntervalTicks;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (this.mob.isSoulAbsorbing()) {
			return false;
		}

		long gameTime = this.mob.level().getGameTime();
		if (gameTime - this.lastCanUseCheck < 20L) {
			return false;
		}

		this.lastCanUseCheck = gameTime;
		LivingEntity target = this.mob.getTarget();
		if (target == null || !target.isAlive()) {
			return false;
		}

		if (this.canPenalize) {
			if (--this.ticksUntilNextPathRecalculation <= 0) {
				this.path = this.mob.getNavigation().createPath(target, 0);
				this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);
				return this.path != null;
			}
			return true;
		}

		this.path = this.mob.getNavigation().createPath(target, 0);
		return this.path != null || this.mob.isWithinMeleeAttackRange(target);
	}

	@Override
	public boolean canContinueToUse() {
		if (this.mob.isSoulAbsorbing()) {
			return false;
		}

		LivingEntity target = this.mob.getTarget();
		if (target == null || !target.isAlive()) {
			return false;
		}

		if (!this.followingTargetEvenIfNotSeen) {
			return !this.mob.getNavigation().isDone() || this.mob.isWithinMeleeAttackRange(target);
		}

		return this.mob.isWithinRestriction(target.blockPosition()) && (!(target instanceof Player player) || !player.isSpectator() && !player.isCreative());
	}

	@Override
	public void start() {
		this.mob.getNavigation().moveTo(this.path, this.speedModifier);
		this.mob.setAggressive(true);
		this.ticksUntilNextPathRecalculation = 0;
		this.ticksUntilNextAttack = 0;
	}

	@Override
	public void stop() {
		LivingEntity target = this.mob.getTarget();
		if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
			this.mob.setTarget(null);
		}

		this.mob.setAggressive(false);
		this.mob.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = this.mob.getTarget();
		if (target == null) {
			return;
		}

		this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
		this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
		if ((this.followingTargetEvenIfNotSeen || this.mob.getSensing().hasLineOfSight(target))
				&& this.ticksUntilNextPathRecalculation <= 0
				&& (this.pathedTargetX == 0.0D && this.pathedTargetY == 0.0D && this.pathedTargetZ == 0.0D
						|| target.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0D
						|| this.mob.getRandom().nextFloat() < 0.05F)) {
			this.pathedTargetX = target.getX();
			this.pathedTargetY = target.getY();
			this.pathedTargetZ = target.getZ();
			this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);
			double distanceSqr = this.mob.distanceToSqr(target);
			if (this.canPenalize) {
				this.applyPathFindingPenalty(target);
			}
			if (distanceSqr > 1024.0D) {
				this.ticksUntilNextPathRecalculation += 10;
			} else if (distanceSqr > 256.0D) {
				this.ticksUntilNextPathRecalculation += 5;
			}

			if (!this.mob.getNavigation().moveTo(target, this.speedModifier)) {
				this.ticksUntilNextPathRecalculation += 15;
			}

			this.ticksUntilNextPathRecalculation = this.adjustedTickDelay(this.ticksUntilNextPathRecalculation);
		}

		this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
		this.checkAndPerformAttack(target);
	}

	private void applyPathFindingPenalty(LivingEntity target) {
		Path currentPath = this.mob.getNavigation().getPath();
		if (currentPath != null) {
			net.minecraft.world.level.pathfinder.Node endNode = currentPath.getEndNode();
			if (endNode != null && target.distanceToSqr(endNode.x, endNode.y, endNode.z) < 1.0D) {
				this.failedPathFindingPenalty = 0;
			} else {
				this.failedPathFindingPenalty += 10;
			}
		} else {
			this.failedPathFindingPenalty += 10;
		}
		this.ticksUntilNextPathRecalculation += this.failedPathFindingPenalty;
	}

	private void checkAndPerformAttack(LivingEntity target) {
		if (this.canPerformAttack(target)) {
			this.resetAttackCooldown();
			this.mob.swing(InteractionHand.MAIN_HAND);
			this.mob.doHurtTarget(target);
		}
	}

	private void resetAttackCooldown() {
		this.ticksUntilNextAttack = this.adjustedTickDelay(this.attackIntervalTicks);
	}

	private boolean isTimeToAttack() {
		return this.ticksUntilNextAttack <= 0;
	}

	private boolean canPerformAttack(LivingEntity target) {
		double baseReachSqr = this.mob.getBbWidth() * this.mob.getBbWidth() + target.getBbWidth();
		double extendedReach = Math.sqrt(baseReachSqr) + 1.0D;
		return !this.mob.isBlocking()
				&& !this.mob.isSoulAbsorbing()
				&& !this.mob.isEmpowering()
				&& !this.mob.isReincarnating()
				&& this.isTimeToAttack()
				&& this.mob.distanceToSqr(target) < extendedReach * extendedReach
				&& this.mob.getSensing().hasLineOfSight(target);
	}
}
