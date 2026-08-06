package net.timothaty.timothatystrinkets.util;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class TimothatysTrinketsControlSounds {
	private static final long STUNNED_LOOP_INTERVAL_TICKS = 18L;
	private static final float STUNNED_LOOP_VOLUME = 0.16F;
	private static final float STUNNED_LOOP_PITCH = 1.0F;

	private TimothatysTrinketsControlSounds() {
	}

	public static void playStunnedLoopIfNeeded(LivingEntity living, CompoundTag data) {
		if (!(living.level() instanceof ServerLevel server))
			return;

		long now = server.getGameTime();
		long nextLoopTick = data.getLong(TimothatysTrinketsControlData.NBT_STUNNED_NEXT_LOOP_TICK);
		if (now < nextLoopTick)
			return;

		SoundSource source = living instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE;
		server.playSound(null,
				living.getX(),
				living.getY() + living.getBbHeight() * 0.5D,
				living.getZ(),
				TimothatysTrinketsModSounds.STUNNED_LOOP.get(),
				source,
				STUNNED_LOOP_VOLUME,
				STUNNED_LOOP_PITCH
		);

		data.putLong(TimothatysTrinketsControlData.NBT_STUNNED_NEXT_LOOP_TICK, now + STUNNED_LOOP_INTERVAL_TICKS);
	}
}
