package net.timothaty.timothatystrinkets.mechanics.cleansing.ritual;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class CleansingRitualSounds {
	private static final float IGNITION_VOLUME = 0.85F;
	private static final float SMOLDER_VOLUME = 0.24F;
	private static final float ABORT_VOLUME = 0.55F;
	private static final float CONSECRATION_VOLUME = 0.42F;
	private static final float FINAL_VOLUME = 0.72F;
	private static final float MANIFESTATION_VOLUME = 0.48F;
	private static final float PICKUP_VOLUME = 0.65F;
	private static final float IGNITION_PITCH_MIN = 0.92F;
	private static final float IGNITION_PITCH_RANGE = 0.16F;
	private static final float SMOLDER_PITCH_MIN = 0.88F;
	private static final float SMOLDER_PITCH_RANGE = 0.22F;
	private static final float ABORT_PITCH = 1.15F;
	private static final float CONSECRATION_PITCH_BASE = 0.90F;
	private static final float CONSECRATION_PITCH_STEP = 0.08F;
	private static final float FINAL_MAGIC_PITCH = 1.18F;
	private static final float FINAL_CHIME_PITCH = 1.42F;
	private static final float MANIFESTATION_PITCH = 1.28F;
	private static final float PICKUP_PITCH = 1.25F;

	private CleansingRitualSounds() {
	}

	public static void ignition(Level level, BlockPos pos, Player player, boolean fireCharge) {
		SoundEvent sound = fireCharge ? SoundEvents.FIRECHARGE_USE : SoundEvents.FLINTANDSTEEL_USE;
		level.playSound(null, pos, sound, SoundSource.BLOCKS, IGNITION_VOLUME,
				IGNITION_PITCH_MIN + level.random.nextFloat() * IGNITION_PITCH_RANGE);
	}

	public static void smolder(Level level, BlockPos pos) {
		level.playSound(null, pos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, SMOLDER_VOLUME,
				SMOLDER_PITCH_MIN + level.random.nextFloat() * SMOLDER_PITCH_RANGE);
	}

	public static void abort(Level level, BlockPos pos) {
		level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, ABORT_VOLUME, ABORT_PITCH);
	}

	public static void consecrationPulse(Level level, BlockPos pos, int pulse) {
		SoundEvent sound = pulse == 0 ? SoundEvents.AMETHYST_BLOCK_RESONATE : SoundEvents.AMETHYST_BLOCK_CHIME;
		level.playSound(null, pos, sound, SoundSource.BLOCKS, CONSECRATION_VOLUME,
				CONSECRATION_PITCH_BASE + pulse * CONSECRATION_PITCH_STEP);
	}

	public static void finalFlash(Level level, BlockPos pos) {
		level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, FINAL_VOLUME, FINAL_MAGIC_PITCH);
		level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, FINAL_VOLUME * 0.85F, FINAL_CHIME_PITCH);
	}

	public static void manifestation(Level level, BlockPos pos) {
		level.playSound(null, pos, SoundEvents.TRIAL_SPAWNER_SPAWN_ITEM, SoundSource.BLOCKS,
				MANIFESTATION_VOLUME, MANIFESTATION_PITCH);
	}

	public static void pickup(Level level, BlockPos pos, Player player) {
		level.playSound(null, pos, SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.PLAYERS, PICKUP_VOLUME, PICKUP_PITCH);
	}
}
