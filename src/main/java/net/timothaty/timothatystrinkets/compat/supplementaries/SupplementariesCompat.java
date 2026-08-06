package net.timothaty.timothatystrinkets.compat.supplementaries;

import net.neoforged.fml.ModList;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Optional, reflection-free Supplementaries integration. */
public final class SupplementariesCompat {
	private static final String MOD_ID = "supplementaries";
	private static final ResourceLocation ASH_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "ash");
	private static volatile boolean resolved;
	private static Item ashItem = Items.AIR;

	private SupplementariesCompat() {
	}

	public static ItemStack createAshDrop() {
		Item item = resolveAshItem();
		return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
	}

	private static Item resolveAshItem() {
		if (!resolved) {
			synchronized (SupplementariesCompat.class) {
				if (!resolved) {
					if (ModList.get().isLoaded(MOD_ID)) {
						ashItem = BuiltInRegistries.ITEM.getOptional(ASH_ID).filter(item -> item != Items.AIR).orElse(Items.AIR);
					}
					resolved = true;
				}
			}
		}
		return ashItem;
	}
}
