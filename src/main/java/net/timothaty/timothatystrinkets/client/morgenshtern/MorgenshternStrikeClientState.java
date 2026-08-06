package net.timothaty.timothatystrinkets.client.morgenshtern;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

public final class MorgenshternStrikeClientState {
	private static final Map<Integer, Float> START_TICKS = new HashMap<>();

	private MorgenshternStrikeClientState() {
	}

	public static void start(int entityId) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return;

		Entity entity = minecraft.level.getEntity(entityId);
		if (entity instanceof LivingEntity livingEntity) {
			START_TICKS.put(entityId, (float) livingEntity.tickCount);
		}
	}

	public static float elapsedTicks(
			LivingEntity entity,
			float ageInTicks
	) {
		if (entity == null)
			return -1.0F;

		Float startTick = START_TICKS.get(entity.getId());
		if (startTick == null)
			return -1.0F;

		float elapsed = ageInTicks - startTick;
		if (elapsed < 0.0F
				|| elapsed > MorgenshternOberhauAnimation.DURATION_TICKS) {
			START_TICKS.remove(entity.getId());
			return -1.0F;
		}
		return elapsed;
	}

	public static boolean isActive(LivingEntity entity, float ageInTicks) {
		return elapsedTicks(entity, ageInTicks) >= 0.0F;
	}

	public static void clear() {
		START_TICKS.clear();
	}
}
