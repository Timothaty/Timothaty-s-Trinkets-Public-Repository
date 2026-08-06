package net.timothaty.timothatystrinkets.client.gorge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.compat.FirstPersonModelCompat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

@EventBusSubscriber(
		modid = TimothatysTrinketsMod.MODID,
		value = Dist.CLIENT
)
public final class GorgeFirstPersonRenderer {
	private GorgeFirstPersonRenderer() {
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRenderHand(RenderHandEvent event) {
		if (FirstPersonModelCompat.isTrueFirstPersonActive()
				|| event.getHand() != InteractionHand.MAIN_HAND)
			return;

		GorgeFirstPersonAnimation.VisualState state =
				GorgeFirstPersonAnimation.sample(event.getPartialTick());
		if (state == null)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null)
			return;

		event.setCanceled(true);
		if (state.bareHand()) {
			renderBareArm(minecraft, player, event, state);
		} else {
			renderItemSwing(minecraft, player, event, state);
		}
	}

	private static void renderBareArm(
			Minecraft minecraft,
			LocalPlayer player,
			RenderHandEvent event,
			GorgeFirstPersonAnimation.VisualState state
	) {
		if (player.isInvisible())
			return;

		boolean rightArm = state.mainArm() == HumanoidArm.RIGHT;
		float side = rightArm ? 1.0F : -1.0F;
		PoseStack poseStack = event.getPoseStack();
		poseStack.pushPose();
		applyHeldObjectBasePose(poseStack, side);
		applyFakeSwingPose(poseStack, side, state.progress());
		applyPlayerArmModelPose(poseStack, side);
		if (minecraft.getEntityRenderDispatcher()
				.getRenderer(player) instanceof PlayerRenderer renderer) {
			if (rightArm) {
				renderer.renderRightHand(
						poseStack,
						event.getMultiBufferSource(),
						event.getPackedLight(),
						player
				);
			} else {
				renderer.renderLeftHand(
						poseStack,
						event.getMultiBufferSource(),
						event.getPackedLight(),
						player
				);
			}
		}
		poseStack.popPose();
	}

	private static void renderItemSwing(
			Minecraft minecraft,
			LocalPlayer player,
			RenderHandEvent event,
			GorgeFirstPersonAnimation.VisualState state
	) {
		if (state.itemSnapshot().isEmpty())
			return;

		boolean rightArm = state.mainArm() == HumanoidArm.RIGHT;
		float side = rightArm ? 1.0F : -1.0F;

		PoseStack poseStack = event.getPoseStack();
		poseStack.pushPose();
		applyHeldObjectBasePose(poseStack, side);
		applyFakeSwingPose(poseStack, side, state.progress());
		minecraft.gameRenderer.itemInHandRenderer.renderItem(
				player,
				state.itemSnapshot(),
				rightArm
						? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
						: ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
				!rightArm,
				poseStack,
				event.getMultiBufferSource(),
				event.getPackedLight()
		);
		poseStack.popPose();
	}

	private static void applyHeldObjectBasePose(
			PoseStack poseStack,
			float side
	) {
		poseStack.translate(side * 0.56F, -0.52F, -0.72F);
	}

	private static void applyFakeSwingPose(
			PoseStack poseStack,
			float side,
			float progress
	) {
		float rootSwing = Mth.sin(
				Mth.sqrt(progress) * (float) Math.PI
		);
		float squaredSwing = Mth.sin(
				progress * progress * (float) Math.PI
		);
		poseStack.translate(
				-side * 0.24F * rootSwing,
				0.20F * rootSwing,
				0.13F * squaredSwing
		);
		poseStack.mulPose(
				Axis.XP.rotationDegrees(-34.0F * rootSwing)
		);
		poseStack.mulPose(
				Axis.YP.rotationDegrees(
						-side * (18.0F * rootSwing + 9.0F * squaredSwing)
				)
		);
		poseStack.mulPose(
				Axis.ZP.rotationDegrees(side * 28.0F * rootSwing)
		);
	}

	private static void applyPlayerArmModelPose(
			PoseStack poseStack,
			float side
	) {
		poseStack.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
		poseStack.translate(side * -1.0F, 3.6F, 3.5F);
		poseStack.mulPose(Axis.ZP.rotationDegrees(side * 120.0F));
		poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
		poseStack.mulPose(Axis.YP.rotationDegrees(side * -135.0F));
		poseStack.translate(side * 5.6F, 0.0F, 0.0F);
	}
}
