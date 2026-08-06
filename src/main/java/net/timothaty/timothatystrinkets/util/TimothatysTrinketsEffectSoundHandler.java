package net.timothaty.timothatystrinkets.util;


import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class TimothatysTrinketsEffectSoundHandler {
	private TimothatysTrinketsEffectSoundHandler() {
	}

	private static final float FIRE_STACKING_VOLUME = 0.85F;
	private static final float MOLTEN_BANE_MARK_VOLUME = 1.0F;
	private static final float CORROSIVE_TOXICITY_STACKING_VOLUME = 0.72F;
	private static final double ARMORED_TOXICITY_THRESHOLD = 0.5D;

	public static void playMoltenBaneStackSound(Entity target) {
		playAtEntity(target,
				TimothatysTrinketsModSounds.FIRE_STACKING.get(),
				FIRE_STACKING_VOLUME,
				randomPitch(target, 0.92F, 1.08F)
		);
	}

	public static void playMoltenBaneMarkSound(Entity target) {
		playAtEntity(target,
				TimothatysTrinketsModSounds.MOLTEN_BANE_MARK.get(),
				MOLTEN_BANE_MARK_VOLUME,
				1.0F
		);
	}

	public static void playCorrosiveToxicityStackSound(Entity target) {
		SoundEvent sound = shouldUseArmoredToxicitySound(target)
				? TimothatysTrinketsModSounds.ARMORED_TOXICITY_HIT.get()
				: TimothatysTrinketsModSounds.NON_ARMORED_TOXICITY_HIT.get();

		playAtEntity(target,
				sound,
				CORROSIVE_TOXICITY_STACKING_VOLUME,
				randomPitch(target, 1.0F, 1.2F)
		);
	}

	private static boolean shouldUseArmoredToxicitySound(Entity target) {
		if (!(target instanceof LivingEntity livingEntity))
			return false;
		return livingEntity.getAttributeValue(Attributes.ARMOR) >= ARMORED_TOXICITY_THRESHOLD;
	}

	private static void playAtEntity(Entity entity, SoundEvent sound, float volume, float pitch) {
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
				pitch
		);
	}

	private static float randomPitch(Entity entity, float min, float max) {
		if (entity == null)
			return (min + max) * 0.5F;
		return min + entity.getRandom().nextFloat() * (max - min);
	}
}
