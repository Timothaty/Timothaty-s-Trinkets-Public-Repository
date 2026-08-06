package net.timothaty.timothatystrinkets.item;

import org.joml.Vector3f;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.mechanics.pact.PactOfAllianceHelper;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PactOfAllianceItem extends Item {
	private static final String FULL_KEY = "action_bar." + TimothatysTrinketsMod.MODID + ".pact_of_alliance.full";
	private static final String CONNECTION_KEY = "action_bar." + TimothatysTrinketsMod.MODID + ".pact_of_alliance.connection";
	private static final String DELETED_KEY = "action_bar." + TimothatysTrinketsMod.MODID + ".pact_of_alliance.deleted";
	private static final String NO_INK_KEY = "action_bar." + TimothatysTrinketsMod.MODID + ".pact_of_alliance.no_ink";

	private static final DustParticleOptions CONNECTION_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.82F, 0.18F), 1.05F);
	private static final DustParticleOptions DELETE_DUST = new DustParticleOptions(new Vector3f(0.87F, 0.24F, 0.07F), 1.05F);

	public PactOfAllianceItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (!(target instanceof Player targetPlayer) || targetPlayer == player)
			return InteractionResult.PASS;

		Level level = player.level();
		if (level.isClientSide())
			return InteractionResult.sidedSuccess(true);

		if (player.isShiftKeyDown()) {
			return removeFromPact(stack, player, targetPlayer, level);
		}
		return addToPact(stack, player, targetPlayer, level);
	}

	private static InteractionResult addToPact(ItemStack stack, Player player, Player target, Level level) {
		if (PactOfAllianceHelper.containsMember(stack, target.getUUID())) {
			PactOfAllianceHelper.addOrRefreshMember(stack, target);
			player.getInventory().setChanged();
			return InteractionResult.SUCCESS;
		}

		if (PactOfAllianceHelper.getMemberCount(stack) >= PactOfAllianceHelper.MAX_MEMBERS) {
			sendActionBar(player, Component.translatable(FULL_KEY, PactOfAllianceHelper.MAX_MEMBERS, PactOfAllianceHelper.MAX_MEMBERS));
			level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.55F, 1.55F);
			return InteractionResult.SUCCESS;
		}

		if (!consumeHolyInk(player)) {
			sendActionBar(player, Component.translatable(NO_INK_KEY));
			level.playSound(null, player.blockPosition(), SoundEvents.INK_SAC_USE, SoundSource.PLAYERS, 0.55F, 0.75F);
			return InteractionResult.SUCCESS;
		}

		PactOfAllianceHelper.AddMemberResult result = PactOfAllianceHelper.addOrRefreshMember(stack, target);
		if (result == PactOfAllianceHelper.AddMemberResult.FULL) {
			sendActionBar(player, Component.translatable(FULL_KEY, PactOfAllianceHelper.MAX_MEMBERS, PactOfAllianceHelper.MAX_MEMBERS));
			return InteractionResult.SUCCESS;
		}
		if (result == PactOfAllianceHelper.AddMemberResult.INVALID)
			return InteractionResult.FAIL;

		player.getInventory().setChanged();
		playConnectionFeedback(level, player, target);
		sendActionBar(player, Component.translatable(CONNECTION_KEY));
		return InteractionResult.SUCCESS;
	}

	private static InteractionResult removeFromPact(ItemStack stack, Player player, Player target, Level level) {
		if (!PactOfAllianceHelper.removeMember(stack, target.getUUID()))
			return InteractionResult.SUCCESS;

		player.getInventory().setChanged();
		if (level instanceof ServerLevel serverLevel) {
			spawnDust(serverLevel, target, DELETE_DUST, 36);
		}
		level.playSound(null, target.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.75F, 1.25F);
		sendActionBar(player, Component.translatable(DELETED_KEY));
		return InteractionResult.SUCCESS;
	}

	private static boolean consumeHolyInk(Player player) {
		if (player.getAbilities().instabuild)
			return true;

		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack candidate = inventory.getItem(slot);
			if (candidate.is(TimothatysTrinketsModItems.HOLY_INK.get())) {
				candidate.shrink(1);
				inventory.setChanged();
				return true;
			}
		}
		return false;
	}

	private static void playConnectionFeedback(Level level, Player player, Player target) {
		if (level instanceof ServerLevel serverLevel) {
			spawnDust(serverLevel, player, CONNECTION_DUST, 30);
			spawnDust(serverLevel, target, CONNECTION_DUST, 30);
		}
		level.playSound(null, target.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.85F, 1.25F);
	}

	private static void spawnDust(ServerLevel serverLevel, LivingEntity entity, DustParticleOptions dust, int count) {
		double y = entity.getY() + entity.getBbHeight() * 0.55D;
		double horizontalSpread = Math.max(0.25D, entity.getBbWidth() * 0.42D);
		double verticalSpread = Math.max(0.25D, entity.getBbHeight() * 0.36D);
		serverLevel.sendParticles(dust, entity.getX(), y, entity.getZ(), count, horizontalSpread, verticalSpread, horizontalSpread, 0.025D);
	}

	private static void sendActionBar(Player player, Component message) {
		player.displayClientMessage(message, true);
	}
}
