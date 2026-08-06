package net.timothaty.timothatystrinkets.util;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class TimothatysTrinketsControlParticles {
	private static final int SPIRAL_KEEPALIVE_INTERVAL = 3;
	private static final double SPIRAL_BASE_Y_OFFSET = 0.0D;
	private static final double STUNNED_STACKED_Y_OFFSET = 0.25D;
	private static final double HUBRIS_CONTROL_EFFECT_Y_OFFSET = 0.32D;
	private static final double SPIRAL_FALLBACK_HEAD_Y_OFFSET = 0.24D;

	private TimothatysTrinketsControlParticles() {
	}

	public static void hideVisibleControlEffectParticles(LivingEntity living) {
		hideVisibleEffectParticles(living, TimothatysTrinketsModMobEffects.STUNNED);
		hideVisibleEffectParticles(living, TimothatysTrinketsModMobEffects.STAGGER);
		hideVisibleEffectParticles(living, TimothatysTrinketsModMobEffects.STUN_IMMUNITY);
	}

	public static void spawnSpiralKeepalive(LivingEntity living, boolean stunned, boolean bothControlsPresent) {
		if ((living.tickCount % SPIRAL_KEEPALIVE_INTERVAL) != 0)
			return;
		if (!(living.level() instanceof ServerLevel server))
			return;

		double yOffset = living.hasEffect(TimothatysTrinketsModMobEffects.HUBRIS)
				? HUBRIS_CONTROL_EFFECT_Y_OFFSET
				: SPIRAL_BASE_Y_OFFSET;
		if (stunned && bothControlsPresent)
			yOffset += STUNNED_STACKED_Y_OFFSET;
		server.sendParticles(
				stunned ? TimothatysTrinketsModParticleTypes.STUNNED_SPIRAL.get() : TimothatysTrinketsModParticleTypes.STAGGER_SPIRAL.get(),
				living.getX(), living.getY() + living.getBbHeight() + SPIRAL_FALLBACK_HEAD_Y_OFFSET + yOffset, living.getZ(),
				0,
				living.getId(), yOffset, 0.0D,
				1.0D
		);
	}

	private static void hideVisibleEffectParticles(LivingEntity living, Holder<MobEffect> effect) {
		MobEffectInstance instance = living.getEffect(effect);
		if (instance == null || !instance.isVisible())
			return;

		int duration = instance.getDuration();
		int amplifier = instance.getAmplifier();
		boolean ambient = instance.isAmbient();
		boolean showIcon = instance.showIcon();
		living.removeEffect(effect);
		living.addEffect(new MobEffectInstance(effect, duration, amplifier, ambient, false, showIcon));
	}
}
