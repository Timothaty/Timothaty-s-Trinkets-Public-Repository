package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.client.morgenshtern.MorgenshternDecapitationClientState;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.DrownedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.DrownedOuterLayer;
import net.minecraft.world.entity.monster.Drowned;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrownedOuterLayer.class)
public abstract class MorgenshternDrownedOuterLayerMixin {
	@Shadow
	@Final
	private DrownedModel<?> model;

	@Unique
	private boolean timothatys_trinkets$savedHeadVisible;

	@Unique
	private boolean timothatys_trinkets$hidingDrownedHead;

	@Inject(method = "render", at = @At("HEAD"), require = 0)
	private void timothatys_trinkets$hideDrownedOuterHead(
			PoseStack poseStack,
			MultiBufferSource buffer,
			int packedLight,
			Drowned entity,
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

		this.timothatys_trinkets$savedHeadVisible = this.model.head.visible;
		this.model.head.visible = false;
		this.timothatys_trinkets$hidingDrownedHead = true;
	}

	@Inject(method = "render", at = @At("RETURN"), require = 0)
	private void timothatys_trinkets$restoreDrownedOuterHead(
			PoseStack poseStack,
			MultiBufferSource buffer,
			int packedLight,
			Drowned entity,
			float limbSwing,
			float limbSwingAmount,
			float partialTicks,
			float ageInTicks,
			float netHeadYaw,
			float headPitch,
			CallbackInfo ci
	) {
		if (!this.timothatys_trinkets$hidingDrownedHead)
			return;

		this.model.head.visible =
				this.timothatys_trinkets$savedHeadVisible;
		this.timothatys_trinkets$hidingDrownedHead = false;
	}
}
