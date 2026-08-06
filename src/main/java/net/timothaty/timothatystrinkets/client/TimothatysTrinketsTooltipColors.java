package net.timothaty.timothatystrinkets.client;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

import java.util.Map;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class TimothatysTrinketsTooltipColors {
	private static final TooltipPalette BLASPHEMY = new TooltipPalette(
			0xF0141316,
			0xFF4E4B54,
			0xFF29272D
	);
	private static final TooltipPalette GNOSIS = new TooltipPalette(
			0xF0140E1B,
			0xFFC77DFF,
			0xFF6B3A85
	);
	private static final TooltipPalette SAINT = new TooltipPalette(
			0xF0141414,
			0xFFFFFFFF,
			0xFF777777
	);

	private static final Map<ResourceLocation, TooltipPalette> TOOLTIP_COLORS = Map.ofEntries(
			entry("bead_of_blasphemy", BLASPHEMY),
			entry("bead_of_gnosis", GNOSIS),
			entry("bead_of_the_saint", SAINT)
	);

	private TimothatysTrinketsTooltipColors() {}

	private static Map.Entry<ResourceLocation, TooltipPalette> entry(String itemId, TooltipPalette palette) {
		return Map.entry(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, itemId), palette);
	}

	@SubscribeEvent
	public static void onTooltipColor(RenderTooltipEvent.Color event) {
		ItemStack stack = event.getItemStack();
		if (stack.isEmpty())
			return;

		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		TooltipPalette palette = TOOLTIP_COLORS.get(itemId);
		if (palette == null)
			return;

		event.setBackground(palette.background());
		event.setBorderStart(palette.borderStart());
		event.setBorderEnd(palette.borderEnd());
	}

	private record TooltipPalette(
			int background,
			int borderStart,
			int borderEnd
	) {}
}
