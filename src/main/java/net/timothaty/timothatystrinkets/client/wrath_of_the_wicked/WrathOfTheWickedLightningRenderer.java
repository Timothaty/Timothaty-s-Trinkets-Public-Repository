package net.timothaty.timothatystrinkets.client.wrath_of_the_wicked;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.wrath_of_the_wicked.WrathOfTheWickedData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(
		modid = TimothatysTrinketsMod.MODID,
		value = Dist.CLIENT
)
public final class WrathOfTheWickedLightningRenderer {
	private static final int PRE_CHARGE_FIRST_TICK = 16;
	private static final int PRE_CHARGE_SECOND_TICK = 19;
	private static final int LAST_SPAWN_TICK = 59;
	private static final int VISUAL_END_TICK = 64;
	private static final int MAX_ACTIVE_BOLTS = 6;
	private static final int MAX_BOLT_LIFETIME_TICKS = 6;
	private static final int MAX_TORSO_LIFETIME_TICKS = 5;
	private static final int FORCED_FADE_TICKS = 2;
	private static final int TORSO_FIRST_MIN_TICK = 23;
	private static final int TORSO_FIRST_MAX_TICK = 28;
	private static final int TORSO_INTERVAL_MIN_TICKS = 8;
	private static final int TORSO_INTERVAL_MAX_TICKS = 14;
	private static final int TORSO_MIN_COUNT = 2;
	private static final int TORSO_MAX_COUNT = 4;
	private static final double RENDER_DISTANCE_SQR = 64.0D * 64.0D;
	private static final long SEED_SALT = 0x6A09E667F3BCC909L;
	private static final long TORSO_SCHEDULE_SALT = 0xBB67AE8584CAA73BL;
	private static final long TORSO_BOLT_SALT = 0x3C6EF372FE94F82BL;

	private static final Map<Integer, LightningState> STATES = new HashMap<>();
	private static ClientLevel trackedLevel;

	private WrathOfTheWickedLightningRenderer() {
	}

	public static void start(int entityId, long startGameTime) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null || entityId <= 0)
			return;

		synchronizeLevel(level);
		LightningState existing = STATES.get(entityId);
		if (existing == null || existing.startGameTime != startGameTime)
			STATES.put(entityId, new LightningState(entityId, startGameTime));
	}

	public static void stop(int entityId) {
		LightningState state = STATES.get(entityId);
		ClientLevel level = Minecraft.getInstance().level;
		if (state != null && level != null)
			state.beginForcedFade(level.getGameTime());
	}

	public static void clear() {
		STATES.clear();
		trackedLevel = null;
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			clear();
			return;
		}
		synchronizeLevel(level);

		long now = level.getGameTime();
		Iterator<Map.Entry<Integer, LightningState>> iterator =
				STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			LightningState state = iterator.next().getValue();
			long elapsed = now - state.startGameTime;
			if (elapsed >= VISUAL_END_TICK) {
				iterator.remove();
				continue;
			}
			if (state.forcedFadeStartGameTime != Long.MIN_VALUE) {
				if (now - state.forcedFadeStartGameTime >= FORCED_FADE_TICKS)
					iterator.remove();
				continue;
			}

			Entity entity = level.getEntity(state.entityId);
			if (!(entity instanceof LivingEntity living))
				continue;
			if (!living.isAlive()
					|| living.isDeadOrDying()
					|| living.isRemoved()
					|| !WrathOfTheWickedClientState.isActive(living)
					|| WrathOfTheWickedClientState.getVisualStartGameTime(living)
							!= state.startGameTime) {
				state.beginForcedFade(now);
				continue;
			}

			removeExpiredOrStretchedBolts(living, state, now);
			spawnDueBolts(living, state, elapsed);
		}
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
				|| STATES.isEmpty()) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null || trackedLevel != level) {
			clear();
			return;
		}

		float partialTick = event.getPartialTick()
				.getGameTimeDeltaPartialTick(false);
		Vec3 cameraPosition = event.getCamera().getPosition();
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource =
				minecraft.renderBuffers().bufferSource();
		RenderType renderType = RenderType.lightning();
		VertexConsumer consumer = bufferSource.getBuffer(renderType);

		poseStack.pushPose();
		poseStack.translate(
				-cameraPosition.x,
				-cameraPosition.y,
				-cameraPosition.z
		);
		for (LightningState state : STATES.values()) {
			Entity entity = level.getEntity(state.entityId);
			if (!(entity instanceof LivingEntity living)
					|| cameraPosition.distanceToSqr(living.position()) > RENDER_DISTANCE_SQR) {
				continue;
			}

			float forcedFade = forcedFade(state, level.getGameTime(), partialTick);
			if (forcedFade <= 0.0F)
				continue;
			for (WrathOfTheWickedLightningBolt bolt : state.bolts) {
				WrathOfTheWickedLightningGeometry.render(
						poseStack.last().pose(),
						consumer,
						living,
						bolt,
						level.getGameTime(),
						partialTick,
						forcedFade
				);
			}
		}
		poseStack.popPose();
		bufferSource.endBatch(renderType);
	}

	private static void spawnDueBolts(
			LivingEntity entity,
			LightningState state,
			long elapsedLong
	) {
		if (elapsedLong < PRE_CHARGE_FIRST_TICK)
			return;

		int elapsed = (int) Math.min(LAST_SPAWN_TICK, elapsedLong);
		int firstTick = state.lastProcessedElapsedTick == Integer.MIN_VALUE
				? Math.max(
						PRE_CHARGE_FIRST_TICK,
						(int) Math.min(
								Integer.MAX_VALUE,
								elapsedLong - MAX_BOLT_LIFETIME_TICKS
						)
				)
				: state.lastProcessedElapsedTick + 1;
		for (int tick = firstTick; tick <= elapsed; tick++) {
			if (tick == PRE_CHARGE_FIRST_TICK || tick == PRE_CHARGE_SECOND_TICK) {
				spawnGeneration(entity, state, tick, true);
			} else if (tick >= WrathOfTheWickedData.ANCHOR_START_TICK
					&& (tick - WrathOfTheWickedData.ANCHOR_START_TICK)
					% WrathOfTheWickedData.LASER_STAGE_INTERVAL_TICKS == 0) {
				spawnGeneration(entity, state, tick, false);
			}
		}
		spawnDueTorsoDischarges(entity, state, elapsedLong);
		state.lastProcessedElapsedTick = elapsed;
	}

	private static void spawnGeneration(
			LivingEntity entity,
			LightningState state,
			int generationTick,
			boolean preCharge
	) {
		RandomSource generationRandom = RandomSource.create(
				seedFor(state, generationTick, -1)
		);
		int count = preCharge
				? 1
				: generationTick == WrathOfTheWickedData.ANCHOR_START_TICK
						? 2 + generationRandom.nextInt(2)
						: 1 + (generationRandom.nextFloat() < 0.30F ? 1 : 0);
		for (int index = 0; index < count; index++) {
			makeRoomForBolt(state);
			state.bolts.add(WrathOfTheWickedLightningBolt.create(
					entity,
					seedFor(state, generationTick, index),
					state.startGameTime + generationTick,
					generationTick,
					VISUAL_END_TICK,
					preCharge
			));
		}
	}

	private static void spawnDueTorsoDischarges(
			LivingEntity entity,
			LightningState state,
			long elapsed
	) {
		while (state.nextTorsoScheduleIndex < state.torsoScheduleTicks.length) {
			int scheduleIndex = state.nextTorsoScheduleIndex;
			int spawnTick = state.torsoScheduleTicks[scheduleIndex];
			if (spawnTick > elapsed || spawnTick > LAST_SPAWN_TICK)
				return;
			state.nextTorsoScheduleIndex++;

			if (elapsed - spawnTick >= MAX_TORSO_LIFETIME_TICKS
					|| hasActiveTorsoDischarge(state)) {
				continue;
			}
			WrathOfTheWickedLightningBolt bolt =
					WrathOfTheWickedLightningBolt.createTorsoDischarge(
							entity,
							torsoBoltSeed(state, spawnTick, scheduleIndex),
							state.startGameTime + spawnTick
					);
			if (elapsed - spawnTick >= bolt.lifetimeTicks)
				continue;

			makeRoomForBolt(state);
			state.bolts.add(bolt);
		}
	}

	private static boolean hasActiveTorsoDischarge(LightningState state) {
		for (WrathOfTheWickedLightningBolt bolt : state.bolts) {
			if (bolt.kind
					== WrathOfTheWickedLightningBolt.BoltKind.TORSO_DISCHARGE) {
				return true;
			}
		}
		return false;
	}

	private static void makeRoomForBolt(LightningState state) {
		while (state.bolts.size() >= MAX_ACTIVE_BOLTS) {
			int removalIndex = 0;
			for (int index = 0; index < state.bolts.size(); index++) {
				if (state.bolts.get(index).kind
						== WrathOfTheWickedLightningBolt.BoltKind.STANDARD) {
					removalIndex = index;
					break;
				}
			}
			state.bolts.remove(removalIndex);
		}
	}

	private static void removeExpiredOrStretchedBolts(
			LivingEntity entity,
			LightningState state,
			long now
	) {
		state.bolts.removeIf(bolt -> bolt.isExpiredOrStretched(entity, now));
	}

	private static float forcedFade(
			LightningState state,
			long gameTime,
			float partialTick
	) {
		if (state.forcedFadeStartGameTime == Long.MIN_VALUE)
			return 1.0F;
		return 1.0F - Mth.clamp(
				(gameTime - state.forcedFadeStartGameTime + partialTick)
						/ FORCED_FADE_TICKS,
				0.0F,
				1.0F
		);
	}

	private static long seedFor(
			LightningState state,
			int generationTick,
			int boltIndex
	) {
		long seed = SEED_SALT;
		seed ^= mix64(state.startGameTime);
		seed ^= mix64((long) state.entityId * 0x9E3779B97F4A7C15L);
		seed ^= mix64((long) generationTick * 0xD1B54A32D192ED03L);
		seed ^= mix64((long) (boltIndex + 2) * 0x94D049BB133111EBL);
		return mix64(seed);
	}

	private static long torsoBoltSeed(
			LightningState state,
			int spawnTick,
			int scheduleIndex
	) {
		long seed = TORSO_BOLT_SALT;
		seed ^= mix64(state.startGameTime);
		seed ^= mix64((long) state.entityId * 0x9E3779B97F4A7C15L);
		seed ^= mix64((long) spawnTick * 0xD1B54A32D192ED03L);
		seed ^= mix64((long) scheduleIndex * 0x94D049BB133111EBL);
		return mix64(seed);
	}

	private static int[] createTorsoSchedule(
			int entityId,
			long startGameTime
	) {
		long seed = TORSO_SCHEDULE_SALT
				^ mix64(startGameTime)
				^ mix64((long) entityId * 0x9E3779B97F4A7C15L);
		RandomSource random = RandomSource.create(mix64(seed));
		int targetCount = TORSO_MIN_COUNT
				+ random.nextInt(TORSO_MAX_COUNT - TORSO_MIN_COUNT + 1);
		int[] schedule = new int[targetCount];
		int actualCount = 0;
		int tick = TORSO_FIRST_MIN_TICK
				+ random.nextInt(TORSO_FIRST_MAX_TICK - TORSO_FIRST_MIN_TICK + 1);
		while (actualCount < targetCount && tick <= LAST_SPAWN_TICK) {
			schedule[actualCount++] = tick;
			tick += TORSO_INTERVAL_MIN_TICKS
					+ random.nextInt(
							TORSO_INTERVAL_MAX_TICKS
									- TORSO_INTERVAL_MIN_TICKS
									+ 1
					);
		}
		return Arrays.copyOf(schedule, actualCount);
	}

	private static long mix64(long value) {
		value = (value ^ value >>> 30) * 0xBF58476D1CE4E5B9L;
		value = (value ^ value >>> 27) * 0x94D049BB133111EBL;
		return value ^ value >>> 31;
	}

	private static void synchronizeLevel(ClientLevel level) {
		if (trackedLevel == level)
			return;
		STATES.clear();
		trackedLevel = level;
	}

	private static final class LightningState {
		private final int entityId;
		private final long startGameTime;
		private final List<WrathOfTheWickedLightningBolt> bolts =
				new ArrayList<>(MAX_ACTIVE_BOLTS);
		private final int[] torsoScheduleTicks;
		private int lastProcessedElapsedTick = Integer.MIN_VALUE;
		private int nextTorsoScheduleIndex;
		private long forcedFadeStartGameTime = Long.MIN_VALUE;

		private LightningState(int entityId, long startGameTime) {
			this.entityId = entityId;
			this.startGameTime = startGameTime;
			this.torsoScheduleTicks = createTorsoSchedule(entityId, startGameTime);
		}

		private void beginForcedFade(long gameTime) {
			if (forcedFadeStartGameTime == Long.MIN_VALUE)
				forcedFadeStartGameTime = gameTime;
		}
	}
}
