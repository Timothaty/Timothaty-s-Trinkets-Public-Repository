package net.timothaty.timothatystrinkets.client.wrath_of_the_wicked;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.handler.WrathOfTheWickedCameraHandler;
import net.timothaty.timothatystrinkets.client.stunned.StunnedClientAnimationState;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.wrath_of_the_wicked.WrathOfTheWickedData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class WrathOfTheWickedClientState {
	public static final int BODY_ROTATION_START_TICK = 20;
	public static final int BODY_ROTATION_END_TICK = 60;
	public static final int BODY_RETURN_END_TICK = 63;
	public static final int VISUAL_END_TICK = 70;

	private static final float FULL_ROTATION_DEGREES = 360.0F;
	private static final Map<Integer, VisualState> STATES = new HashMap<>();

	private static ClientLevel trackedLevel;

	private WrathOfTheWickedClientState() {
	}

	public static void setVisualState(
			int entityId,
			long startGameTime,
			float initialYaw,
			boolean rotationLocked,
			boolean active
	) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		synchronizeLevel(level);
		if (!active) {
			VisualState removed = STATES.remove(entityId);
			resetBodyRotation(level == null ? null : level.getEntity(entityId), removed);
			WrathOfTheWickedLightningRenderer.stop(entityId);
			clearLocalFeedback(entityId);
			return;
		}
		if (level == null || entityId <= 0) {
			return;
		}

		VisualState existing = STATES.get(entityId);
		if (existing != null && existing.startGameTime == startGameTime) {
			existing.updateRotation(entityId, initialYaw, rotationLocked);
			if (!existing.controlInterrupted)
				WrathOfTheWickedLightningRenderer.start(entityId, startGameTime);
		} else {
			VisualState state = new VisualState(
					entityId,
					startGameTime,
					initialYaw,
					rotationLocked
			);
			STATES.put(entityId, state);
			WrathOfTheWickedLightningRenderer.start(entityId, startGameTime);
		}
	}

	public static boolean isActive(Entity entity) {
		VisualState state = findActiveState(entity);
		return state != null && elapsedTicks(entity.level().getGameTime(), state) < VISUAL_END_TICK;
	}

	public static boolean isMovementLocked(LocalPlayer player) {
		VisualState state = findActiveState(player);
		if (state == null) {
			return false;
		}

		long elapsed = elapsedTicks(player.level().getGameTime(), state);
		return elapsed >= BODY_ROTATION_START_TICK && elapsed < BODY_ROTATION_END_TICK;
	}

	public static boolean isMouseLocked(LocalPlayer player) {
		VisualState state = findActiveState(player);
		if (state == null || !state.rotationLocked)
			return false;

		long elapsed = elapsedTicks(player.level().getGameTime(), state);
		return elapsed >= BODY_ROTATION_START_TICK && elapsed < BODY_ROTATION_END_TICK;
	}

	public static CameraPose getCameraPose(LocalPlayer player, float partialTick) {
		VisualState state = findActiveState(player);
		if (state == null || !state.rotationLocked)
			return null;

		float elapsed = elapsedTicks(player.level().getGameTime(), state)
				+ Mth.clamp(partialTick, 0.0F, 1.0F);
		if (elapsed < BODY_ROTATION_START_TICK || elapsed >= BODY_ROTATION_END_TICK)
			return null;

		float progress = Mth.clamp(
				(elapsed - BODY_ROTATION_START_TICK)
						/ (BODY_ROTATION_END_TICK - BODY_ROTATION_START_TICK),
				0.0F,
				1.0F
		);
		float smoothProgress = progress * progress * (3.0F - 2.0F * progress);
		return new CameraPose(
				state.initialYaw + FULL_ROTATION_DEGREES * smoothProgress,
				state.initialPitch
		);
	}

	public static float getAnimationElapsedTicks(LivingEntity entity, float ageInTicks) {
		VisualState state = findActiveState(entity);
		if (state == null) {
			return -1.0F;
		}

		float partialTick = Mth.clamp(ageInTicks - entity.tickCount, 0.0F, 1.0F);
		return Mth.clamp(
				(float) elapsedTicks(entity.level().getGameTime(), state) + partialTick,
				0.0F,
				(float) VISUAL_END_TICK
		);
	}

	public static long getVisualStartGameTime(Entity entity) {
		VisualState state = findActiveState(entity);
		return state == null ? Long.MIN_VALUE : state.startGameTime;
	}

	public static void clear() {
		ClientLevel level = Minecraft.getInstance().level;
		if (level != null) {
			for (Map.Entry<Integer, VisualState> entry : STATES.entrySet())
				resetBodyRotation(level.getEntity(entry.getKey()), entry.getValue());
		}
		STATES.clear();
		trackedLevel = null;
		WrathOfTheWickedCameraHandler.clear();
		WrathOfTheWickedLightningRenderer.clear();
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		synchronizeLevel(level);
		if (level == null) {
			return;
		}

		long now = level.getGameTime();
		Iterator<Map.Entry<Integer, VisualState>> iterator = STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, VisualState> entry = iterator.next();
			VisualState state = entry.getValue();
			long elapsed = elapsedTicks(now, state);
			if (elapsed >= VISUAL_END_TICK) {
				resetBodyRotation(level.getEntity(entry.getKey()), state);
				clearLocalFeedback(entry.getKey());
				WrathOfTheWickedLightningRenderer.stop(entry.getKey());
				iterator.remove();
				continue;
			}
			Entity entity = level.getEntity(entry.getKey());
			if (entity == null)
				continue;
			if (!(entity instanceof LivingEntity living)
					|| !living.isAlive()
					|| living.isDeadOrDying()
					|| living.isRemoved()) {
				resetBodyRotation(entity, state);
				clearLocalFeedback(entry.getKey());
				WrathOfTheWickedLightningRenderer.stop(entry.getKey());
				iterator.remove();
				continue;
			}
			if (state.controlInterrupted || isControlInterrupted(living)) {
				suppressForControlEffect(living, state);
				continue;
			}

			if (!state.pulseSpawned && elapsed >= (long) Math.floor(
					WrathOfTheWickedData.PULSE_VISUAL_START_TICK
			)) {
				state.pulseSpawned = true;
				if (elapsed <= Math.ceil(
						WrathOfTheWickedData.PULSE_VISUAL_START_TICK
								+ WrathOfTheWickedData.PULSE_DURATION_TICKS
				)) {
					level.addParticle(
							TimothatysTrinketsModParticleTypes.WICKED_PULSE.get(),
							living.getX(),
							living.getY(),
							living.getZ(),
							(double) elapsed,
							0.0D,
							0.0D
					);
				}
			}

			if (state.rotationLocked
					&& elapsed >= BODY_ROTATION_START_TICK
					&& elapsed <= BODY_RETURN_END_TICK) {
				applyBodyRotation(living, state, elapsed);
			}
		}
	}

	private static VisualState findActiveState(Entity entity) {
		VisualState state = findState(entity);
		if (state == null || state.controlInterrupted)
			return null;
		if (isControlInterrupted(entity)) {
			suppressForControlEffect(entity, state);
			return null;
		}
		return state;
	}

	private static VisualState findState(Entity entity) {
		if (entity == null || !(entity.level() instanceof ClientLevel level)) {
			return null;
		}

		synchronizeLevel(level);
		VisualState state = STATES.get(entity.getId());
		if (state == null) {
			return null;
		}
		if (elapsedTicks(level.getGameTime(), state) >= VISUAL_END_TICK) {
			STATES.remove(entity.getId());
			resetBodyRotation(entity, state);
			clearLocalFeedback(entity.getId());
			WrathOfTheWickedLightningRenderer.stop(entity.getId());
			return null;
		}
		return state;
	}

	private static void applyBodyRotation(LivingEntity entity, VisualState state, long elapsed) {
		float currentYaw;
		float previousYaw;
		if (elapsed <= BODY_ROTATION_END_TICK) {
			currentYaw = bodyYawAt(state, elapsed);
			previousYaw = bodyYawAt(state, Math.max(0L, elapsed - 1L));
		} else {
			float progress = WrathOfTheWickedData.smoothstep(
					(elapsed - BODY_ROTATION_END_TICK)
							/ (float) (BODY_RETURN_END_TICK - BODY_ROTATION_END_TICK)
			);
			float previousProgress = WrathOfTheWickedData.smoothstep(
					(elapsed - 1L - BODY_ROTATION_END_TICK)
							/ (float) (BODY_RETURN_END_TICK - BODY_ROTATION_END_TICK)
			);
			float scriptedEndYaw = bodyYawAt(state, BODY_ROTATION_END_TICK);
			currentYaw = Mth.rotLerp(progress, scriptedEndYaw, entity.getYRot());
			previousYaw = Mth.rotLerp(previousProgress, scriptedEndYaw, entity.getYRot());
		}
		entity.yBodyRotO = previousYaw;
		entity.yBodyRot = currentYaw;
	}

	private static float bodyYawAt(VisualState state, long elapsed) {
		float progress = Mth.clamp(
				((float) elapsed - BODY_ROTATION_START_TICK)
						/ (BODY_ROTATION_END_TICK - BODY_ROTATION_START_TICK),
				0.0F,
				1.0F
		);
		float smoothProgress = progress * progress * (3.0F - 2.0F * progress);
		return state.initialYaw + FULL_ROTATION_DEGREES * smoothProgress;
	}

	private static boolean isControlInterrupted(Entity entity) {
		return entity instanceof LivingEntity living
				&& (StunnedClientAnimationState.isStunned(living)
						|| living.hasEffect(TimothatysTrinketsModMobEffects.STAGGER));
	}

	private static void suppressForControlEffect(Entity entity, VisualState state) {
		if (state == null || state.controlInterrupted)
			return;
		state.controlInterrupted = true;
		resetBodyRotation(entity, state);
		clearLocalFeedback(entity.getId());
		WrathOfTheWickedLightningRenderer.stop(entity.getId());
	}

	private static void resetBodyRotation(Entity entity, VisualState state) {
		if (state == null || state.bodyRotationReset)
			return;
		state.bodyRotationReset = true;
		if (entity instanceof LivingEntity living) {
			float stableYaw = living.getYRot();
			living.yBodyRot = stableYaw;
			living.yBodyRotO = stableYaw;
		}
	}

	private static long elapsedTicks(long currentGameTime, VisualState state) {
		return Math.max(0L, currentGameTime - state.startGameTime);
	}

	private static void synchronizeLevel(ClientLevel level) {
		if (trackedLevel == level) {
			return;
		}
		STATES.clear();
		WrathOfTheWickedCameraHandler.clear();
		WrathOfTheWickedLightningRenderer.clear();
		trackedLevel = level;
	}

	private static void clearLocalFeedback(int entityId) {
		LocalPlayer localPlayer = Minecraft.getInstance().player;
		if (localPlayer != null && localPlayer.getId() == entityId)
			WrathOfTheWickedCameraHandler.clear();
	}

	public record CameraPose(float yaw, float pitch) {
	}

	private static final class VisualState {
		private final long startGameTime;
		private float initialYaw;
		private float initialPitch;
		private boolean rotationLocked;
		private boolean pulseSpawned;
		private boolean controlInterrupted;
		private boolean bodyRotationReset;

		private VisualState(
				int entityId,
				long startGameTime,
				float initialYaw,
				boolean rotationLocked
		) {
			this.startGameTime = startGameTime;
			updateRotation(entityId, initialYaw, rotationLocked);
		}

		private void updateRotation(int entityId, float initialYaw, boolean rotationLocked) {
			boolean newlyLocked = rotationLocked && !this.rotationLocked;
			this.initialYaw = initialYaw;
			this.rotationLocked = rotationLocked;
			if (!newlyLocked)
				return;

			LocalPlayer localPlayer = Minecraft.getInstance().player;
			if (localPlayer != null && localPlayer.getId() == entityId)
				this.initialPitch = localPlayer.getXRot();
		}
	}
}
