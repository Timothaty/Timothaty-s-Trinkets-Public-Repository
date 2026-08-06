package net.timothaty.timothatystrinkets.client.soul_empower;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class SoulOrbAbsorptionVisualState {
	private static final int STATE_TIMEOUT_TICKS = 80;
	private static final Map<Integer, PullingState> PULLING_ENTITIES = new HashMap<>();
	private static ClientLevel trackedLevel;

	private SoulOrbAbsorptionVisualState() {
	}

	public static void setPullingState(int entityId, boolean pulling, HumanoidArm arm) {
		if (!pulling || arm == null) {
			PULLING_ENTITIES.remove(entityId);
			return;
		}

		ClientLevel level = Minecraft.getInstance().level;
		if (level != null && level != trackedLevel) {
			PULLING_ENTITIES.clear();
			SoulOrbAbsorptionThirdPersonAnimation.clear();
			trackedLevel = level;
		}
		if (level != null && entityId >= 0) {
			PULLING_ENTITIES.put(entityId, new PullingState(arm, level.getGameTime()));
		}
	}

	public static boolean isPulling(LivingEntity entity) {
		return getValidState(entity) != null;
	}

	public static HumanoidArm getPullingArm(LivingEntity entity) {
		PullingState state = getValidState(entity);
		return state == null ? null : state.arm();
	}

	public static void clientTick(ClientLevel level) {
		if (level != trackedLevel) {
			PULLING_ENTITIES.clear();
			SoulOrbAbsorptionThirdPersonAnimation.clear();
			trackedLevel = level;
		}
		if (level == null) {
			return;
		}

		long gameTime = level.getGameTime();
		Iterator<Map.Entry<Integer, PullingState>> iterator = PULLING_ENTITIES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, PullingState> entry = iterator.next();
			Entity entity = level.getEntity(entry.getKey());
			boolean entityMissing = entity == null && gameTime - entry.getValue().lastUpdateTick() > 20;
			boolean entityInvalid = entity != null && (!(entity instanceof LivingEntity living)
					|| living.isRemoved() || !living.isAlive() || living.isDeadOrDying());
			boolean entityGone = entityMissing || entityInvalid;
			boolean stale = gameTime - entry.getValue().lastUpdateTick() > STATE_TIMEOUT_TICKS;
			if (entityGone || stale) {
				iterator.remove();
				if (entityGone) {
					SoulOrbAbsorptionThirdPersonAnimation.removeEntity(entry.getKey());
				}
			}
		}
		SoulOrbAbsorptionThirdPersonAnimation.cleanStaleStates(gameTime);
	}

	public static void clear() {
		PULLING_ENTITIES.clear();
		SoulOrbAbsorptionThirdPersonAnimation.clear();
		trackedLevel = null;
	}

	private static PullingState getValidState(LivingEntity entity) {
		if (entity == null || entity.isRemoved() || !entity.isAlive() || entity.isDeadOrDying()) {
			return null;
		}
		PullingState state = PULLING_ENTITIES.get(entity.getId());
		if (state == null || !(entity.level() instanceof ClientLevel level)) {
			return null;
		}
		if (level.getGameTime() - state.lastUpdateTick() > STATE_TIMEOUT_TICKS) {
			PULLING_ENTITIES.remove(entity.getId());
			return null;
		}
		return state;
	}

	private record PullingState(HumanoidArm arm, long lastUpdateTick) {
	}
}
