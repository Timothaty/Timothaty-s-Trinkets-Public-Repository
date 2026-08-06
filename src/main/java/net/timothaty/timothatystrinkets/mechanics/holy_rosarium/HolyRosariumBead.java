package net.timothaty.timothatystrinkets.mechanics.holy_rosarium;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

public enum HolyRosariumBead {
	HUMILITY("bead_of_humility", 1 << 0),
	PENANCE("bead_of_repentance", 1 << 1),
	RESURRECTION("bead_of_resurrection", 1 << 3),
	SACRAMENT("bead_of_the_sacrament", 1 << 2),
	SAINT("bead_of_the_saint", 1 << 4);

	private static final HolyRosariumBead[] CACHED_VALUES = values();

	private final ResourceLocation itemId;
	private final String serializedItemId;
	private final int bit;

	HolyRosariumBead(String itemPath, int bit) {
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

	public static Optional<HolyRosariumBead> byItemId(ResourceLocation itemId) {
		return Optional.ofNullable(byItemIdOrNull(itemId));
	}

	public static Optional<HolyRosariumBead> byStack(ItemStack stack) {
		if (stack.isEmpty())
			return Optional.empty();
		return byItemId(BuiltInRegistries.ITEM.getKey(stack.getItem()));
	}

	static HolyRosariumBead byItemIdOrNull(ResourceLocation itemId) {
		if (itemId == null)
			return null;

		for (HolyRosariumBead bead : CACHED_VALUES) {
			if (bead.itemId.equals(itemId))
				return bead;
		}
		return null;
	}

	static HolyRosariumBead bySerializedItemIdOrNull(String itemId) {
		if (itemId == null || itemId.isEmpty())
			return null;

		for (HolyRosariumBead bead : CACHED_VALUES) {
			if (bead.serializedItemId.equals(itemId))
				return bead;
		}
		return null;
	}
}
