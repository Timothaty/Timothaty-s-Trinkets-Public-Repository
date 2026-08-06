package net.timothaty.timothatystrinkets.item;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.network.RefreshingChaliceVfxMessage;
import net.timothaty.timothatystrinkets.util.TimothatysCuriosHelper;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class RefreshingChaliceItem extends Item {
	private static final int USE_DURATION = 32;
	private static final int SELF_COOLDOWN_TICKS = 20 * 25;
	private static final double VFX_TRACKING_RANGE_SQR = 64.0D * 64.0D;

	public RefreshingChaliceItem() {
		super(new Item.Properties().durability(2).rarity(Rarity.RARE));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!player.isCreative() && player.getCooldowns().isOnCooldown(this)) {
			return InteractionResultHolder.fail(stack);
		}

		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.DRINK;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return USE_DURATION;
	}

	@Override
	public SoundEvent getDrinkingSound() {
		return TimothatysTrinketsModSounds.BOTTLE_OF_BLOOD_DRINK.get();
	}

	@Override
	public SoundEvent getEatingSound() {
		return TimothatysTrinketsModSounds.BOTTLE_OF_BLOOD_DRINK.get();
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!(entity instanceof Player player)) {
			return stack;
		}

		if (!level.isClientSide()) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(), TimothatysTrinketsModSounds.REFRESHING_CHALICE_USE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
			resetInventoryCooldowns(player);
			TimothatysCuriosHelper.removeCooldownsForEquippedItems(player);
			if (!player.isCreative()) {
				player.getCooldowns().addCooldown(this, SELF_COOLDOWN_TICKS);
			}
			sendRefreshingChaliceVfx(player);
		}

		return player.isCreative() ? stack : new ItemStack(TimothatysTrinketsModItems.EMPTY_CHALICE.get());
	}

	private static void resetInventoryCooldowns(Player player) {
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack inventoryStack = inventory.getItem(slot);
			if (!inventoryStack.isEmpty()) {
				player.getCooldowns().removeCooldown(inventoryStack.getItem());
			}
		}
	}

	private static void sendRefreshingChaliceVfx(Player player) {
		if (!(player.level() instanceof ServerLevel serverLevel))
			return;

		RefreshingChaliceVfxMessage message = new RefreshingChaliceVfxMessage(player.getId());
		for (ServerPlayer recipient : serverLevel.players()) {
			if (recipient.distanceToSqr(player) <= VFX_TRACKING_RANGE_SQR) {
				PacketDistributor.sendToPlayer(recipient, message);
			}
		}
	}
}
