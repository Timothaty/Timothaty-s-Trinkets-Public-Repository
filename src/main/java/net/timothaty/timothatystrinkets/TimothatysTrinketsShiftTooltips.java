package net.timothaty.timothatystrinkets;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.timothaty.timothatystrinkets.client.TimothatysTrinketsSpecialTooltipLines;

import java.util.List;
import java.util.Map;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class TimothatysTrinketsShiftTooltips {
	private TimothatysTrinketsShiftTooltips() {}

	private static final Component HOLD_SHIFT = Component
			.translatable("tooltip." + TimothatysTrinketsMod.MODID + ".hold_shift")
			.withStyle(ChatFormatting.GOLD);

	private static final Component SEPARATOR = Component.literal("────────────").withStyle(ChatFormatting.DARK_GRAY);

	private static final Map<ResourceLocation, List<Component>> SHIFT_TEXT = Map.ofEntries(
			Map.entry(id("undead_knights_armlet"), List.of(
					lore("undead_knights_armlet.1")
			))
			,Map.entry(id("refreshing_chalice"), List.of(
					lore("refreshing_chalice.1")
			))
			,Map.entry(id("pagans_charm"), List.of(
					lore("pagans_charm.1")
			))
			,Map.entry(id("holy_rosarium"), List.of(
					lore("holy_rosarium.1")
			))
			,Map.entry(id("belt_of_outcast"), List.of(
					lore("belt_of_outcast.1")
			))
			,Map.entry(id("fangs"), List.of(
					lore("fangs.1")
			))
			,Map.entry(id("drums_of_haste"), List.of(
					lore("drums_of_haste.1")
			))
			,Map.entry(id("farmers_ring"), List.of(
					lore("farmers_ring.1")
			))
			,Map.entry(id("cleansing_dust"), List.of(
					lore("cleansing_dust.1")
			))
			,Map.entry(id("ritual_dagger"), List.of(
					lore("ritual_dagger.1")
			))
			,Map.entry(id("champions_gauntlet"), List.of(
					lore("champions_gauntlet.1")
			))
			,Map.entry(id("rusty_gauntlet"), List.of(
					lore("rusty_gauntlet.1")
			))
			,Map.entry(id("rusty_armlet"), List.of(
					lore("rusty_armlet.1")
			))
			,Map.entry(id("pillagers_coin"), List.of(
					lore("pillagers_coin.1")
			))
			,Map.entry(id("void_sphere"), List.of(
					lore("void_sphere.1")
			))
			,Map.entry(id("echo_sphere"), List.of(
					lore("echo_sphere.1")
			))
			,Map.entry(id("fire_sphere"), List.of(
					lore("fire_sphere.1")
			))
			,Map.entry(id("venom_sphere"), List.of(
					lore("venom_sphere.1")
			))
			,Map.entry(id("golden_honey_comb"), List.of(
					lore("golden_honey_comb.1")
			))
			,Map.entry(id("necronomicon"), List.of(
					lore("necronomicon.1")
			))
			,Map.entry(id("seal_of_alliance"), List.of(
					lore("seal_of_alliance.1")
			))
			,Map.entry(id("striker_of_the_morning_star"), List.of(
					lore("striker_of_the_morning_star.1")
			))
			,Map.entry(id("empty_chalice"), List.of(
					lore("empty_chalice.1")
			))
			
		
	);

	private static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, path);
	}

	private static Component lore(String key) {
		return Component.translatable("tooltip." + TimothatysTrinketsMod.MODID + "." + key)
				.withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC);
	}

	@SubscribeEvent
	public static void onItemTooltip(ItemTooltipEvent event) {
		if (event.getEntity() == null)
			return;

		ItemStack stack = event.getItemStack();
		if (stack.isEmpty())
			return;

		TimothatysTrinketsSpecialTooltipLines.addFor(stack, event.getToolTip(), event.getEntity());

		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		List<Component> lines = SHIFT_TEXT.get(itemId);
		if (lines == null)
			return;

		List<Component> tooltip = event.getToolTip();

		tooltip.add(SEPARATOR);
		if (Screen.hasShiftDown()) {
			tooltip.addAll(lines);
		} else {
			tooltip.add(HOLD_SHIFT);
		}
	}
}
