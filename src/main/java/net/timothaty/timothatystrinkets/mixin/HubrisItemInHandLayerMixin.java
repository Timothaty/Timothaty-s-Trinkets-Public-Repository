package net.timothaty.timothatystrinkets.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.timothaty.timothatystrinkets.client.hubris.HubrisActivationClientState;
import net.timothaty.timothatystrinkets.client.hubris.HubrisSwordItemTransform;
import net.timothaty.timothatystrinkets.client.hubris.PlayerHandAnchorTracker;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris.HubrisData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class HubrisItemInHandLayerMixin {
	@Inject(
			method = "renderArmWithItem",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
			)
	)
	private void timothatys_trinkets$transformHubrisSwordAndCaptureAnchor(
			LivingEntity entity,
			ItemStack stack,
			ItemDisplayContext context,
			HumanoidArm arm,
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			int packedLight,
			CallbackInfo ci
	) {
		if (!(entity instanceof AbstractClientPlayer player) || arm != player.getMainArm())
			return;

		float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
		HubrisActivationClientState.View state = HubrisActivationClientState.getView(player, partialTick);
		if (state != null) {
			HubrisSwordItemTransform.apply(poseStack, state, arm, context);
		}

		if (player.hasEffect(TimothatysTrinketsModMobEffects.HUBRIS)
				&& (stack.is(ItemTags.SWORDS) || stack.is(HubrisData.HEAVY_ARMS)))
			PlayerHandAnchorTracker.captureThirdPerson(player, poseStack);
	}
}
