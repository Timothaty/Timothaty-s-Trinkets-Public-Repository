package net.timothaty.timothatystrinkets.client.stunned;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class StunnedClientAnimationState {
	private static final int STUNNED_VISUAL_TIMEOUT_TICKS = 10;
	private static final Map<Integer, Long> STUNNED_UNTIL_TICK = new HashMap<>();

	private StunnedClientAnimationState() {
	}

	public static void markStunned(int entityId, ClientLevel level) {
		if (entityId <= 0 || level == null)
			return;

		long now = level.getGameTime();
		STUNNED_UNTIL_TICK.put(entityId, now + STUNNED_VISUAL_TIMEOUT_TICKS);
		cleanupExpired(now);
	}

	public static boolean isStunned(Entity entity) {
		if (!(entity instanceof LivingEntity living))
			return false;
		if (living instanceof Player player && (player.isCreative() || player.isSpectator()))
			return false;
		if (living.hasEffect(TimothatysTrinketsModMobEffects.STUNNED))
			return true;
		if (!(living.level() instanceof ClientLevel clientLevel))
			return false;

		long now = clientLevel.getGameTime();
		Long stunnedUntil = STUNNED_UNTIL_TICK.get(living.getId());
		if (stunnedUntil == null)
			return false;

		if (stunnedUntil < now) {
			STUNNED_UNTIL_TICK.remove(living.getId());
			return false;
		}

		return true;
	}

	private static void cleanupExpired(long now) {
		Iterator<Map.Entry<Integer, Long>> iterator = STUNNED_UNTIL_TICK.entrySet().iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getValue() < now) {
				iterator.remove();
			}
		}
	}

	public static void clear() {
		STUNNED_UNTIL_TICK.clear();
	}
}
