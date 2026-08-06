package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.NecromancerEntity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class NecromancerApproachCastTargetGoal extends Goal {
	private static final double ACTIVE_AGGRO_RANGE = 24.0D;
	private static final double ACTIVE_AGGRO_RANGE_SQR = ACTIVE_AGGRO_RANGE * ACTIVE_AGGRO_RANGE;
	private static final double CAST_RANGE_BUFFER = 1.5D;
	private static final int PATH_RECALCULATE_TICKS = 10;

	private final NecromancerEntity necromancer;
	private final NecromancerUndeadificationSpellGoal undeadificationSpellGoal;
	private final NecromancerMagicDamageSpellGoal magicDamageSpellGoal;
	private final NecromancerSummonUndeadGoal summonUndeadGoal;
	private final double speedModifier;
	private LivingEntity target;
	private int pathRecalculateTicks;

	public NecromancerApproachCastTargetGoal(
		NecromancerEntity necromancer,
		NecromancerUndeadificationSpellGoal undeadificationSpellGoal,
		NecromancerMagicDamageSpellGoal magicDamageSpellGoal,
		NecromancerSummonUndeadGoal summonUndeadGoal,
		double speedModifier
	) {
		this.necromancer = necromancer;
		this.undeadificationSpellGoal = undeadificationSpellGoal;
		this.magicDamageSpellGoal = magicDamageSpellGoal;
		this.summonUndeadGoal = summonUndeadGoal;
		this.speedModifier = speedModifier;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public boolean canUse() {
		LivingEntity currentTarget = necromancer.getTarget();
		if (!shouldApproach(currentTarget)) {
			return false;
		}

		target = currentTarget;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return shouldApproach(target);
	}

	@Override
	public void start() {
		pathRecalculateTicks = 0;
	}

	@Override
	public void stop() {
		target = null;
		pathRecalculateTicks = 0;
		necromancer.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (target == null) {
			return;
		}

		necromancer.getLookControl().setLookAt(target, 30.0F, 30.0F);
		pathRecalculateTicks--;
		if (pathRecalculateTicks <= 0 || necromancer.getNavigation().isDone()) {
			pathRecalculateTicks = PATH_RECALCULATE_TICKS;
			necromancer.getNavigation().moveTo(target, speedModifier);
		}
	}

	private boolean shouldApproach(LivingEntity candidate) {
		if (candidate == null || !candidate.isAlive() || necromancer.shouldRetreat() || necromancer.isCastingAnySpell()) {
			return false;
		}

		double distanceSqr = necromancer.distanceToSqr(candidate);
		if (distanceSqr > ACTIVE_AGGRO_RANGE_SQR) {
			return false;
		}

		double readyCastRange = getHighestPriorityReadyCastRange(candidate);
		if (readyCastRange <= 0.0D) {
			return false;
		}

		double desiredRange = Math.max(1.0D, readyCastRange - CAST_RANGE_BUFFER);
		return distanceSqr > desiredRange * desiredRange || !necromancer.hasLineOfSight(candidate);
	}

	private double getHighestPriorityReadyCastRange(LivingEntity candidate) {
		if (undeadificationSpellGoal.isReadyToApproachTarget(candidate)) {
			return NecromancerUndeadificationSpellGoal.CAST_RANGE;
		}

		if (magicDamageSpellGoal.isReadyToApproachTarget(candidate)) {
			return NecromancerMagicDamageSpellGoal.CAST_RANGE;
		}

		if (summonUndeadGoal.isReadyToApproachTarget(candidate)) {
			return NecromancerSummonUndeadGoal.CAST_RANGE;
		}

		return -1.0D;
	}
}
