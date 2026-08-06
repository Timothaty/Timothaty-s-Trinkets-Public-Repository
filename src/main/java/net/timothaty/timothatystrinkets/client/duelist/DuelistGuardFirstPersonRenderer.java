package net.timothaty.timothatystrinkets.client.duelist;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.DuelistGuardClient;
import net.timothaty.timothatystrinkets.client.compat.FirstPersonModelCompat;
import net.timothaty.timothatystrinkets.client.gorge.GorgeFirstPersonAnimation;
import net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet.DuelistGauntletCurios;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class DuelistGuardFirstPersonRenderer {
	private DuelistGuardFirstPersonRenderer() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onClientTick(ClientTickEvent.Post event) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || !player.isAlive() || player.isDeadOrDying() || player.isRemoved()) {
			DuelistGuardFirstPersonAnimation.reset();
			return;
		}
		DuelistGuardFirstPersonAnimation.tick();
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRenderHand(RenderHandEvent event) {
		if (FirstPersonModelCompat.isTrueFirstPersonActive())
			return;

		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || !shouldRenderGuardPose(player, event))
			return;

		DuelistGuardFirstPersonAnimation.VisualPose visualPose = DuelistGuardFirstPersonAnimation.sample(event.getPartialTick());
		if (!visualPose.visible())
			return;

		event.setCanceled(true);
		renderGuardedSword(minecraft, player, event, visualPose);
	}

	private static boolean shouldRenderGuardPose(LocalPlayer player, RenderHandEvent event) {
		if (!player.isAlive() || player.isDeadOrDying() || player.isRemoved())
			return false;
		if (GorgeFirstPersonAnimation.isActive())
			return false;
		if (!DuelistGuardClient.isVisuallyGuarding())
			return false;
		if (!DuelistGauntletCurios.hasGauntletEquipped(player))
			return false;
		InteractionHand weaponHand = getGuardWeaponHand(player);
		return event.getHand() == weaponHand && isSword(event.getItemStack());
	}

	private static InteractionHand getGuardWeaponHand(LocalPlayer player) {
		return isSword(player.getMainHandItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
	}

	private static void renderGuardedSword(Minecraft minecraft, LocalPlayer player, RenderHandEvent event, DuelistGuardFirstPersonAnimation.VisualPose visualPose) {
		PoseStack poseStack = event.getPoseStack();
		ItemStack stack = event.getItemStack();
		HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
		boolean rightArm = arm == HumanoidArm.RIGHT;

		poseStack.pushPose();
		try {
			applyVanillaIdleItemBase(poseStack, rightArm, event.getEquipProgress());
			applyGuardPose(poseStack, rightArm, visualPose);
			minecraft.gameRenderer.itemInHandRenderer.renderItem(
					player,
					stack,
					rightArm ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
					!rightArm,
					poseStack,
					event.getMultiBufferSource(),
					event.getPackedLight()
			);
		} finally {
			poseStack.popPose();
		}
	}

	private static void applyVanillaIdleItemBase(PoseStack poseStack, boolean rightArm, float equipProgress) {
		int armSide = rightArm ? 1 : -1;
		poseStack.translate(armSide * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
	}

	private static void applyGuardPose(PoseStack poseStack, boolean rightArm, DuelistGuardFirstPersonAnimation.VisualPose visualPose) {
		float active = visualPose.active();
		float side = visualPose.side();
		int armSide = rightArm ? 1 : -1;
		float centerOffset = -0.42F * armSide;
		float sideOffset = 0.24F * side;

		poseStack.translate((centerOffset + sideOffset) * active, 0.20F * active, 0.18F * active);
		poseStack.mulPose(Axis.XP.rotationDegrees(-38.0F * active));
		poseStack.mulPose(Axis.YP.rotationDegrees((armSide * 8.0F - side * 18.0F) * active));
		poseStack.mulPose(Axis.ZP.rotationDegrees((armSide * 8.0F - side * 16.0F) * active));
	}

	private static boolean isSword(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.getItem() instanceof SwordItem;
	}
}
