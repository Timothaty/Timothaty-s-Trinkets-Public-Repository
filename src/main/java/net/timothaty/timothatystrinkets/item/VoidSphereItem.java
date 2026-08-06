package net.timothaty.timothatystrinkets.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class VoidSphereItem extends Item {
	public VoidSphereItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return 0xB84DFF;
	}
}
