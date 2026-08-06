package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

public enum CorruptedRosariumBead {
	SIN("bead_of_sin", 1 << 0),
	BLASPHEMY("bead_of_blasphemy", 1 << 1),
	WRATH("bead_of_wrath", 1 << 2),
	PRIDE("bead_of_pride", 1 << 3);

	private static final CorruptedRosariumBead[] CACHED_VALUES = values();

	private final ResourceLocation itemId;
	private final String serializedItemId;
	private final int bit;

	CorruptedRosariumBead(String itemPath, int bit) {
		this.itemId = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, itemPath);
		this.serializedItemId = this.itemId.toString();
		this.bit = bit;
	}

	public ResourceLocation itemId() {
		return itemId;
	}

	public int bit() {
		return bit;
	}

	public ItemStack createStack() {
		Item item = BuiltInRegistries.ITEM.get(itemId);
		return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
	}

	public static Optional<CorruptedRosariumBead> byItemId(ResourceLocation itemId) {
		return Optional.ofNullable(byItemIdOrNull(itemId));
	}

	public static Optional<CorruptedRosariumBead> byStack(ItemStack stack) {
		if (stack.isEmpty())
			return Optional.empty();
		return byItemId(BuiltInRegistries.ITEM.getKey(stack.getItem()));
	}

	static CorruptedRosariumBead byItemIdOrNull(ResourceLocation itemId) {
		if (itemId == null)
			return null;

		for (CorruptedRosariumBead bead : CACHED_VALUES) {
			if (bead.itemId.equals(itemId))
				return bead;
		}
		return null;
	}

	static CorruptedRosariumBead bySerializedItemIdOrNull(String itemId) {
		if (itemId == null || itemId.isEmpty())
			return null;

		for (CorruptedRosariumBead bead : CACHED_VALUES) {
			if (bead.serializedItemId.equals(itemId))
				return bead;
		}
		return null;
	}
}
