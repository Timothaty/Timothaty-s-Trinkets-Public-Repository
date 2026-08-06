package net.timothaty.timothatystrinkets.client.morgenshtern;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class MorgenshternDecapitationClientState {
	private static final Set<LivingEntity> DECAPITATED =
			Collections.newSetFromMap(new WeakHashMap<>());

	private MorgenshternDecapitationClientState() {
	}

	public static void mark(int entityId) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return;

		Entity entity = minecraft.level.getEntity(entityId);
		if (entity instanceof LivingEntity livingEntity) {
			DECAPITATED.add(livingEntity);
		}
	}

	public static boolean isDecapitated(LivingEntity entity) {
		return entity != null && DECAPITATED.contains(entity);
	}

	public static void clear() {
		DECAPITATED.clear();
	}
}
