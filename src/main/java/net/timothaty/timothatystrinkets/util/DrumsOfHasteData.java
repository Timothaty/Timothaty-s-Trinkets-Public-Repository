package net.timothaty.timothatystrinkets.util;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.resources.ResourceLocation;

public final class DrumsOfHasteData {
	public static final ResourceLocation DRUMS_ID =
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "drums_of_haste");

	public static final int MAX_STACKS = 15;
	public static final int BURST_STACK_CAP = 12;

	public static final String NBT_LAST_STACK_SOUND_TICK = "ttr_drums_of_haste_last_stack_sound_tick";
	public static final String NBT_STACKS = "ttr_drums_of_haste_stacks";
	public static final String NBT_EQUIP_EXPIRE_TICK = "ttr_drums_of_haste_equip_expire";
	public static final String NBT_LAST_DAMAGE_TICK = "ttr_drums_of_haste_last_damage_tick";
	public static final String NBT_NEXT_DECAY_TICK = "ttr_drums_of_haste_next_decay_tick";
	public static final String NBT_NEXT_FLAME_TICK = "ttr_drums_of_haste_next_flame_tick";
	public static final String NBT_NEXT_DRUMBEAT_TICK = "ttr_drums_of_haste_next_drumbeat_tick";
	public static final String NBT_ITEM_FURY = "ttr_drums_of_haste_fury";

	public static final long EQUIP_GRACE_TICKS = 5L;
	public static final long NO_DAMAGE_GRACE_TICKS = 15L * 20L;
	public static final long DECAY_INTERVAL_TICKS = 3L * 20L;

	public static final ResourceLocation MOVE_SPEED_MOD_ID =
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "drums_of_haste_movespeed");
	public static final ResourceLocation ATTACK_SPEED_MOD_ID =
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "drums_of_haste_attackspeed");

	private DrumsOfHasteData() {
	}

	public static int clampStacks(int stacks) {
		if (stacks < 0)
			return 0;
		return Math.min(stacks, MAX_STACKS);
	}
}
