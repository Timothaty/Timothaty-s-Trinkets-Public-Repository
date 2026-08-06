package net.timothaty.timothatystrinkets.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class DuelistsGauntletItem extends Item {
	private static final int DURABILITY = 875;
	private static final int DURABILITY_BAR_COLOR = 0xFF8E26;

	public DuelistsGauntletItem() {
		super(new Item.Properties().durability(DURABILITY));
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return stack.isDamaged();
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return DURABILITY_BAR_COLOR;
	}
}
