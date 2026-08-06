package net.timothaty.timothatystrinkets.client.gorge;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EventBusSubscriber(
		modid = TimothatysTrinketsMod.MODID,
		value = Dist.CLIENT
)
public final class GorgeAnimationState {
	public static final int DURATION_TICKS = 14;

	private static final float BODY_TURN_END_TICK = 5.0F;
	private static final float BODY_RETURN_START_TICK = 10.0F;
	private static final Map<Integer, State> STATES = new HashMap<>();
	private static ClientLevel trackedLevel;

	private GorgeAnimationState() {
	}

	public static void start(
			int consumerEntityId,
			double targetX,
			double targetZ
	) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null || consumerEntityId < 0)
			return;
		if (trackedLevel != level) {
			clear();
			trackedLevel = level;
		}
		STATES.put(
				consumerEntityId,
				new State(level.getGameTime(), targetX, targetZ)
		);
	}

	public static float elapsedTicks(
			LivingEntity entity,
			float ageInTicks
	) {
		if (!isActive(entity))
			return -1.0F;

		State state = STATES.get(entity.getId());
		float partialTick = Mth.clamp(
				ageInTicks - entity.tickCount,
				0.0F,
				1.0F
		);
		return entity.level().getGameTime()
				- state.startGameTime
				+ partialTick;
	}

	public static boolean isActive(Entity entity) {
		if (!(entity instanceof LivingEntity living)
				|| !living.isAlive()
				|| living.isDeadOrDying()
				|| living.isRemoved()
				|| living.level() != trackedLevel) {
			return false;
		}

		State state = STATES.get(living.getId());
		if (state == null)
			return false;
		long elapsed = living.level().getGameTime() - state.startGameTime;
		if (elapsed >= DURATION_TICKS) {
			STATES.remove(living.getId());
			return false;
		}
		return true;
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			clear();
			return;
		}
		if (trackedLevel != level) {
			clear();
			trackedLevel = level;
			return;
		}

		long now = level.getGameTime();
		Iterator<Map.Entry<Integer, State>> iterator =
				STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, State> entry = iterator.next();
			Entity entity = level.getEntity(entry.getKey());
			State state = entry.getValue();
			long elapsed = now - state.startGameTime;
			if (elapsed >= DURATION_TICKS
					|| entity != null
					&& (!entity.isAlive() || entity.isRemoved())) {
				iterator.remove();
				continue;
			}
			if (entity instanceof LivingEntity living)
				applyBodyRotation(living, state, elapsed);
		}
	}

	public static void clear() {
		STATES.clear();
		trackedLevel = null;
	}

	private static void applyBodyRotation(
			LivingEntity entity,
			State state,
			long elapsed
	) {
		float vanillaBodyYaw = entity.yBodyRot;
		if (!state.rotationInitialized) {
			state.initialBodyYaw = vanillaBodyYaw;
			double deltaX = state.targetX - entity.getX();
			double deltaZ = state.targetZ - entity.getZ();
			state.targetBodyYaw = deltaX * deltaX + deltaZ * deltaZ > 1.0E-6D
					? (float) (Mth.atan2(deltaZ, deltaX) * Mth.RAD_TO_DEG) - 90.0F
					: vanillaBodyYaw;
			state.lastAppliedBodyYaw = vanillaBodyYaw;
			state.rotationInitialized = true;
		}

		float bodyYaw;
		if (elapsed < BODY_TURN_END_TICK) {
			float progress = smoothStep(elapsed / BODY_TURN_END_TICK);
			bodyYaw = Mth.rotLerp(
					progress,
					state.initialBodyYaw,
					state.targetBodyYaw
			);
		} else if (elapsed < BODY_RETURN_START_TICK) {
			bodyYaw = state.targetBodyYaw;
		} else {
			float progress = smoothStep(
					((float) elapsed - BODY_RETURN_START_TICK)
							/ (DURATION_TICKS - BODY_RETURN_START_TICK)
			);
			bodyYaw = Mth.rotLerp(
					progress,
					state.targetBodyYaw,
					vanillaBodyYaw
			);
		}

		entity.yBodyRotO = state.lastAppliedBodyYaw;
		entity.yBodyRot = bodyYaw;
		state.lastAppliedBodyYaw = bodyYaw;
	}

	private static float smoothStep(float value) {
		float progress = Mth.clamp(value, 0.0F, 1.0F);
		return progress * progress * (3.0F - 2.0F * progress);
	}

	private static final class State {
		private final long startGameTime;
		private final double targetX;
		private final double targetZ;
		private float initialBodyYaw;
		private float targetBodyYaw;
		private float lastAppliedBodyYaw;
		private boolean rotationInitialized;

		private State(long startGameTime, double targetX, double targetZ) {
			this.startGameTime = startGameTime;
			this.targetX = targetX;
			this.targetZ = targetZ;
		}
	}
}
