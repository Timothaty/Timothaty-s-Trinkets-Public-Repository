package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.mechanics.pagans_charm.PaganCharmMeditationPlayerState;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class PaganCharmMeditationEyeHeightMixin {
	@Unique
	private static final float timothatys_trinkets$MEDITATION_EYE_HEIGHT = 1.05F;

	@Inject(method = "getEyeHeight()F", at = @At("RETURN"), cancellable = true)
	private void timothatys_trinkets$getMeditationEyeHeight(CallbackInfoReturnable<Float> cir) {
		float adjustedEyeHeight = this.timothatys_trinkets$getAdjustedMeditationEyeHeight(cir.getReturnValue());
		if (adjustedEyeHeight >= 0.0F)
			cir.setReturnValue(adjustedEyeHeight);
	}

	@Inject(method = "getEyeHeight(Lnet/minecraft/world/entity/Pose;)F", at = @At("RETURN"), cancellable = true)
	private void timothatys_trinkets$getMeditationEyeHeightForPose(Pose pose, CallbackInfoReturnable<Float> cir) {
		float adjustedEyeHeight = this.timothatys_trinkets$getAdjustedMeditationEyeHeight(cir.getReturnValue());
		if (adjustedEyeHeight >= 0.0F)
			cir.setReturnValue(adjustedEyeHeight);
	}

	@Inject(method = "getEyeY()D", at = @At("RETURN"), cancellable = true)
	private void timothatys_trinkets$getMeditationEyeY(CallbackInfoReturnable<Double> cir) {
		Entity entity = (Entity) (Object) this;
		float baseEyeHeight = (float) (cir.getReturnValue() - entity.getY());
		float adjustedEyeHeight = this.timothatys_trinkets$getAdjustedMeditationEyeHeight(baseEyeHeight);
		if (adjustedEyeHeight >= 0.0F)
			cir.setReturnValue(entity.getY() + adjustedEyeHeight);
	}

	@Unique
	private float timothatys_trinkets$getAdjustedMeditationEyeHeight(float baseEyeHeight) {
		Entity entity = (Entity) (Object) this;
		if (!(entity instanceof PaganCharmMeditationPlayerState meditationState))
			return -1.0F;

		int phase = meditationState.timothatys_trinkets$getPaganCharmMeditationPhase(entity.tickCount);
		if (phase == PaganCharmMeditationPlayerState.PHASE_NONE)
			return -1.0F;

		float activeTicks = meditationState.timothatys_trinkets$getPaganCharmMeditationActiveTicks(entity.tickCount);
		float progress = Mth.clamp(activeTicks / PaganCharmMeditationPlayerState.MEDITATE_TICKS, 0.0F, 1.0F);
		float smoothedProgress = progress * progress * (3.0F - 2.0F * progress);
		return Mth.lerp(smoothedProgress, baseEyeHeight, timothatys_trinkets$MEDITATION_EYE_HEIGHT);
	}
}
