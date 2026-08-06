package net.timothaty.timothatystrinkets.client.hubris;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris.HubrisData;
import net.timothaty.timothatystrinkets.network.HubrisVisualStateMessage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class HubrisClientState {
	private static final int FADE_IN_TICKS = 5;
	private static final int FADE_OUT_TICKS = 2;
	private static final int THORN_FADE_TICKS = 3;
	private static final int KEEPALIVE_TIMEOUT_TICKS = 45;
	private static final int MIN_DOT_INTERVAL_TICKS = 12;
	private static final int DOT_INTERVAL_VARIATION_TICKS = 9;
	private static final float DOT_MIN_RADIUS = 0.31F;
	private static final float DOT_RADIUS_VARIATION = 0.08F;
	private static final float THORN_RADIUS = 0.38F;
	private static final Map<Integer, NimbusState> STATES = new HashMap<>();
	private static ClientLevel trackedLevel;

	private HubrisClientState() {
	}

	public static void handle(HubrisVisualStateMessage message) {
		ClientLevel level = Minecraft.getInstance().level;
		synchronizeLevel(level);
		if (level == null || message.entityId() <= 0)
			return;

		int remainingThorns = Mth.clamp(message.remainingThorns(), 0, HubrisData.INITIAL_THORNS);
		NimbusState existing = STATES.get(message.entityId());
		if (!message.active()) {
			if (existing != null && existing.sessionToken == message.sessionToken()) {
				existing.updateRemainingThorns(level, remainingThorns, true);
				existing.terminated = true;
				existing.startFadeOut();
			}
			return;
		}
		if (existing != null && existing.startGameTime > message.startGameTime())
			return;
		if (existing != null && existing.sessionToken == message.sessionToken()) {
			if (existing.terminated)
				return;
			existing.endGameTime = message.endGameTime();
			existing.lastServerUpdateGameTime = level.getGameTime();
			existing.updateRemainingThorns(level, remainingThorns, true);
			return;
		}

		STATES.put(message.entityId(), new NimbusState(
				message.entityId(),
				message.sessionToken(),
				message.startGameTime(),
				message.endGameTime(),
				level.getGameTime(),
				remainingThorns
		));
	}

	public static Collection<NimbusState> states() {
		return STATES.values();
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		ClientLevel level = Minecraft.getInstance().level;
		synchronizeLevel(level);
		if (level == null)
			return;

		long now = level.getGameTime();
		Iterator<NimbusState> iterator = STATES.values().iterator();
		while (iterator.hasNext()) {
			NimbusState state = iterator.next();
			Entity entity = level.getEntity(state.entityId);
			if (!state.fadingOut && (now >= state.endGameTime
					|| now - state.lastServerUpdateGameTime > KEEPALIVE_TIMEOUT_TICKS
					|| entity != null && (!entity.isAlive() || entity.isRemoved())))
				state.startFadeOut();
			state.tick(now);
			if (!state.fadingOut
					&& state.alpha >= 0.75F
					&& entity != null
					&& entity.isAlive()
					&& now >= state.nextDotGameTime) {
				emitNimbusDot(level, entity, state);
				state.nextDotGameTime = now + MIN_DOT_INTERVAL_TICKS
						+ level.random.nextInt(DOT_INTERVAL_VARIATION_TICKS);
			}
			if (state.fadingOut && state.fadeOutTicks >= FADE_OUT_TICKS)
				iterator.remove();
		}
	}

	private static void emitNimbusDot(ClientLevel level, Entity entity, NimbusState state) {
		RandomSource random = level.random;
		float angle = random.nextFloat() * Mth.TWO_PI;
		float radius = DOT_MIN_RADIUS + random.nextFloat() * DOT_RADIUS_VARIATION;
		Vector3f offset = new Vector3f(
				Mth.cos(angle) * radius,
				(random.nextFloat() - 0.5F) * 0.035F,
				Mth.sin(angle) * radius
		);
		state.rotation().transform(offset);

		double centerX = entity.getX();
		double centerY = entity.getY() + entity.getBbHeight() + 0.15D + state.bob;
		double centerZ = entity.getZ();
		Particle particle = createCrimsonDot(level, centerX + offset.x(), centerY + offset.y(), centerZ + offset.z());
		if (particle == null)
			return;

		Vector3f velocity = new Vector3f(offset);
		if (velocity.lengthSquared() > 1.0E-6F)
			velocity.normalize();
		float outwardSpeed = 0.006F + random.nextFloat() * 0.006F;
		particle.setParticleSpeed(
				velocity.x() * outwardSpeed,
				0.010D + random.nextDouble() * 0.008D + velocity.y() * outwardSpeed,
				velocity.z() * outwardSpeed
		);
	}

	private static void emitSpentThornDots(ClientLevel level, Entity entity, NimbusState state, int thornIndex) {
		if (entity == null || !entity.isAlive())
			return;
		RandomSource random = level.random;
		Vector3f offset = new Vector3f(0.0F, 0.0F, THORN_RADIUS)
				.rotateY(thornIndex * 90.0F * Mth.DEG_TO_RAD);
		state.rotation().transform(offset);
		double x = entity.getX() + offset.x();
		double y = entity.getY() + entity.getBbHeight() + 0.15D + state.bob + offset.y();
		double z = entity.getZ() + offset.z();
		int count = 2 + random.nextInt(3);
		for (int index = 0; index < count; index++) {
			Particle particle = createCrimsonDot(
					level,
					x + (random.nextDouble() - 0.5D) * 0.045D,
					y + (random.nextDouble() - 0.5D) * 0.065D,
					z + (random.nextDouble() - 0.5D) * 0.045D
			);
			if (particle == null)
				continue;
			Vector3f velocity = new Vector3f(offset);
			if (velocity.lengthSquared() > 1.0E-6F)
				velocity.normalize();
			particle.setParticleSpeed(
					velocity.x() * (0.012F + random.nextFloat() * 0.009F),
					0.012D + random.nextDouble() * 0.012D,
					velocity.z() * (0.012F + random.nextFloat() * 0.009F)
			);
		}
	}

	private static Particle createCrimsonDot(ClientLevel level, double x, double y, double z) {
		return Minecraft.getInstance().particleEngine.createParticle(
				TimothatysTrinketsModParticleTypes.DOT.get(),
				x,
				y,
				z,
				HubrisData.CRIMSON_RED,
				HubrisData.CRIMSON_GREEN,
				HubrisData.CRIMSON_BLUE
		);
	}

	public static void clear() {
		STATES.clear();
		trackedLevel = null;
	}

	private static void synchronizeLevel(ClientLevel level) {
		if (trackedLevel == level)
			return;
		STATES.clear();
		trackedLevel = level;
	}

	private static float smoothstep(float value) {
		float x = Mth.clamp(value, 0.0F, 1.0F);
		return x * x * (3.0F - 2.0F * x);
	}

	public static final class NimbusState {
		private final int entityId;
		private final long sessionToken;
		private final long startGameTime;
		private final float phase;
		private final ThornVisual[] thorns = new ThornVisual[HubrisData.INITIAL_THORNS];
		private long endGameTime;
		private long lastServerUpdateGameTime;
		private long nextDotGameTime;
		private int remainingThorns;
		private int fadeInTicks;
		private int fadeOutTicks;
		private boolean fadingOut;
		private boolean terminated;
		private float alpha;
		private float previousAlpha;
		private float yaw;
		private float previousYaw;
		private float bob;
		private float previousBob;
		private float pitch;
		private float previousPitch;
		private float roll;
		private float previousRoll;
		private float fadeOutStartAlpha = 1.0F;

		private NimbusState(
				int entityId,
				long sessionToken,
				long startGameTime,
				long endGameTime,
				long localGameTime,
				int remainingThorns
		) {
			this.entityId = entityId;
			this.sessionToken = sessionToken;
			this.startGameTime = startGameTime;
			this.endGameTime = endGameTime;
			this.lastServerUpdateGameTime = localGameTime;
			this.nextDotGameTime = localGameTime + 6L + Math.floorMod(entityId * 31L + startGameTime, 7L);
			this.phase = Math.floorMod(entityId * 31L + startGameTime, 360L) * Mth.DEG_TO_RAD;
			this.yaw = Mth.wrapDegrees((float) (localGameTime - startGameTime) * 1.35F + phase * Mth.RAD_TO_DEG);
			this.previousYaw = yaw;
			this.remainingThorns = HubrisData.INITIAL_THORNS;
			for (int index = 0; index < thorns.length; index++)
				thorns[index] = new ThornVisual();
			setInitialRemainingThorns(remainingThorns);
		}

		private void tick(long now) {
			previousAlpha = alpha;
			previousYaw = yaw;
			previousBob = bob;
			previousPitch = pitch;
			previousRoll = roll;
			for (ThornVisual thorn : thorns)
				thorn.tick();

			float elapsed = now - startGameTime;
			yaw = Mth.wrapDegrees(elapsed * 1.35F + phase * Mth.RAD_TO_DEG);
			bob = Mth.sin(elapsed * 0.115F + phase) * 0.028F;
			pitch = Mth.sin(elapsed * 0.072F + phase * 0.73F) * 2.5F;
			roll = Mth.cos(elapsed * 0.083F + phase * 1.17F) * 2.5F;

			if (fadingOut) {
				fadeOutTicks++;
				alpha = fadeOutStartAlpha * (1.0F - smoothstep(fadeOutTicks / (float) FADE_OUT_TICKS));
			} else {
				fadeInTicks = Math.min(FADE_IN_TICKS, fadeInTicks + 1);
				alpha = smoothstep(fadeInTicks / (float) FADE_IN_TICKS);
			}
		}

		private void setInitialRemainingThorns(int remaining) {
			remainingThorns = Mth.clamp(remaining, 0, HubrisData.INITIAL_THORNS);
			int consumed = HubrisData.INITIAL_THORNS - remainingThorns;
			for (int index = 0; index < consumed; index++)
				thorns[index].hideImmediately();
		}

		private void updateRemainingThorns(ClientLevel level, int remaining, boolean emitDots) {
			int clamped = Mth.clamp(remaining, 0, HubrisData.INITIAL_THORNS);
			if (clamped >= remainingThorns)
				return;
			int oldConsumed = HubrisData.INITIAL_THORNS - remainingThorns;
			int newConsumed = HubrisData.INITIAL_THORNS - clamped;
			Entity entity = level.getEntity(entityId);
			for (int index = oldConsumed; index < newConsumed; index++) {
				thorns[index].startFadeOut();
				if (emitDots)
					emitSpentThornDots(level, entity, this, index);
			}
			remainingThorns = clamped;
		}

		private void startFadeOut() {
			if (fadingOut)
				return;
			fadingOut = true;
			fadeOutTicks = 0;
			fadeOutStartAlpha = alpha;
		}

		private Quaternionf rotation() {
			return new Quaternionf()
					.rotationY(yaw * Mth.DEG_TO_RAD)
					.rotateX(pitch * Mth.DEG_TO_RAD)
					.rotateZ(roll * Mth.DEG_TO_RAD);
		}

		public int entityId() {
			return entityId;
		}

		public float alpha(float partialTick) {
			return Mth.lerp(partialTick, previousAlpha, alpha);
		}

		public float yaw(float partialTick) {
			return Mth.rotLerp(partialTick, previousYaw, yaw);
		}

		public float bob(float partialTick) {
			return Mth.lerp(partialTick, previousBob, bob);
		}

		public float pitch(float partialTick) {
			return Mth.lerp(partialTick, previousPitch, pitch);
		}

		public float roll(float partialTick) {
			return Mth.lerp(partialTick, previousRoll, roll);
		}

		public float thornAlpha(int index, float partialTick) {
			return index < 0 || index >= thorns.length ? 0.0F : thorns[index].alpha(partialTick);
		}

		public float thornScale(int index, float partialTick) {
			return index < 0 || index >= thorns.length ? 0.0F : thorns[index].scale(partialTick);
		}
	}

	private static final class ThornVisual {
		private int fadeTicks;
		private boolean fadingOut;
		private float previousAlpha = 1.0F;
		private float alpha = 1.0F;
		private float previousScale = 1.0F;
		private float scale = 1.0F;

		private void tick() {
			previousAlpha = alpha;
			previousScale = scale;
			if (!fadingOut || alpha <= 0.0F)
				return;
			fadeTicks = Math.min(THORN_FADE_TICKS, fadeTicks + 1);
			float progress = smoothstep(fadeTicks / (float) THORN_FADE_TICKS);
			alpha = 1.0F - progress;
			scale = Mth.lerp(progress, 1.0F, 0.52F);
		}

		private void startFadeOut() {
			if (fadingOut || alpha <= 0.0F)
				return;
			fadingOut = true;
			fadeTicks = 0;
		}

		private void hideImmediately() {
			fadingOut = true;
			fadeTicks = THORN_FADE_TICKS;
			previousAlpha = 0.0F;
			alpha = 0.0F;
			previousScale = 0.52F;
			scale = 0.52F;
		}

		private float alpha(float partialTick) {
			return Mth.lerp(partialTick, previousAlpha, alpha);
		}

		private float scale(float partialTick) {
			return Mth.lerp(partialTick, previousScale, scale);
		}
	}
}
