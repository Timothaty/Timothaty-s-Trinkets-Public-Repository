package net.timothaty.timothatystrinkets.client.morgenshtern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.compat.FirstPersonModelCompat;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;

@EventBusSubscriber(
		modid = TimothatysTrinketsMod.MODID,
		value = Dist.CLIENT
)
public final class MorgenshternFirstPersonRenderer {
	private MorgenshternFirstPersonRenderer() {
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onRenderHand(RenderHandEvent event) {
		if (FirstPersonModelCompat.isTrueFirstPersonActive()
				|| event.getHand() != InteractionHand.MAIN_HAND
				|| !event.getItemStack().is(
						TimothatysTrinketsModItems.MORGENSHTERN.get()
				))
			return;

		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null)
			return;

		float ageInTicks = player.tickCount + event.getPartialTick();
		float elapsed = MorgenshternStrikeClientState.elapsedTicks(
				player,
				ageInTicks
		);
		if (elapsed < 0.0F)
			return;

		event.setCanceled(true);
		renderAnimatedItem(minecraft, player, event, elapsed);
	}

	private static void renderAnimatedItem(
			Minecraft minecraft,
			LocalPlayer player,
			RenderHandEvent event,
			float elapsed
	) {
		boolean rightArm = player.getMainArm() == HumanoidArm.RIGHT;
		float side = rightArm ? 1.0F : -1.0F;
		float progress = Mth.clamp(
				elapsed / MorgenshternOberhauAnimation.DURATION_TICKS,
				0.0F,
				1.0F
		);
		MorgenshternOberhauAnimation.Pose armPose =
				MorgenshternOberhauAnimation.sample(elapsed);
		float overhead = Mth.clamp(
				-armPose.xDegrees() / 155.0F,
				0.0F,
				1.0F
		);
		float impact = Mth.sin(progress * (float) Math.PI);

		PoseStack poseStack = event.getPoseStack();
		poseStack.pushPose();
		poseStack.translate(
				side * 0.56F,
				-0.52F + event.getEquipProgress() * -0.6F,
				-0.72F
		);
		poseStack.translate(
				-side * 0.22F * overhead,
				0.34F * overhead - 0.08F * impact,
				0.18F * overhead
		);
		poseStack.mulPose(
				Axis.XP.rotationDegrees(-108.0F * overhead)
		);
		poseStack.mulPose(
				Axis.YP.rotationDegrees(
						side * (20.0F * overhead - 16.0F * impact)
				)
		);
		poseStack.mulPose(
				Axis.ZP.rotationDegrees(
						side * (-24.0F * overhead + 12.0F * impact)
				)
		);

		minecraft.gameRenderer.itemInHandRenderer.renderItem(
				player,
				event.getItemStack(),
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
}
