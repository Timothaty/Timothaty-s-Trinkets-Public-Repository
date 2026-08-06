package net.timothaty.timothatystrinkets.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.timothaty.timothatystrinkets.client.soul_empower.SoulOrbAbsorptionClient;
import net.timothaty.timothatystrinkets.client.soul_empower.SoulOrbAbsorptionFirstPersonAnimation;
import net.timothaty.timothatystrinkets.client.soul_empower.SoulOrbAbsorptionFirstPersonPose;

@Mixin(ItemInHandRenderer.class)
public abstract class SoulOrbAbsorptionItemInHandRendererMixin {
	@Invoker("renderPlayerArm")
	protected abstract void timothatys_trinkets$invokeRenderPlayerArm(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
			float equippedProgress, float swingProgress, HumanoidArm side);

	@Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$renderEmptyAbsorbingOffHand(AbstractClientPlayer player, float partialTicks, float pitch,
			InteractionHand hand, float swingProgress, ItemStack stack, float equippedProgress, PoseStack poseStack,
			MultiBufferSource buffer, int combinedLight, CallbackInfo ci) {
		if (hand != InteractionHand.OFF_HAND || !stack.isEmpty() || player.isInvisible() || player.isScoping()) {
			return;
		}

		HumanoidArm side = player.getMainArm().getOpposite();
		if (!SoulOrbAbsorptionClient.shouldTransformArm(side)) {
			return;
		}

		float customEquippedProgress = SoulOrbAbsorptionFirstPersonAnimation.getOffhandEquippedProgress(partialTicks);
		poseStack.pushPose();
		this.timothatys_trinkets$invokeRenderPlayerArm(poseStack, buffer, combinedLight, customEquippedProgress, swingProgress, side);
		poseStack.popPose();
		ci.cancel();
	}

	@Inject(method = "renderPlayerArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V"))
	private void timothatys_trinkets$applyRightSoulAbsorptionPose(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
			float equippedProgress, float swingProgress, HumanoidArm side, CallbackInfo ci) {
		applyPose(poseStack, side);
	}

	@Inject(method = "renderPlayerArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;)V"))
	private void timothatys_trinkets$applyLeftSoulAbsorptionPose(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
			float equippedProgress, float swingProgress, HumanoidArm side, CallbackInfo ci) {
		applyPose(poseStack, side);
	}

	private static void applyPose(PoseStack poseStack, HumanoidArm side) {
		float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
		SoulOrbAbsorptionFirstPersonPose.apply(poseStack, side, partialTick);
	}
}
