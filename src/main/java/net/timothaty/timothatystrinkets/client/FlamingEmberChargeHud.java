package net.timothaty.timothatystrinkets.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.mechanics.flaming_ember.FlamingEmberData;
import net.timothaty.timothatystrinkets.util.TimothatysCuriosHelper;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class FlamingEmberChargeHud {
	private static final ResourceLocation BAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/screens/ember_charge_bars.png");
	private static final int TEXTURE_WIDTH = 98;
	private static final int TEXTURE_HEIGHT = 34;
	private static final int BAR_WIDTH = 98;
	private static final int BAR_HEIGHT = 17;
	private static final int LEFT_MARGIN = 8;
	private static final int FILL_X_OFFSET = 1;
	private static final int FILL_Y_OFFSET = -1;

	private FlamingEmberChargeHud() {
	}

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		if (player == null || minecraft.level == null || minecraft.options.hideGui)
			return;

		ItemStack ember = TimothatysCuriosHelper.findCurio(player, TimothatysTrinketsModItems.FLAMING_EMBER.get());
		if (ember.isEmpty())
			return;

		int x = LEFT_MARGIN;
		int y = minecraft.getWindow().getGuiScaledHeight() / 2 - BAR_HEIGHT / 2;
		renderBar(event.getGuiGraphics(), x, y, getChargeProgress(ember));
	}

	private static void renderBar(GuiGraphics gui, int x, int y, float progress) {
		int fillWidth = Mth.clamp(Math.round(BAR_WIDTH * Mth.clamp(progress, 0.0F, 1.0F)), 0, BAR_WIDTH);

		RenderSystem.enableBlend();
		gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		gui.blit(BAR_TEXTURE, x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

		if (fillWidth > 0) {
			gui.blit(BAR_TEXTURE, x + FILL_X_OFFSET, y + FILL_Y_OFFSET, 0, BAR_HEIGHT, fillWidth, BAR_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
		}

		gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
	}

	private static float getChargeProgress(ItemStack ember) {
		return FlamingEmberData.getHeatProgress(ember);
	}
}
