package net.timothaty.timothatystrinkets.client.animation;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.beatific_pallium.BeatificPalliumPlayerAnimation;
import net.timothaty.timothatystrinkets.client.cherubims_wisdom.CherubimsWisdomPlayerAnimation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class PlayerCastHandDustVisuals {
	private static final float MIN_SIZE = 0.55F;
	private static final float SIZE_RANGE = 0.30F;
	private static final double JITTER_RANGE = 0.030D;
	private static final double VELOCITY_Y = 0.004D;
	private static final Map<Integer, HandDustState> STATES = new HashMap<>();
	private static ClientLevel trackedLevel;

	private PlayerCastHandDustVisuals() {
	}

	public enum CastProfile {
		CHERUBIMS_WISDOM(0xFFF16A, 24),
		BEATIFIC_PALLIUM(0xFFE36A, 20);

		private final int dustRgb;
		private final int safetyTimeoutTicks;

		CastProfile(int dustRgb, int safetyTimeoutTicks) {
			this.dustRgb = dustRgb;
			this.safetyTimeoutTicks = safetyTimeoutTicks;
		}

		private Vector3f createDustColor() {
			return new Vector3f(
					((this.dustRgb >> 16) & 0xFF) / 255.0F,
					((this.dustRgb >> 8) & 0xFF) / 255.0F,
					(this.dustRgb & 0xFF) / 255.0F
			);
		}
	}

	public static void start(int entityId, CastProfile profile) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null || profile == null)
			return;

		Entity entity = level.getEntity(entityId);
		if (!(entity instanceof AbstractClientPlayer player) || !player.isAlive() || player.isRemoved())
			return;

		if (trackedLevel != level) {
			clear();
			trackedLevel = level;
		}

		stop(entityId);
		long startGameTime = level.getGameTime();
		STATES.put(entityId, new HandDustState(
				entityId,
				profile,
				startGameTime,
				mix(entityId, startGameTime, profile)
		));
	}

	public static void stop(int entityId) {
		if (STATES.remove(entityId) != null)
			PlayerCastHandAnchorTracker.remove(entityId);
	}

	public static void stop(int entityId, CastProfile profile) {
		HandDustState state = STATES.get(entityId);
		if (state != null && state.profile == profile) {
			STATES.remove(entityId);
			PlayerCastHandAnchorTracker.remove(entityId);
		}
	}

	public static boolean isActive(int entityId) {
		return STATES.containsKey(entityId);
	}

	public static void clear() {
		STATES.clear();
		PlayerCastHandAnchorTracker.clear();
		trackedLevel = null;
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		ClientLevel level = Minecraft.getInstance().level;
		if (trackedLevel != level) {
			clear();
			trackedLevel = level;
		}
		if (level == null)
			return;

		long gameTime = level.getGameTime();
		PlayerCastHandAnchorTracker.prune(gameTime);
		Iterator<HandDustState> iterator = STATES.values().iterator();
		while (iterator.hasNext()) {
			HandDustState state = iterator.next();
			Entity entity = level.getEntity(state.entityId);
			AbstractClientPlayer player = entity instanceof AbstractClientPlayer clientPlayer
					&& clientPlayer.isAlive() && !clientPlayer.isRemoved()
					? clientPlayer
					: null;
			long elapsedTicks = gameTime - state.startGameTime;
			if (player == null
					|| elapsedTicks >= state.profile.safetyTimeoutTicks
					|| !isProfileAnimationActive(state.profile, player)) {
				iterator.remove();
				PlayerCastHandAnchorTracker.remove(state.entityId);
				continue;
			}

			if (state.lastSpawnGameTime == gameTime)
				continue;
			PlayerCastHandAnchorTracker.HandAnchors anchors =
					PlayerCastHandAnchorTracker.getFresh(state.entityId, gameTime);
			if (anchors == null)
				continue;

			RandomSource random = RandomSource.create(
					state.seed ^ gameTime * 0x9E3779B97F4A7C15L
			);
			spawnHandDustAt(level, anchors.right(), state.profile, random);
			spawnHandDustAt(level, anchors.left(), state.profile, random);
			state.lastSpawnGameTime = gameTime;
		}
	}

	private static boolean isProfileAnimationActive(CastProfile profile, AbstractClientPlayer player) {
		return switch (profile) {
			case CHERUBIMS_WISDOM -> CherubimsWisdomPlayerAnimation.isActive(player);
			case BEATIFIC_PALLIUM -> BeatificPalliumPlayerAnimation.isActive(player);
		};
	}

	private static void spawnHandDustAt(
			ClientLevel level,
			Vec3 anchor,
			CastProfile profile,
			RandomSource random
	) {
		if (anchor == null)
			return;

		float size = MIN_SIZE + random.nextFloat() * SIZE_RANGE;
		DustParticleOptions dust = new DustParticleOptions(profile.createDustColor(), size);
		double jitterX = (random.nextDouble() - 0.5D) * JITTER_RANGE;
		double jitterY = (random.nextDouble() - 0.5D) * JITTER_RANGE;
		double jitterZ = (random.nextDouble() - 0.5D) * JITTER_RANGE;
		level.addParticle(
				dust,
				anchor.x + jitterX,
				anchor.y + jitterY,
				anchor.z + jitterZ,
				0.0D,
				VELOCITY_Y,
				0.0D
		);
	}

	private static long mix(int entityId, long gameTime, CastProfile profile) {
		long value = gameTime
				^ (long) entityId * 0x9E3779B97F4A7C15L
				^ (long) profile.ordinal() * 0xD1B54A32D192ED03L;
		value ^= value >>> 30;
		value *= 0xBF58476D1CE4E5B9L;
		value ^= value >>> 27;
		value *= 0x94D049BB133111EBL;
		return value ^ value >>> 31;
	}

	private static final class HandDustState {
		private final int entityId;
		private final CastProfile profile;
		private final long startGameTime;
		private final long seed;
		private long lastSpawnGameTime = Long.MIN_VALUE;

		private HandDustState(int entityId, CastProfile profile, long startGameTime, long seed) {
			this.entityId = entityId;
			this.profile = profile;
			this.startGameTime = startGameTime;
			this.seed = seed;
		}
	}
}
