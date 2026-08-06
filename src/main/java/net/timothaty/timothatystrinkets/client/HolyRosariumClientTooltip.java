package net.timothaty.timothatystrinkets.client;

import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumData;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumTooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class HolyRosariumClientTooltip implements ClientTooltipComponent {
	private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("container/bundle/background");
	private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/bundle/slot");
	private static final int SLOT_WIDTH = 18;
	private static final int SLOT_HEIGHT = 20;
	private static final int BORDER_WIDTH = 1;
	private static final int NAME_COLOR = 0xFBAE02;
	private static final int NAME_HEIGHT = 9;
	private static final int NAME_SPACING = 2;
	private static final float[][] SECTION_COLORS = {
		{0x54 / 255.0F, 0xB8 / 255.0F, 0xD0 / 255.0F},
		{0xD8 / 255.0F, 0xA8 / 255.0F, 0x3E / 255.0F}
	};
	private static final int BACKGROUND_WIDTH = HolyRosariumData.SLOT_COUNT * SLOT_WIDTH + BORDER_WIDTH * 2;
	private static final int BACKGROUND_HEIGHT = SLOT_HEIGHT + BORDER_WIDTH * 2;

	private final List<ItemStack> beads;
	private final Component combinationName;

	public HolyRosariumClientTooltip(HolyRosariumTooltip tooltip) {
		this.beads = tooltip.beads();
		this.combinationName = tooltip.combinationName().orElse(null);
	}

	@Override
	public int getHeight() {
		return BACKGROUND_HEIGHT + 4 + (this.combinationName == null ? 0 : NAME_HEIGHT + NAME_SPACING);
	}

	@Override
	public int getWidth(Font font) {
		return this.combinationName == null
				? BACKGROUND_WIDTH
				: Math.max(BACKGROUND_WIDTH, font.width(this.combinationName));
	}

	@Override
	public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
		int panelX = x;
		int panelY = y;
		if (this.combinationName != null) {
			guiGraphics.drawString(font, this.combinationName, x, y, NAME_COLOR);
			panelY += NAME_HEIGHT + NAME_SPACING;
		}

		renderColoredBackground(guiGraphics, panelX, panelY);

		for (int index = 0; index < HolyRosariumData.SLOT_COUNT; index++) {
			int slotX = panelX + index * SLOT_WIDTH + BORDER_WIDTH;
			int slotY = panelY + BORDER_WIDTH;
			setColor(guiGraphics, SECTION_COLORS[index]);
			guiGraphics.blitSprite(SLOT_SPRITE, slotX, slotY, 0, SLOT_WIDTH, SLOT_HEIGHT);
			resetColor(guiGraphics);

			if (index < beads.size()) {
				ItemStack bead = beads.get(index);
				guiGraphics.renderItem(bead, slotX + 1, slotY + 1, index);
				guiGraphics.renderItemDecorations(font, bead, slotX + 1, slotY + 1);
			}
		}
	}

	private static void renderColoredBackground(GuiGraphics guiGraphics, int x, int y) {
		int sectionWidth = BACKGROUND_WIDTH / HolyRosariumData.SLOT_COUNT;
		for (int index = 0; index < HolyRosariumData.SLOT_COUNT; index++) {
			int sectionX = x + index * sectionWidth;
			guiGraphics.enableScissor(sectionX, y, sectionX + sectionWidth, y + BACKGROUND_HEIGHT);
			setColor(guiGraphics, SECTION_COLORS[index]);
			guiGraphics.blitSprite(BACKGROUND_SPRITE, x, y, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
			resetColor(guiGraphics);
			guiGraphics.disableScissor();
		}
	}

	private static void setColor(GuiGraphics guiGraphics, float[] color) {
		guiGraphics.setColor(color[0], color[1], color[2], 1.0F);
	}

	private static void resetColor(GuiGraphics guiGraphics) {
		guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
	}
}
