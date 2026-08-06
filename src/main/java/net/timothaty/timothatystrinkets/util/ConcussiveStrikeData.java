package net.timothaty.timothatystrinkets.util;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

public final class ConcussiveStrikeData {
	public static final ResourceKey<Enchantment> ENCHANTMENT = ResourceKey.create(
			Registries.ENCHANTMENT,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "concussive_strike")
	);

	public static final TagKey<Item> COMPAT_ITEMS = ItemTags.create(
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "constr_compat")
	);

	public static final float LISTED_CHANCE = 0.14F;
	public static final float MOB_LISTED_CHANCE = 0.07F;
	public static final float FULL_ATTACK_STRENGTH = 0.9F;

	public static final int BASE_STUN_TICKS = 14;
	public static final int STUN_TICKS_PER_LEVEL = 6;
	public static final int REQUIRED_SWEEPING_EDGE_LEVEL = 2;

	public static final String NBT_LAST_DIRECT_TARGET_ID = "tt_concussive_strike_last_target_id";
	public static final String NBT_LAST_DIRECT_ATTACK_TICK = "tt_concussive_strike_last_attack_tick";
	public static final String NBT_LAST_ATTACK_FULLY_CHARGED = "tt_concussive_strike_last_attack_charged";
	public static final String NBT_PROC_ROLL_TICK = "tt_concussive_strike_roll_tick";
	public static final String NBT_PROC_ROLL_DIRECT_TARGET_ID = "tt_concussive_strike_roll_target_id";
	public static final String NBT_PROC_ROLL_TARGET_ID = "tt_concussive_strike_roll_affected_target_id";
	public static final String NBT_PROC_ROLL_SUCCEEDED = "tt_concussive_strike_roll_succeeded";
	public static final String NBT_SOUND_TICK = "tt_concussive_strike_sound_tick";
	public static final String NBT_SOUND_DIRECT_TARGET_ID = "tt_concussive_strike_sound_target_id";
	public static final String NBT_SHAKE_TICK = "tt_concussive_strike_shake_tick";
	public static final String NBT_SHAKE_DIRECT_TARGET_ID = "tt_concussive_strike_shake_target_id";

	private ConcussiveStrikeData() {
	}

	public static int getStunTicks(int enchantmentLevel) {
		return BASE_STUN_TICKS + Math.max(0, enchantmentLevel - 1) * STUN_TICKS_PER_LEVEL;
	}

	public static int getLevel(ItemStack weapon, Level level) {
		if (weapon == null || weapon.isEmpty() || level == null)
			return 0;

		try {
			Holder<Enchantment> holder = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(ENCHANTMENT);
			return weapon.getEnchantmentLevel(holder);
		} catch (Exception ignored) {
			return 0;
		}
	}

	public static boolean has(ItemStack weapon, Level level) {
		return getLevel(weapon, level) > 0;
	}
}
