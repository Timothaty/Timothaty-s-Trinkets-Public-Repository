package net.timothaty.timothatystrinkets.client.hubris;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.compat.FirstPersonModelCompat;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris.HubrisAnimationVariant;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class HubrisFirstPersonRenderer {
	private HubrisFirstPersonRenderer() {
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRenderHand(RenderHandEvent event) {
		if (FirstPersonModelCompat.isTrueFirstPersonActive() || event.getHand() != InteractionHand.MAIN_HAND)
			return;

		HubrisActivationClientState.View state = HubrisActivationClientState.getLocalView(event.getPartialTick());
		if (state == null || state.weaponSnapshot().isEmpty())
			return;
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null)
			return;

		event.setCanceled(true);
		boolean rightArm = state.mainArm() == HumanoidArm.RIGHT;
		float side = rightArm ? 1.0F : -1.0F;
		PoseStack poseStack = event.getPoseStack();
		poseStack.pushPose();
		applyPose(poseStack, side, state.elapsedTicks(), state.variant());
		ItemDisplayContext context = rightArm
				? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
				: ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
		HubrisSwordItemTransform.apply(poseStack, state, state.mainArm(), context);
		minecraft.gameRenderer.itemInHandRenderer.renderItem(
				player,
				state.weaponSnapshot(),
				context,
				!rightArm,
				poseStack,
				event.getMultiBufferSource(),
				event.getPackedLight()
		);
		poseStack.popPose();
	}

	private static void applyPose(PoseStack poseStack, float side, float elapsedTicks, HubrisAnimationVariant variant) {
		float windup = smoothstep(elapsedTicks / 10.0F);
		float impact = smoothstep((elapsedTicks - 10.0F) / 4.0F);
		float release = smoothstep((elapsedTicks - 20.0F) / 9.0F);
		float active = 1.0F - release;
		float heavy = variant == HubrisAnimationVariant.HEAVY ? 1.0F : 0.0F;

		poseStack.translate(side * (0.56F + 0.08F * heavy), -0.50F, -0.72F);
		poseStack.translate(
				-side * (0.16F + 0.10F * heavy) * windup * active,
				(-0.10F * windup + (0.50F + 0.12F * heavy) * impact) * active,
				(0.18F * windup - 0.22F * impact) * active
		);
		poseStack.mulPose(Axis.XP.rotationDegrees((-52.0F * windup + (96.0F + 18.0F * heavy) * impact) * active));
		poseStack.mulPose(Axis.YP.rotationDegrees(-side * (24.0F * windup + 12.0F * heavy * impact) * active));
		poseStack.mulPose(Axis.ZP.rotationDegrees(side * (34.0F + 16.0F * heavy) * windup * active));
	}

	private static float smoothstep(float value) {
		float x = Mth.clamp(value, 0.0F, 1.0F);
		return x * x * (3.0F - 2.0F * x);
	}
}
