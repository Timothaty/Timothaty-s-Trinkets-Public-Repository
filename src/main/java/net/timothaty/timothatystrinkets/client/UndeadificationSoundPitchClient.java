package net.timothaty.timothatystrinkets.client;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.util.UndeadificationEntityStateHelper;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

import java.util.List;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class UndeadificationSoundPitchClient {
	private static final TagKey<SoundEvent> PITCH_SHIFT_EXCLUDED_SOUNDS = TagKey.create(
			Registries.SOUND_EVENT,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "undeadification_pitch_shift_excluded")
	);
	private static final TagKey<SoundEvent> ENTITY_SOUNDS = TagKey.create(
			Registries.SOUND_EVENT,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "undeadification_pitch_shift_entity_sounds")
	);
	private static final String[] CREATURE_SOUND_TOKENS = {
			"ambient", "idle", "hurt", "death", "step", "breathe", "breath",
			"growl", "roar", "scream", "snort", "angry", "celebrate", "trade",
			"talk", "yes", "no"
	};

	private static final float TWO_SEMITONES_DOWN = 0.8908987F;
	private static final float MIN_PITCH = 0.5F;
	private static final float MAX_PITCH = 2.0F;
	private static final double POSITION_SOUND_SEARCH_RADIUS = 1.35D;

	private UndeadificationSoundPitchClient() {
	}

	@SubscribeEvent
	public static void onPlayEntitySound(PlayLevelSoundEvent.AtEntity event) {
		if (!isCreatureSound(event)) {
			return;
		}

		if (event.getEntity() instanceof LivingEntity livingEntity && hasUndeadification(livingEntity)) {
			lowerPitch(event);
		}
	}

	@SubscribeEvent
	public static void onPlayPositionSound(PlayLevelSoundEvent.AtPosition event) {
		if (!isCreatureSound(event)) {
			return;
		}

		Vec3 position = event.getPosition();
		Level level = event.getLevel();

		AABB searchBox = AABB.ofSize(position, POSITION_SOUND_SEARCH_RADIUS * 2.0D, POSITION_SOUND_SEARCH_RADIUS * 2.0D, POSITION_SOUND_SEARCH_RADIUS * 2.0D);
		List<LivingEntity> entities = level.getEntitiesOfClass(
				LivingEntity.class,
				searchBox,
				livingEntity -> livingEntity.isAlive() && hasUndeadification(livingEntity)
		);

		if (entities.isEmpty()) {
			return;
		}

		LivingEntity closest = null;
		double closestDistance = POSITION_SOUND_SEARCH_RADIUS * POSITION_SOUND_SEARCH_RADIUS;
		for (LivingEntity entity : entities) {
			double distance = entity.distanceToSqr(position);
			if (distance <= closestDistance) {
				closest = entity;
				closestDistance = distance;
			}
		}

		if (closest != null) {
			lowerPitch(event);
		}
	}

	private static boolean hasUndeadification(LivingEntity livingEntity) {
		return UndeadificationEntityStateHelper.hasUndeadificationVisualMarker(livingEntity);
	}

	private static boolean isCreatureSound(PlayLevelSoundEvent event) {
		Holder<SoundEvent> sound = event.getSound();
		if (sound == null || sound.is(PITCH_SHIFT_EXCLUDED_SOUNDS)) {
			return false;
		}

		if (sound.is(ENTITY_SOUNDS)) {
			return true;
		}

		SoundSource source = event.getSource();
		if (source != SoundSource.HOSTILE
				&& source != SoundSource.NEUTRAL
				&& source != SoundSource.PLAYERS) {
			return false;
		}

		String path = sound.value().getLocation().getPath();
		for (String token : CREATURE_SOUND_TOKENS) {
			if (containsWholeToken(path, token)) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsWholeToken(String path, String token) {
		int start = 0;
		while ((start = path.indexOf(token, start)) >= 0) {
			int end = start + token.length();
			boolean leftBoundary = start == 0 || !Character.isLetterOrDigit(path.charAt(start - 1));
			boolean rightBoundary = end == path.length() || !Character.isLetterOrDigit(path.charAt(end));
			if (leftBoundary && rightBoundary) {
				return true;
			}
			start = end;
		}
		return false;
	}

	private static void lowerPitch(PlayLevelSoundEvent event) {
		event.setNewPitch(Mth.clamp(event.getNewPitch() * TWO_SEMITONES_DOWN, MIN_PITCH, MAX_PITCH));
	}
}
