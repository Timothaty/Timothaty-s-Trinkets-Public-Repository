package net.timothaty.timothatystrinkets.client;

import net.timothaty.timothatystrinkets.util.UndeadificationEntityStateHelper;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class UndeadificationShakeHelper {
	private static final int SHAKE_RAMP_TICKS = 200;
	private static final float MIN_SHAKE_STRENGTH = 0.3F;
	private static final float MAX_SHAKE_STRENGTH = 1.0F;
	private static final float ATTRIBUTE_FALLBACK_SHAKE_STRENGTH = 0.75F;

	private UndeadificationShakeHelper() {
	}

	public static float getShakeStrength(LivingEntity entity) {
		MobEffectInstance instance = UndeadificationEntityStateHelper.findUndeadification(entity);
		if (instance != null) {
			return calculateShakeStrength(instance.getDuration());
		}

		return UndeadificationEntityStateHelper.hasUndeadificationVisualMarker(entity) ? ATTRIBUTE_FALLBACK_SHAKE_STRENGTH : 0.0F;
	}

	private static float calculateShakeStrength(int remainingDurationTicks) {
		float progressToEnd = 1.0F - Mth.clamp((float) remainingDurationTicks / (float) SHAKE_RAMP_TICKS, 0.0F, 1.0F);
		return Mth.lerp(progressToEnd, MIN_SHAKE_STRENGTH, MAX_SHAKE_STRENGTH);
	}
}
