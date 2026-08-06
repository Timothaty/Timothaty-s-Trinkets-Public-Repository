package net.timothaty.timothatystrinkets.mechanics.venom;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class CorrosiveToxicityHelper {
	private CorrosiveToxicityHelper() {
	}

	public static boolean applyLevelOneForAtLeast(
			LivingEntity target,
			LivingEntity source,
			int durationTicks
	) {
		if (target == null
				|| target.level().isClientSide()
				|| !target.isAlive()
				|| target.isDeadOrDying()
				|| durationTicks <= 0) {
			return false;
		}

		MobEffectInstance current = target.getEffect(
				TimothatysTrinketsModMobEffects.CORROSIVE_TOXICITY
		);
		if (current != null) {
			if (current.getAmplifier() > 0 || current.getDuration() >= durationTicks)
				return false;
		}

		MobEffectInstance refreshed = new MobEffectInstance(
				TimothatysTrinketsModMobEffects.CORROSIVE_TOXICITY,
				durationTicks,
				0,
				current != null && current.isAmbient(),
				current == null || current.isVisible(),
				current == null || current.showIcon()
		);
		return source == null
				? target.addEffect(refreshed)
				: target.addEffect(refreshed, source);
	}
}
