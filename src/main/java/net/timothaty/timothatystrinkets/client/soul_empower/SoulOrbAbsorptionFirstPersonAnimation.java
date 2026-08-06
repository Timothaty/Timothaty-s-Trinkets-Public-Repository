package net.timothaty.timothatystrinkets.client.soul_empower;

import net.minecraft.util.Mth;

public final class SoulOrbAbsorptionFirstPersonAnimation {
	private static final float VISIBILITY_ENTER_STEP = 0.08F;
	private static final float VISIBILITY_EXIT_STEP = 0.10F;
	private static final float PULL_ENTER_STEP = 0.32F;
	private static final float PULL_EXIT_STEP = 0.42F;
	private static final float PULSE_DECAY_PER_TICK = 0.13F;
	private static final float PULSE_INCREMENT = 0.72F;
	private static final float MAX_PULSE = 1.0F;

	private static float previousVisibility;
	private static float currentVisibility;
	private static float previousPullProgress;
	private static float currentPullProgress;
	private static float previousPulse;
	private static float currentPulse;
	private static int animationTicks;

	private SoulOrbAbsorptionFirstPersonAnimation() {
	}

	public static void tick(boolean channeling, boolean pulling) {
		previousVisibility = currentVisibility;
		previousPullProgress = currentPullProgress;
		previousPulse = currentPulse;
		currentVisibility = Mth.approach(currentVisibility, channeling ? 1.0F : 0.0F,
				channeling ? VISIBILITY_ENTER_STEP : VISIBILITY_EXIT_STEP);
		currentPullProgress = Mth.approach(currentPullProgress, pulling ? 1.0F : 0.0F,
				pulling ? PULL_ENTER_STEP : PULL_EXIT_STEP);
		currentPulse = Math.max(0.0F, currentPulse - PULSE_DECAY_PER_TICK);
		animationTicks++;
	}

	public static void pulse() {
		currentPulse = Math.min(MAX_PULSE, currentPulse + PULSE_INCREMENT);
		previousPulse = Math.min(previousPulse, currentPulse);
	}

	public static VisualPose sample(float partialTick) {
		float visibility = Mth.lerp(partialTick, previousVisibility, currentVisibility);
		float pullProgress = Mth.lerp(partialTick, previousPullProgress, currentPullProgress);
		float pulse = Mth.lerp(partialTick, previousPulse, currentPulse);
		float time = animationTicks + partialTick;
		float fast = Mth.sin(time * 2.7F);
		float detail = Mth.sin(time * 5.1F + 0.8F);
		float vertical = Mth.sin(time * 3.6F + 1.4F);
		float pullWave = (fast * 0.7F + detail * 0.3F) * pullProgress;
		float pullVerticalWave = (vertical * 0.75F + detail * 0.25F) * pullProgress;
		float pulseWave = Mth.sin(time * 2.4F) * pulse;
		return new VisualPose(visibility, visibility, pullProgress, pulse, pullWave, pullVerticalWave, pulseWave);
	}

	public static float getOffhandEquippedProgress(float partialTick) {
		float visibility = Mth.clamp(sample(partialTick).visibility(), 0.0F, 1.0F);
		float easedVisibility = visibility * visibility * (3.0F - 2.0F * visibility);
		return 1.0F - easedVisibility;
	}

	public static boolean isVisible() {
		return currentVisibility > 0.001F || previousVisibility > 0.001F || currentPullProgress > 0.001F
				|| previousPullProgress > 0.001F || currentPulse > 0.001F || previousPulse > 0.001F;
	}

	public static void reset() {
		previousVisibility = 0.0F;
		currentVisibility = 0.0F;
		previousPullProgress = 0.0F;
		currentPullProgress = 0.0F;
		previousPulse = 0.0F;
		currentPulse = 0.0F;
		animationTicks = 0;
	}

	public record VisualPose(float visibility, float holdProgress, float pullProgress, float pulse,
			float pullWave, float pullVerticalWave, float pulseWave) {
		public boolean visible() {
			return visibility > 0.001F || pullProgress > 0.001F || pulse > 0.001F;
		}
	}
}
