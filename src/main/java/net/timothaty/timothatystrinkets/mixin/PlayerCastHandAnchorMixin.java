package net.timothaty.timothatystrinkets.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.timothaty.timothatystrinkets.client.animation.PlayerCastHandAnchorTracker;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures final PAL-mutated arm transforms while the player-space PoseStack is still active. */
@Mixin(LivingEntityRenderer.class)
public abstract class PlayerCastHandAnchorMixin<T extends LivingEntity, M extends EntityModel<T>> {
	@Shadow
	protected M model;

	@Inject(
			method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At(
					value = "INVOKE",
					target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V",
					shift = At.Shift.BEFORE,
					ordinal = 0
			)
	)
	private void timothatys_trinkets$capturePlayerCastHandAnchors(
			T entity,
			float entityYaw,
			float partialTick,
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			int light,
			CallbackInfo ci
	) {
		if (entity instanceof AbstractClientPlayer player && this.model instanceof PlayerModel<?> playerModel)
			PlayerCastHandAnchorTracker.capture(player, playerModel, poseStack);
	}
}
