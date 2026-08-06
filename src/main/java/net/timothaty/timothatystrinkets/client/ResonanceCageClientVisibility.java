package net.timothaty.timothatystrinkets.client;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public final class ResonanceCageClientVisibility {
	private static final double MAX_FROZEN_SPEED_SQR = 0.0009D;

	private ResonanceCageClientVisibility() {
	}

	public static boolean shouldHide(LivingEntity entity) {
		return entity != null && looksLikeResonanceCaged(entity);
	}

	private static boolean looksLikeResonanceCaged(LivingEntity entity) {
		if (entity.hasEffect(TimothatysTrinketsModMobEffects.RESONANCE_CAGE)) {
			return true;
		}

		if (!entity.isInvisible() || !entity.isNoGravity()) {
			return false;
		}

		if (entity.getDeltaMovement().lengthSqr() > MAX_FROZEN_SPEED_SQR) {
			return false;
		}

		return !(entity instanceof Mob mob) || mob.isNoAi();
	}
}
