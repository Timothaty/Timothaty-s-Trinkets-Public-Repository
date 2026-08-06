package net.timothaty.timothatystrinkets.mechanics.natures_barrier;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class NaturesBarrierSounds {
	private NaturesBarrierSounds() {
	}

	public static SoundEvent hurtSound() {
		return SoundEvents.SHIELD_BLOCK;
	}

	public static void playActivation(LivingEntity entity) {
		playAtEntity(entity, SoundEvents.ENCHANTMENT_TABLE_USE, 0.82F, 1.25F);
		playAtEntity(entity, SoundEvents.AMETHYST_BLOCK_CHIME, 0.72F, 1.45F);
	}

	public static void playEnd(LivingEntity entity) {
		playAtEntity(entity, SoundEvents.SHIELD_BREAK, 0.68F, 1.35F);
	}

	private static void playAtEntity(LivingEntity entity, SoundEvent sound, float volume, float pitch) {
		if (entity == null || sound == null)
			return;

		Level level = entity.level();
		if (!(level instanceof ServerLevel server))
			return;

		SoundSource source = entity instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE;
		server.playSound(null,
				entity.getX(),
				entity.getY() + entity.getBbHeight() * 0.5D,
				entity.getZ(),
				sound,
				source,
				volume,
				pitch);
	}
}
