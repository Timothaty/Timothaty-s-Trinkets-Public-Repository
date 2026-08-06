package net.timothaty.timothatystrinkets.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.timothaty.timothatystrinkets.client.renderer.curio.PalFirstPersonHandCurioRenderer;

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

/**
 * PAL 1.1.5 filters non-armor and non-item player layers during its special
 * first-person pass, and NeoForge's post-render event runs after this pose has
 * been popped. This hook admits only our hand-curio pass at the last stable
 * point where the PAL-mutated model and player-space PoseStack coexist.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class PalFirstPersonHandCurioRenderMixin<T extends LivingEntity, M extends EntityModel<T>> {
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
	private void timothatys_trinkets$renderPalFirstPersonHandCurios(
			T entity,
			float entityYaw,
			float partialTick,
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			int light,
			CallbackInfo ci
	) {
		if (entity instanceof AbstractClientPlayer player && this.model instanceof PlayerModel<?> playerModel) {
			PalFirstPersonHandCurioRenderer.render(
					player,
					playerModel,
					poseStack,
					bufferSource,
					light
			);
		}
	}
}
