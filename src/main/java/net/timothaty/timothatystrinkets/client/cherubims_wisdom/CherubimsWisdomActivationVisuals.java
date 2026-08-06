package net.timothaty.timothatystrinkets.client.cherubims_wisdom;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.animation.PlayerCastHandDustVisuals;
import net.timothaty.timothatystrinkets.client.animation.PlayerCastHandDustVisuals.CastProfile;
import net.timothaty.timothatystrinkets.client.animation.PlayerCastAnimationConflicts;
import net.timothaty.timothatystrinkets.client.particle.CherubimsWisdomExperienceDotParticle;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.cherubims_wisdom.CherubimsWisdomData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundSource;
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
public final class CherubimsWisdomActivationVisuals {
	public static final int EXPERIENCE_GREEN_RGB = 0xA6FF5C;

	private static final int EXPERIENCE_DOT_COUNT = 12;
	private static final int ABSORPTION_END_TICK = 7;
	private static final int GOLD_FLASH_COUNT = 20;
	private static final int FINAL_BURST_TICK = 14;
	private static final int FINAL_BURST_COUNT = 22;
	private static final int VISUAL_END_TICK = 18;
	private static final Vector3f HOLY_DUST_COLOR = new Vector3f(
			1.0F,
			241.0F / 255.0F,
			106.0F / 255.0F
	);
	private static final Map<Integer, ActivationState> STATES = new HashMap<>();
	private static ClientLevel trackedLevel;

	private CherubimsWisdomActivationVisuals() {
	}

	public static void start(int entityId) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null)
			return;

		Entity entity = level.getEntity(entityId);
		if (!(entity instanceof AbstractClientPlayer player))
			return;

		if (trackedLevel != level) {
			clear();
			trackedLevel = level;
		}

		stop(entityId);
		long startGameTime = level.getGameTime();
		long seed = mix(entityId, startGameTime);
		ActivationState state = new ActivationState(startGameTime, seed);
		STATES.put(entityId, state);
		spawnExperienceAbsorption(level, player, state);
		state.experienceSpawned = true;
		playActivationSound(level, player, state);
		state.soundPlayed = true;
	}

	public static boolean isActive(int entityId) {
		return STATES.containsKey(entityId);
	}

	public static void stop(int entityId) {
		STATES.remove(entityId);
		PlayerCastHandDustVisuals.stop(entityId, CastProfile.CHERUBIMS_WISDOM);
		CherubimsWisdomExperienceDotParticle.removeForTarget(entityId);
	}

	public static void clear() {
		for (int entityId : STATES.keySet())
			PlayerCastHandDustVisuals.stop(entityId, CastProfile.CHERUBIMS_WISDOM);
		STATES.clear();
		CherubimsWisdomExperienceDotParticle.clearTrackedParticles();
		trackedLevel = null;
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (trackedLevel != level) {
			clear();
			trackedLevel = level;
		}
		if (level == null)
			return;

		long gameTime = level.getGameTime();
		Iterator<Map.Entry<Integer, ActivationState>> iterator = STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, ActivationState> entry = iterator.next();
			int entityId = entry.getKey();
			ActivationState state = entry.getValue();
			long elapsedTicks = gameTime - state.startGameTime;
			Entity entity = level.getEntity(entityId);
			AbstractClientPlayer player = entity instanceof AbstractClientPlayer clientPlayer
					&& clientPlayer.isAlive() && !clientPlayer.isRemoved()
					? clientPlayer
					: null;
			if (player != null && PlayerCastAnimationConflicts.hasVisualConflict(player)) {
				iterator.remove();
				PlayerCastHandDustVisuals.stop(entityId, CastProfile.CHERUBIMS_WISDOM);
				CherubimsWisdomExperienceDotParticle.removeForTarget(entityId);
				continue;
			}

			if (elapsedTicks >= ABSORPTION_END_TICK && !state.goldFlashSpawned && player != null) {
				spawnGoldTransformation(level, player, state);
				state.goldFlashSpawned = true;
			}

			if (elapsedTicks >= FINAL_BURST_TICK && !state.finalBurstSpawned && player != null) {
				spawnFinalBurst(level, player, state);
				state.finalBurstSpawned = true;
			}

			if (elapsedTicks >= VISUAL_END_TICK) {
				iterator.remove();
				PlayerCastHandDustVisuals.stop(entityId, CastProfile.CHERUBIMS_WISDOM);
				CherubimsWisdomExperienceDotParticle.removeForTarget(entityId);
			}
		}
	}

	private static void spawnExperienceAbsorption(
			ClientLevel level,
			AbstractClientPlayer player,
			ActivationState state
	) {
		RandomSource random = RandomSource.create(state.seed ^ 0x243F6A8885A308D3L);
		for (int index = 0; index < EXPERIENCE_DOT_COUNT; index++) {
			double angle = random.nextDouble() * Math.PI * 2.0D;
			double radius = 0.40D + random.nextDouble() * 0.50D;
			double x = player.getX() + Math.cos(angle) * radius;
			double y = player.getY() + 0.16D + random.nextDouble() * Math.min(1.35D, player.getBbHeight() * 0.76D);
			double z = player.getZ() + Math.sin(angle) * radius;
			double curveLift = 0.12D + random.nextDouble() * 0.30D;
			double curveSide = (random.nextDouble() - 0.5D) * 0.44D;
			/* SimpleParticleType motion fields carry target id, curve lift and curve side respectively. */
			level.addParticle(
					TimothatysTrinketsModParticleTypes.CHERUBIMS_WISDOM_EXPERIENCE_DOT.get(),
					x, y, z,
					player.getId(), curveLift, curveSide
			);
		}
	}

	private static void spawnGoldTransformation(
			ClientLevel level,
			AbstractClientPlayer player,
			ActivationState state
	) {
		RandomSource random = RandomSource.create(state.seed ^ 0x13198A2E03707344L);
		Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		for (int index = 0; index < GOLD_FLASH_COUNT; index++) {
			double angle = random.nextDouble() * Math.PI * 2.0D;
			double radius = 0.25D + random.nextDouble() * 0.42D;
			Vec3 position = new Vec3(
					player.getX() + Math.cos(angle) * radius,
					player.getY() + 0.48D + random.nextDouble() * 1.08D,
					player.getZ() + Math.sin(angle) * radius
			);
			if (position.distanceToSqr(camera) < 0.09D)
				position = position.add(Math.cos(angle) * 0.24D, -0.10D, Math.sin(angle) * 0.24D);
			level.addParticle(
					TimothatysTrinketsModParticleTypes.DOT.get(),
					position.x, position.y, position.z,
					CherubimsWisdomData.HOLY_PARTICLE_R,
					CherubimsWisdomData.HOLY_PARTICLE_G,
					CherubimsWisdomData.HOLY_PARTICLE_B
			);
		}
	}

	private static void spawnFinalBurst(
			ClientLevel level,
			AbstractClientPlayer player,
			ActivationState state
	) {
		RandomSource random = RandomSource.create(state.seed ^ 0xA4093822299F31D0L);
		for (int index = 0; index < FINAL_BURST_COUNT; index++) {
			double angle = random.nextDouble() * Math.PI * 2.0D;
			double radius = 0.25D + random.nextDouble() * 0.40D;
			double radialVelocity = 0.016D + random.nextDouble() * 0.030D;
			float size = 0.60F + random.nextFloat() * 0.40F;
			DustParticleOptions dust = new DustParticleOptions(new Vector3f(HOLY_DUST_COLOR), size);
			level.addParticle(
					dust,
					player.getX() + Math.cos(angle) * radius,
					player.getY() + 0.52D + random.nextDouble() * 1.04D,
					player.getZ() + Math.sin(angle) * radius,
					Math.cos(angle) * radialVelocity,
					0.014D + random.nextDouble() * 0.032D,
					Math.sin(angle) * radialVelocity
			);
		}
	}

	private static void playActivationSound(
			ClientLevel level,
			AbstractClientPlayer player,
			ActivationState state
	) {
		RandomSource random = RandomSource.create(state.seed ^ 0x082EFA98EC4E6C89L);
		level.playLocalSound(
				player.getX(),
				player.getY(),
				player.getZ(),
				TimothatysTrinketsModSounds.CHERUBIMS_WISDOM.get(),
				SoundSource.PLAYERS,
				1.0F,
				1.0F + random.nextFloat() * 0.20F,
				false
		);
	}

	private static long mix(int entityId, long gameTime) {
		long value = gameTime ^ (long) entityId * 0x9E3779B97F4A7C15L;
		value ^= value >>> 30;
		value *= 0xBF58476D1CE4E5B9L;
		value ^= value >>> 27;
		value *= 0x94D049BB133111EBL;
		return value ^ value >>> 31;
	}

	private static final class ActivationState {
		private final long startGameTime;
		private final long seed;
		private boolean experienceSpawned;
		private boolean goldFlashSpawned;
		private boolean finalBurstSpawned;
		private boolean soundPlayed;

		private ActivationState(long startGameTime, long seed) {
			this.startGameTime = startGameTime;
			this.seed = seed;
		}
	}
}
