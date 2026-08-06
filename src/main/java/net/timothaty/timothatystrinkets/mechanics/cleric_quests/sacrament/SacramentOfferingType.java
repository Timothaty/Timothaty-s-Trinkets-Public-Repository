package net.timothaty.timothatystrinkets.mechanics.cleric_quests.sacrament;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

public enum SacramentOfferingType {
	WOODEN_BEAD(1 << 0),
	BREAD(1 << 1),
	DRINK(1 << 2);

	public static final int ALL_MASK = WOODEN_BEAD.bit | BREAD.bit | DRINK.bit;
	public static final TagKey<Item> OFFERING_DRINKS = TagKey.create(
		Registries.ITEM,
		ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "sacrament_offering_drinks")
	);

	private final int bit;

	SacramentOfferingType(int bit) {
		this.bit = bit;
	}

	public int bit() {
		return bit;
	}

	public static SacramentOfferingType fromStack(ItemStack stack) {
		if (stack.is(TimothatysTrinketsModItems.WOODEN_BEAD.get()))
			return WOODEN_BEAD;
		if (stack.is(Items.BREAD))
			return BREAD;
		if (stack.is(OFFERING_DRINKS))
			return DRINK;
		if (!stack.is(Items.POTION))
			return null;
		PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
		if (contents == null || !contents.customEffects().isEmpty() || contents.customColor().isPresent())
			return null;
		return contents.is(Potions.HEALING)
				|| contents.is(Potions.STRONG_HEALING)
				|| contents.is(Potions.REGENERATION)
				|| contents.is(Potions.LONG_REGENERATION)
				|| contents.is(Potions.STRONG_REGENERATION)
			? DRINK
			: null;
	}
}
