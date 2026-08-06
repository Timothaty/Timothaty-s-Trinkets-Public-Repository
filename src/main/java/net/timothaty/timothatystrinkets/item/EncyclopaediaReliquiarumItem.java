package net.timothaty.timothatystrinkets.item;

import net.timothaty.timothatystrinkets.client.EncyclopaediaReliquiarumClient;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EncyclopaediaReliquiarumItem extends Item {
	public EncyclopaediaReliquiarumItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (level.isClientSide && FMLEnvironment.dist == Dist.CLIENT) {
			EncyclopaediaReliquiarumClient.open();
		} else {
			level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.85F, 0.95F + level.getRandom().nextFloat() * 0.1F);
		}

		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}
}
