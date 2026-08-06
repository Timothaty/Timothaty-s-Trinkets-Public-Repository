package net.timothaty.timothatystrinkets.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.vampiric_fangs.VampiricFangsEvents;
import net.timothaty.timothatystrinkets.util.VampiricFangsCurios;
import net.timothaty.timothatystrinkets.util.VampiricFangsData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class VampiricFangsInsatiableHud {
	private VampiricFangsInsatiableHud() {}

	private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/screens/insatiable_background.png");
	private static final ResourceLocation FILL = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/screens/insatiable.png");
	private static final int ICON_SIZE = 16;
	private static final int CAST_ANIMATION_TICKS = 20;
	private static final float ACTIVE_ALPHA = 0.85F;
	private static final float IDLE_ALPHA = 0.35F;

	private static int lastCooldownLeft = 0;
	private static long castAnimationStartedAt = -1000L;

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null || mc.level == null || mc.options.hideGui)
			return;

		ItemStack fangs = VampiricFangsCurios.getEquippedFangs(player);
		if (fangs.isEmpty()) {
			lastCooldownLeft = 0;
			return;
		}

		LivingEntity target = VampiricFangsEvents.findLookedAtTarget(player, VampiricFangsEvents.INSATIABLE_RANGE);
		int cooldownLeft = VampiricFangsEvents.getInsatiableCooldownLeft(player);
		if (lastCooldownLeft == 0 && cooldownLeft > 0) {
			castAnimationStartedAt = mc.level.getGameTime();
		}
		lastCooldownLeft = cooldownLeft;

		int screenWidth = mc.getWindow().getGuiScaledWidth();
		int screenHeight = mc.getWindow().getGuiScaledHeight();
		int x = screenWidth / 2 - 27;
		int y = screenHeight / 2 - 8;
		float alpha = target == null ? IDLE_ALPHA : ACTIVE_ALPHA;

		int animationAge = getCastAnimationAge(mc);
		if (animationAge >= 0) {
			float shakeStrength = 1.0F - animationAge / (float) CAST_ANIMATION_TICKS;
			x += Math.round(Mth.sin(animationAge * 2.9F) * 2.0F * shakeStrength);
			y += Math.round(Mth.sin(animationAge * 4.1F) * 1.25F * shakeStrength);
		}
		float scale = getCastAnimationScale(animationAge);

		GuiGraphics gui = event.getGuiGraphics();
		RenderSystem.enableBlend();
		gui.pose().pushPose();
		if (scale != 1.0F) {
			float centerX = x + ICON_SIZE / 2.0F;
			float centerY = y + ICON_SIZE / 2.0F;
			gui.pose().translate(centerX, centerY, 0.0F);
			gui.pose().scale(scale, scale, 1.0F);
			gui.pose().translate(-centerX, -centerY, 0.0F);
		}

		gui.setColor(1.0F, 1.0F, 1.0F, alpha);
		gui.blit(BACKGROUND, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

		if (target != null) {
			double fames = VampiricFangsData.getFames(fangs);
			double cost = VampiricFangsEvents.getInsatiableCost(target);
			if (cooldownLeft > 0) {
				float cooldownProgress = 1.0F - Mth.clamp(cooldownLeft / (float) VampiricFangsEvents.INSATIABLE_COOLDOWN_TICKS, 0.0F, 1.0F);
				renderFill(gui, x, y, cooldownProgress, 1.0F, 0.0F, 0.0F, ACTIVE_ALPHA);
			} else {
				float famesProgress = cost <= 0.0D ? 1.0F : (float) Mth.clamp(fames / cost, 0.0D, 1.0D);
				renderFill(gui, x, y, famesProgress, 1.0F, 1.0F, 1.0F, ACTIVE_ALPHA);
			}
		}

		gui.pose().popPose();
		gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
	}

	private static void renderFill(GuiGraphics gui, int x, int y, float progress, float r, float g, float b, float alpha) {
		int fillHeight = Mth.clamp(Math.round(ICON_SIZE * Mth.clamp(progress, 0.0F, 1.0F)), 0, ICON_SIZE);
		if (fillHeight <= 0)
			return;
		int fillY = y + ICON_SIZE - fillHeight;
		gui.setColor(r, g, b, alpha);
		gui.blit(FILL, x, fillY, 0, ICON_SIZE - fillHeight, ICON_SIZE, fillHeight, ICON_SIZE, ICON_SIZE);
	}

	private static int getCastAnimationAge(Minecraft mc) {
		if (mc.level == null || castAnimationStartedAt < 0)
			return -1;
		long age = mc.level.getGameTime() - castAnimationStartedAt;
		if (age < 0 || age > CAST_ANIMATION_TICKS)
			return -1;
		return (int) age;
	}

	private static float getCastAnimationScale(int animationAge) {
		if (animationAge < 0)
			return 1.0F;
		float t = Mth.clamp(animationAge / (float) CAST_ANIMATION_TICKS, 0.0F, 1.0F);
		float pop = 1.0F - t;
		return 1.0F + 0.45F * pop * pop;
	}
}
