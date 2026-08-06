package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.DebtlordEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;

import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.Tags;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class DebtlordDisarmAbility {
	public static final float SELECTION_CHANCE = 0.15F;
	public static final int CAST_DURATION_TICKS = 20;
	public static final int SWING_SOUND_TICK = 6;
	public static final int IMPACT_TICK = 8;
	public static final int COOLDOWN_TICKS = 15 * 20;

	private DebtlordDisarmAbility() {
	}

	public static void playSwingSound(DebtlordEntity debtlord) {
		debtlord.level().playSound(null, debtlord.blockPosition(), TimothatysTrinketsModSounds.HEAVY_SWING.get(), SoundSource.HOSTILE, 1.35F, 1.0F);
	}

	public static void performImpact(DebtlordEntity debtlord, LivingEntity target) {
		if (!(target instanceof Player player) || !canDisarmTarget(player) || DebtlordHornsGoal.getValidImpactDirection(debtlord, target) == null || isShieldBlocking(player))
			return;

		ItemStack heldItem = player.getMainHandItem();
		if (heldItem.isEmpty())
			return;

		ItemStack droppedStack = heldItem.copy();
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		ItemEntity droppedEntity = player.drop(droppedStack, false, false);
		if (droppedEntity == null) {
			player.setItemInHand(InteractionHand.MAIN_HAND, droppedStack);
			return;
		}
		droppedEntity.setGlowingTag(true);

		if (debtlord.level() instanceof ServerLevel serverLevel) {
			serverLevel.playSound(null, player.blockPosition(), TimothatysTrinketsModSounds.DEBTLORD_DISARM.get(), SoundSource.HOSTILE, 1.45F, 1.0F);
		}
	}

	public static boolean canDisarmTarget(LivingEntity target) {
		if (!(target instanceof Player player))
			return false;

		ItemStack heldItem = player.getMainHandItem();
		return heldItem.is(Tags.Items.MELEE_WEAPON_TOOLS)
			|| heldItem.is(Tags.Items.RANGED_WEAPON_TOOLS)
			|| heldItem.is(Tags.Items.MINING_TOOL_TOOLS);
	}

	private static boolean isShieldBlocking(Player player) {
		return player.isBlocking()
			&& !player.getUseItem().isEmpty()
			&& player.getUseItem().canPerformAction(ItemAbilities.SHIELD_BLOCK);
	}
}
