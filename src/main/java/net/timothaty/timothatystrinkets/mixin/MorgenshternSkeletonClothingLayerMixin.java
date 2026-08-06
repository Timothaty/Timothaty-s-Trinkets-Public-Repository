package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.client.morgenshtern.MorgenshternDecapitationClientState;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.SkeletonClothingLayer;
import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkeletonClothingLayer.class)
public abstract class MorgenshternSkeletonClothingLayerMixin {
	@Shadow
	@Final
	private SkeletonModel<?> layerModel;

	@Unique
	private boolean timothatys_trinkets$savedHeadVisible;

	@Unique
	private boolean timothatys_trinkets$savedHatVisible;

	@Unique
	private boolean timothatys_trinkets$hidingSkeletonHead;

	@Inject(method = "render", at = @At("HEAD"), require = 0)
	private void timothatys_trinkets$hideSkeletonClothingHead(
			PoseStack poseStack,
			MultiBufferSource buffer,
			int packedLight,
			Mob entity,
			float limbSwing,
			float limbSwingAmount,
			float partialTicks,
			float ageInTicks,
			float netHeadYaw,
			float headPitch,
			CallbackInfo ci
	) {
		if (!MorgenshternDecapitationClientState.isDecapitated(entity))
			return;

		this.timothatys_trinkets$savedHeadVisible =
				this.layerModel.head.visible;
		this.timothatys_trinkets$savedHatVisible =
				this.layerModel.hat.visible;
		this.layerModel.head.visible = false;
		this.layerModel.hat.visible = false;
		this.timothatys_trinkets$hidingSkeletonHead = true;
	}

	@Inject(method = "render", at = @At("RETURN"), require = 0)
	private void timothatys_trinkets$restoreSkeletonClothingHead(
			PoseStack poseStack,
			MultiBufferSource buffer,
			int packedLight,
			Mob entity,
			float limbSwing,
			float limbSwingAmount,
			float partialTicks,
			float ageInTicks,
			float netHeadYaw,
			float headPitch,
			CallbackInfo ci
	) {
		if (!this.timothatys_trinkets$hidingSkeletonHead)
			return;

		this.layerModel.head.visible =
				this.timothatys_trinkets$savedHeadVisible;
		this.layerModel.hat.visible =
				this.timothatys_trinkets$savedHatVisible;
		this.timothatys_trinkets$hidingSkeletonHead = false;
	}
}
