package net.timothaty.timothatystrinkets.util;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public final class DrumsOfHasteVfx {
	private DrumsOfHasteVfx() {
	}

	public static void spawnByStacks(ServerLevel level, Player player, int stacks, long nowTick) {
		spawnFlameByStacks(level, player, stacks, nowTick);
		spawnDrumBeatByStacks(level, player, stacks, nowTick);
	}

	public static void spawnFlameByStacks(ServerLevel level, Player player, int stacks, long nowTick) {
		if (stacks <= 0) {
			player.getPersistentData().putLong(DrumsOfHasteData.NBT_NEXT_FLAME_TICK, 0L);
			return;
		}

		int visualStacks = Math.min(stacks, DrumsOfHasteData.BURST_STACK_CAP);
		int interval = Mth.clamp(19 - visualStacks, 7, 20);

		long next = player.getPersistentData().getLong(DrumsOfHasteData.NBT_NEXT_FLAME_TICK);
		if (next <= 0L) {
			player.getPersistentData().putLong(DrumsOfHasteData.NBT_NEXT_FLAME_TICK, nowTick + interval);
			return;
		}
		if (nowTick < next)
			return;

		int count = 1 + (visualStacks / 3);
		if (player.getRandom().nextFloat() < 0.35F)
			count++;

		double baseX = player.getX();
		double baseY = player.getY();
		double baseZ = player.getZ();

		for (int i = 0; i < count; i++) {
			double ox = (player.getRandom().nextDouble() - 0.5D) * 0.9D;
			double oz = (player.getRandom().nextDouble() - 0.5D) * 0.9D;
			double oy = 0.2D + (player.getRandom().nextDouble() * 1.4D);

			level.sendParticles(ParticleTypes.FLAME, baseX + ox, baseY + oy, baseZ + oz, 1, 0.0D, 0.02D, 0.0D, 0.0D);
		}

		player.getPersistentData().putLong(DrumsOfHasteData.NBT_NEXT_FLAME_TICK, nowTick + interval);
	}

	public static void spawnDrumBeatByStacks(ServerLevel level, Player player, int stacks, long nowTick) {
		if (stacks <= 0) {
			player.getPersistentData().putLong(DrumsOfHasteData.NBT_NEXT_DRUMBEAT_TICK, 0L);
			return;
		}

		int visualStacks = Math.min(stacks, DrumsOfHasteData.BURST_STACK_CAP);
		int interval = Mth.clamp(34 - visualStacks, 10, 34);

		long next = player.getPersistentData().getLong(DrumsOfHasteData.NBT_NEXT_DRUMBEAT_TICK);
		if (next <= 0L) {
			player.getPersistentData().putLong(DrumsOfHasteData.NBT_NEXT_DRUMBEAT_TICK, nowTick + interval);
			return;
		}
		if (nowTick < next)
			return;

		spawnDrumBeatOnce(level, player, visualStacks);
		player.getPersistentData().putLong(DrumsOfHasteData.NBT_NEXT_DRUMBEAT_TICK, nowTick + interval);
	}

	public static void spawnDrumBeatOnce(ServerLevel level, Player player, int stacks) {
		int visualStacks = Math.min(DrumsOfHasteData.clampStacks(stacks), DrumsOfHasteData.BURST_STACK_CAP);

		double x = player.getX();
		double y = player.getY() + 0.08D;
		double z = player.getZ();

		level.sendParticles(
				TimothatysTrinketsModParticleTypes.DRUM_BEAT.get(),
				x, y, z,
				0,
				(double) player.getId(), (double) visualStacks, 0.0D,
				1.0D
		);

		level.playSound(null, x, y, z, TimothatysTrinketsModSounds.DRUM_BEAT_WAVE_SOUND.get(), SoundSource.PLAYERS, 0.34F, 0.96F);
	}
}
