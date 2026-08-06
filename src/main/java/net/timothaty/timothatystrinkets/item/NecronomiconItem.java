package net.timothaty.timothatystrinkets.item;

import net.timothaty.timothatystrinkets.mechanics.necronomicon.NecronomiconUseHandler;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class NecronomiconItem extends Item {
	public NecronomiconItem() {
		super(new Item.Properties().durability(550));
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		return NecronomiconUseHandler.useOnEntity(player.level(), player, target, stack);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return 0xA52019;
	}
}
