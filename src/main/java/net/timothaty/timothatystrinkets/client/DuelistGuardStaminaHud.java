package net.timothaty.timothatystrinkets.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet.DuelistGauntletCurios;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class DuelistGuardStaminaHud {
	private static final ResourceLocation BAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/screens/duelist_stamina_bar.png");
	private static final int TEXTURE_WIDTH = 10;
	private static final int TEXTURE_HEIGHT = 32;
	private static final int ICON_WIDTH = 10;
	private static final int ICON_HEIGHT = 16;
	private static final int VERTICAL_OFFSET = 16;
	private static final int FILL_Y_OFFSET = 1;

	private DuelistGuardStaminaHud() {
	}

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		if (player == null || minecraft.level == null || minecraft.options.hideGui)
			return;
		if (!DuelistGauntletCurios.hasGauntletEquipped(player))
			return;

		int x = (minecraft.getWindow().getGuiScaledWidth() - ICON_WIDTH) / 2;
		int y = minecraft.getWindow().getGuiScaledHeight() / 2 + VERTICAL_OFFSET;
		renderBar(event.getGuiGraphics(), x, y, DuelistGuardClient.getSyncedStaminaProgress());
	}

	private static void renderBar(GuiGraphics gui, int x, int y, float progress) {
		int fillHeight = Mth.clamp(Math.round(ICON_HEIGHT * Mth.clamp(progress, 0.0F, 1.0F)), 0, ICON_HEIGHT);

		RenderSystem.enableBlend();
		gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		gui.blit(BAR_TEXTURE, x, y, 0, ICON_HEIGHT, ICON_WIDTH, ICON_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

		if (fillHeight > 0) {
			int sourceY = ICON_HEIGHT - fillHeight;
			int targetY = y + ICON_HEIGHT - fillHeight + FILL_Y_OFFSET;
			gui.blit(BAR_TEXTURE, x, targetY, 0, sourceY, ICON_WIDTH, fillHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
		}

		gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
	}
}
