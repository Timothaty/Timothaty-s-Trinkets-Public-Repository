package net.timothaty.timothatystrinkets.util;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.resources.ResourceLocation;

public final class StrikerOfTheMorningStarData {
	public static final ResourceLocation STRIKER_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "striker_of_the_morning_star");

	public static final String NBT_SPRINT_START_TICK = "ttr_striker_sprint_start_tick";
	public static final String NBT_LAST_BASH_TICK = "ttr_striker_last_bash_tick";
	public static final String NBT_LAST_MACE_WAVE_TICK = "ttr_striker_last_mace_wave_tick";
	public static final String NBT_SHOCKWAVE_DAMAGE_GUARD = "ttr_striker_shockwave_damage_guard";

	public static final int MIN_SPRINT_TICKS = 20;
	public static final int SPRINT_BASH_COOLDOWN_TICKS = 30;
	public static final float SPRINT_BASH_CHANCE = 0.40F;
	public static final float CONCUSSIVE_NON_PLAYER_STUN_CHANCE_BONUS = 0.08F;
	public static final float CONCUSSIVE_PLAYER_STUN_CHANCE_BONUS = 0.045F;
	public static final int STUN_FATIGUE_TICKS = 20 * 2;
	public static final int STUN_FATIGUE_AMPLIFIER = 1;

	public static final float MACE_MIN_FALL_DISTANCE = 30.0F;
	public static final float MACE_SHOCKWAVE_FALL_DISTANCE = 50.0F;
	public static final double MACE_SHOCKWAVE_RADIUS_MULTIPLIER = 0.16D;
	public static final float MACE_SHOCKWAVE_DAMAGE_MULTIPLIER = 0.35F;
	public static final double MACE_SHOCKWAVE_VERTICAL_TOLERANCE = 1.25D;
	public static final int UNDEAD_KNIGHT_BLOCK_STUN_TICKS = 20 * 4;
	public static final int UNDEAD_KNIGHT_AXE_BLOCK_STUN_TICKS = 20 * 6;

	private StrikerOfTheMorningStarData() {
	}
}
