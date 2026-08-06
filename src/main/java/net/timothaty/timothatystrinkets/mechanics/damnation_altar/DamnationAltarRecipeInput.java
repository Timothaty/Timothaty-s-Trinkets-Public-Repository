package net.timothaty.timothatystrinkets.mechanics.damnation_altar;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public final class DamnationAltarRecipeInput implements RecipeInput {
	private final NonNullList<ItemStack> stacks;

	public DamnationAltarRecipeInput(NonNullList<ItemStack> stacks) {
		this.stacks = NonNullList.withSize(DamnationAltarSlot.values().length, ItemStack.EMPTY);
		for (DamnationAltarSlot slot : DamnationAltarSlot.values()) {
			this.stacks.set(slot.index(), stacks.get(slot.index()).copy());
		}
	}

	@Override
	public ItemStack getItem(int index) {
		return stacks.get(index);
	}

	public ItemStack getItem(DamnationAltarSlot slot) {
		return getItem(slot.index());
	}

	@Override
	public int size() {
		return stacks.size();
	}
}
