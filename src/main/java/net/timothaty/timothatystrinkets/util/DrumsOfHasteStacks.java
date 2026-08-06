package net.timothaty.timothatystrinkets.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class DrumsOfHasteStacks {
	private DrumsOfHasteStacks() {
	}

	public static int getStacks(Player player) {
		if (player == null)
			return 0;
		return DrumsOfHasteData.clampStacks(player.getPersistentData().getInt(DrumsOfHasteData.NBT_STACKS));
	}

	public static void setStacks(Player player, int stacks) {
		if (player == null)
			return;
		int clamped = DrumsOfHasteData.clampStacks(stacks);
		player.getPersistentData().putInt(DrumsOfHasteData.NBT_STACKS, clamped);
		syncEquippedStack(player, clamped);
	}

	public static void syncEquippedStack(Player player, int stacks) {
		ItemStack stack = DrumsOfHasteCurios.getEquippedDrumsStack(player);
		setStackFury(stack, stacks);
	}

	public static void resetTimers(Player player) {
		if (player == null)
			return;
		player.getPersistentData().putLong(DrumsOfHasteData.NBT_NEXT_DECAY_TICK, 0L);
		player.getPersistentData().putLong(DrumsOfHasteData.NBT_LAST_DAMAGE_TICK, 0L);
		player.getPersistentData().putLong(DrumsOfHasteData.NBT_NEXT_FLAME_TICK, 0L);
		player.getPersistentData().putLong(DrumsOfHasteData.NBT_NEXT_DRUMBEAT_TICK, 0L);
	}

	public static int decayIfNeeded(Player player, long nowTick) {
		int stacks = getStacks(player);
		if (stacks <= 0) {
			player.getPersistentData().putLong(DrumsOfHasteData.NBT_NEXT_DECAY_TICK, 0L);
			return 0;
		}

		long lastDamageTick = player.getPersistentData().getLong(DrumsOfHasteData.NBT_LAST_DAMAGE_TICK);
		if (lastDamageTick <= 0L) {
			player.getPersistentData().putLong(DrumsOfHasteData.NBT_LAST_DAMAGE_TICK, nowTick);
			return stacks;
		}

		long sinceDamage = nowTick - lastDamageTick;
		if (sinceDamage < DrumsOfHasteData.NO_DAMAGE_GRACE_TICKS) {
			player.getPersistentData().putLong(DrumsOfHasteData.NBT_NEXT_DECAY_TICK, 0L);
			return stacks;
		}

		long nextDecay = player.getPersistentData().getLong(DrumsOfHasteData.NBT_NEXT_DECAY_TICK);
		if (nextDecay <= 0L) {
			player.getPersistentData().putLong(DrumsOfHasteData.NBT_NEXT_DECAY_TICK, nowTick + DrumsOfHasteData.DECAY_INTERVAL_TICKS);
			return stacks;
		}

		if (nowTick >= nextDecay) {
			stacks = DrumsOfHasteData.clampStacks(stacks - 1);
			setStacks(player, stacks);
			player.getPersistentData().putLong(DrumsOfHasteData.NBT_NEXT_DECAY_TICK, nowTick + DrumsOfHasteData.DECAY_INTERVAL_TICKS);
		}
		return stacks;
	}

	public static int getStackFury(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return 0;
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		return DrumsOfHasteData.clampStacks(tag.getInt(DrumsOfHasteData.NBT_ITEM_FURY));
	}

	public static void setStackFury(ItemStack stack, int stacks) {
		if (stack == null || stack.isEmpty())
			return;

		int clamped = DrumsOfHasteData.clampStacks(stacks);
		if (clamped <= 0) {
			clearStackFury(stack);
			return;
		}

		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		tag.putInt(DrumsOfHasteData.NBT_ITEM_FURY, clamped);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}

	public static void clearStackFury(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return;

		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		if (!tag.contains(DrumsOfHasteData.NBT_ITEM_FURY))
			return;

		tag.remove(DrumsOfHasteData.NBT_ITEM_FURY);
		if (tag.isEmpty()) {
			stack.remove(DataComponents.CUSTOM_DATA);
		} else {
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		}
	}
}
