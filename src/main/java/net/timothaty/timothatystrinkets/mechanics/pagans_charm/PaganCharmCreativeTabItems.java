package net.timothaty.timothatystrinkets.mechanics.pagans_charm;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class PaganCharmCreativeTabItems {
	private static final ResourceKey<CreativeModeTab> TIMOTHATYS_TRINKETS_TAB = ResourceKey.create(
			Registries.CREATIVE_MODE_TAB,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "timothatys_trinkets_tab")
	);

	private PaganCharmCreativeTabItems() {
	}

	@SubscribeEvent
	public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
		if (!TIMOTHATYS_TRINKETS_TAB.equals(event.getTabKey()))
			return;

		ItemStack defaultCharm = new ItemStack(TimothatysTrinketsModItems.PAGANS_CHARM.get());
		ItemStack chargedCharm = TimothatysTrinketsModItems.PAGANS_CHARM.get().getDefaultInstance();
		CreativeModeTab.TabVisibility visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;

		event.remove(defaultCharm, visibility);

		ItemStack anchor = new ItemStack(TimothatysTrinketsModItems.GOLDEN_HONEY_COMB.get());
		if (event.getParentEntries().contains(anchor) && event.getSearchEntries().contains(anchor)) {
			event.insertAfter(anchor, chargedCharm, visibility);
		} else {
			event.accept(chargedCharm, visibility);
		}
	}
}
