package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.NecromancerEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.necromancer.NecromancerAllyHelper;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

public class NecromancerUndeadificationSpellGoal extends Goal {
	private static final int CAST_DURATION_TICKS = NecromancerEntity.UNDEADIFICATION_CAST_ANIMATION_TICKS;
	private static final int EFFECT_DURATION_TICKS = 20 * 60 * 15;
	private static final int COOLDOWN_TICKS = 20 * 10;
	public static final double CAST_RANGE = 16.0D;
	private static final double CAST_RANGE_SQR = CAST_RANGE * CAST_RANGE;
	private static final TargetingConditions SPELL_TARGETING = TargetingConditions.forCombat().range(CAST_RANGE);

	private final NecromancerEntity necromancer;
	private int castTicks;
	private int cooldownTicks;
	private LivingEntity target;
	private boolean effectApplied;

	public NecromancerUndeadificationSpellGoal(NecromancerEntity necromancer) {
		this.necromancer = necromancer;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public boolean canUse() {
		if (cooldownTicks > 0) {
			cooldownTicks--;
			return false;
		}

		LivingEntity currentTarget = necromancer.getTarget();
		if (!isValidTarget(currentTarget) || necromancer.shouldRetreat() || necromancer.isCastingAnySpell()) {
			return false;
		}

		target = currentTarget;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return castTicks > 0 && isValidTarget(target) && !necromancer.shouldRetreat();
	}

	@Override
	public void start() {
		castTicks = CAST_DURATION_TICKS;
		effectApplied = false;
		necromancer.stopControlledMovement();
		necromancer.startUndeadificationCast();
		playCastStartSound();
	}

	@Override
	public void tick() {
		if (target == null) {
			return;
		}

		necromancer.stopControlledMovement();
		necromancer.getLookControl().setLookAt(target, 30.0F, 30.0F);
		castTicks--;
		if (castTicks <= 0 && !effectApplied && isValidTarget(target)) {
			effectApplied = applyUndeadification(target);
		}
	}

	@Override
	public void stop() {
		boolean shouldStartCooldown = effectApplied;
		castTicks = 0;
		effectApplied = false;
		if (shouldStartCooldown) {
			cooldownTicks = COOLDOWN_TICKS;
		}
		target = null;
	}

	public boolean isReadyToApproachTarget(LivingEntity candidate) {
		return cooldownTicks <= 0 && isPotentialTarget(candidate);
	}

	private boolean isValidTarget(LivingEntity candidate) {
		return isCastTargetStillAvailable(candidate) && isPotentialTarget(candidate);
	}

	private boolean isCastTargetStillAvailable(LivingEntity candidate) {
		if (candidate == null || !candidate.isAlive()) {
			return false;
		}

		if (necromancer.distanceToSqr(candidate) > CAST_RANGE_SQR) {
			return false;
		}

		Level level = necromancer.level();
		return level.isClientSide() || SPELL_TARGETING.test(necromancer, candidate);
	}

	private boolean isPotentialTarget(LivingEntity candidate) {
		return candidate != null
			&& candidate.isAlive()
			&& !NecromancerAllyHelper.isFriendlyPair(necromancer, candidate)
			&& NecromancerUndeadificationTargets.canReceiveUndeadification(candidate)
			&& !candidate.hasEffect(TimothatysTrinketsModMobEffects.UNDEADIFICATION);
	}

	private boolean applyUndeadification(LivingEntity target) {
		return target.addEffect(new MobEffectInstance(TimothatysTrinketsModMobEffects.UNDEADIFICATION, EFFECT_DURATION_TICKS, 0, false, true, true), necromancer);
	}

	private void playCastStartSound() {
		necromancer.level().playSound(
			null,
			necromancer.blockPosition(),
			TimothatysTrinketsModSounds.NECRO_CAST_UNDEADIFICATION.get(),
			SoundSource.HOSTILE,
			0.9F,
			0.8F + necromancer.getRandom().nextFloat() * 0.2F
		);
	}
}
