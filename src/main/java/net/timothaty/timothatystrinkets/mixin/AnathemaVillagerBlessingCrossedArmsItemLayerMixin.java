package net.timothaty.timothatystrinkets.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaVillagerBlessingState;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrossedArmsItemLayer.class)
public abstract class AnathemaVillagerBlessingCrossedArmsItemLayerMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$hideBlessingHeldItem(
			PoseStack poseStack,
			MultiBufferSource buffer,
			int packedLight,
			LivingEntity entity,
			float limbSwing,
			float limbSwingAmount,
			float partialTicks,
			float ageInTicks,
			float netHeadYaw,
			float headPitch,
			CallbackInfo ci
	) {
		if (entity instanceof AnathemaVillagerBlessingState state
				&& state.timothatys_trinkets$isBlessingsAnimationActive())
			ci.cancel();
	}
}
