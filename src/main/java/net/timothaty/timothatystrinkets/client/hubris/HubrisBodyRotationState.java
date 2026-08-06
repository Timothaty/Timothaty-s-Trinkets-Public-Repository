package net.timothaty.timothatystrinkets.client.hubris;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris.HubrisData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class HubrisBodyRotationState {
	private static final float BODY_TURN_DEGREES = 36.0F;
	private static final float TURN_END_TICK = 6.0F;
	private static final float RETURN_START_TICK = 23.0F;
	private static final int FORCED_RETURN_TICKS = 3;

	private static final Map<Integer, State> STATES = new HashMap<>();
	private static ClientLevel trackedLevel;

	private HubrisBodyRotationState() {
	}

	public static void start(int entityId, long startGameTime, HumanoidArm mainArm) {
		ClientLevel level = Minecraft.getInstance().level;
		synchronizeLevel(level);
		if (level == null || entityId <= 0 || mainArm == null)
			return;
		STATES.put(entityId, new State(startGameTime, mainArm));
	}

	public static void finish(int entityId) {
		ClientLevel level = Minecraft.getInstance().level;
		synchronizeLevel(level);
		State state = STATES.get(entityId);
		if (level == null || state == null || state.returning)
			return;

		Entity entity = level.getEntity(entityId);
		state.returning = true;
		state.returnStartGameTime = level.getGameTime();
		state.returnStartBodyYaw = entity instanceof LivingEntity living
				? living.yBodyRot
				: state.lastAppliedBodyYaw;
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		ClientLevel level = Minecraft.getInstance().level;
		synchronizeLevel(level);
		if (level == null)
			return;

		long now = level.getGameTime();
		Iterator<Map.Entry<Integer, State>> iterator = STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, State> entry = iterator.next();
			Entity entity = level.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity living)
					|| !living.isAlive()
					|| living.isDeadOrDying()
					|| living.isRemoved()) {
				iterator.remove();
				continue;
			}

			State state = entry.getValue();
			initialize(living, state);
			if (state.returning) {
				if (applyForcedReturn(living, state, now))
					iterator.remove();
				continue;
			}

			long elapsed = now - state.startGameTime;
			if (elapsed >= HubrisData.ACTIVATION_TICKS) {
				state.returning = true;
				state.returnStartGameTime = now;
				state.returnStartBodyYaw = state.lastAppliedBodyYaw;
				continue;
			}
			applyCastRotation(living, state, elapsed);
		}
	}

	public static void clear() {
		STATES.clear();
		trackedLevel = null;
	}

	private static void initialize(LivingEntity entity, State state) {
		if (state.initialized)
			return;
		state.initialBodyYaw = entity.yBodyRot;
		state.targetBodyYaw = state.initialBodyYaw
				+ (state.mainArm == HumanoidArm.RIGHT ? BODY_TURN_DEGREES : -BODY_TURN_DEGREES);
		state.lastAppliedBodyYaw = state.initialBodyYaw;
		state.initialized = true;
	}

	private static void applyCastRotation(LivingEntity entity, State state, long elapsed) {
		float bodyYaw;
		if (elapsed < TURN_END_TICK) {
			bodyYaw = Mth.rotLerp(
					smoothStep(elapsed / TURN_END_TICK),
					state.initialBodyYaw,
					state.targetBodyYaw
			);
		} else if (elapsed < RETURN_START_TICK) {
			bodyYaw = state.targetBodyYaw;
		} else {
			float returnDuration = HubrisData.ACTIVATION_TICKS - RETURN_START_TICK;
			bodyYaw = Mth.rotLerp(
					smoothStep((elapsed - RETURN_START_TICK) / returnDuration),
					state.targetBodyYaw,
					entity.getYRot()
			);
		}
		apply(entity, state, bodyYaw);
	}

	private static boolean applyForcedReturn(LivingEntity entity, State state, long now) {
		long elapsed = Math.max(0L, now - state.returnStartGameTime);
		if (elapsed >= FORCED_RETURN_TICKS) {
			entity.yBodyRotO = entity.getYRot();
			entity.yBodyRot = entity.getYRot();
			return true;
		}
		float bodyYaw = Mth.rotLerp(
				smoothStep(elapsed / (float) FORCED_RETURN_TICKS),
				state.returnStartBodyYaw,
				entity.getYRot()
		);
		apply(entity, state, bodyYaw);
		return false;
	}

	private static void apply(LivingEntity entity, State state, float bodyYaw) {
		entity.yBodyRotO = state.lastAppliedBodyYaw;
		entity.yBodyRot = bodyYaw;
		state.lastAppliedBodyYaw = bodyYaw;
	}

	private static float smoothStep(float value) {
		float progress = Mth.clamp(value, 0.0F, 1.0F);
		return progress * progress * (3.0F - 2.0F * progress);
	}

	private static void synchronizeLevel(ClientLevel level) {
		if (trackedLevel == level)
			return;
		STATES.clear();
		trackedLevel = level;
	}

	private static final class State {
		private final long startGameTime;
		private final HumanoidArm mainArm;
		private float initialBodyYaw;
		private float targetBodyYaw;
		private float lastAppliedBodyYaw;
		private float returnStartBodyYaw;
		private long returnStartGameTime;
		private boolean initialized;
		private boolean returning;

		private State(long startGameTime, HumanoidArm mainArm) {
			this.startGameTime = startGameTime;
			this.mainArm = mainArm;
		}
	}
}
