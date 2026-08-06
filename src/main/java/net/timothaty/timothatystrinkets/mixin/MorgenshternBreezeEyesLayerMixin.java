package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.client.morgenshtern.MorgenshternDecapitationClientState;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.BreezeEyesLayer;
import net.minecraft.world.entity.monster.breeze.Breeze;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BreezeEyesLayer.class)
public abstract class MorgenshternBreezeEyesLayerMixin {
	@Inject(
			method = "render",
			at = @At("HEAD"),
			cancellable = true,
			require = 0
	)
	private void timothatys_trinkets$hideDecapitatedBreezeEyes(
			PoseStack poseStack,
			MultiBufferSource buffer,
			int packedLight,
			Breeze entity,
			float limbSwing,
			float limbSwingAmount,
			float partialTicks,
			float ageInTicks,
			float netHeadYaw,
			float headPitch,
			CallbackInfo ci
	) {
		if (MorgenshternDecapitationClientState.isDecapitated(entity)) {
			ci.cancel();
		}
	}
}
