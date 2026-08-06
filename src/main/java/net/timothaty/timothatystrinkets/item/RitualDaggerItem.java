package net.timothaty.timothatystrinkets.item;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;

public class RitualDaggerItem extends Item {
	private static final int DURABILITY_BAR_COLOR = 0x11d111;

	public RitualDaggerItem() {
		super(new Item.Properties().durability(163));
	}

	@Override
	public boolean hasCraftingRemainingItem(ItemStack stack) {
		return true;
	}

	@Override
	public ItemStack getCraftingRemainingItem(ItemStack itemstack) {
		ItemStack retval = new ItemStack(this);
		retval.setDamageValue(itemstack.getDamageValue() + 1);
		if (retval.getDamageValue() >= retval.getMaxDamage()) {
			return ItemStack.EMPTY;
		}
		return retval;
	}

	@Override
	public boolean isRepairable(ItemStack itemstack) {
		return false;
	}

	@Override
	public int getEnchantmentValue() {
		return 15;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return stack.isDamaged();
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return DURABILITY_BAR_COLOR;
	}

	@Override
	public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
		return level.getBlockState(pos).is(TimothatysTrinketsModBlocks.DAMNATION_ALTAR.get())
				|| super.doesSneakBypassUse(stack, level, pos, player);
	}
}
