package net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class HumilityGolemRepairTracker {
	private static final Map<UUID, PendingRepair> PENDING = new HashMap<>();

	private HumilityGolemRepairTracker() {
	}

	public static void track(ServerLevel level, IronGolem golem, UUID playerId) {
		PENDING.put(golem.getUUID(), new PendingRepair(level.dimension(), golem.getUUID(), playerId, golem.getHealth(), level.getGameTime() + 1L));
	}

	public static void tick(MinecraftServer server) {
		if (PENDING.isEmpty())
			return;
		Iterator<PendingRepair> iterator = PENDING.values().iterator();
		while (iterator.hasNext()) {
			PendingRepair pending = iterator.next();
			ServerLevel level = server.getLevel(pending.dimension);
			if (level == null || level.getGameTime() < pending.checkAt)
				continue;
			iterator.remove();
			Entity entity = level.getEntity(pending.golemId);
			if (entity instanceof IronGolem golem
					&& !golem.isPlayerCreated()
					&& pending.previousHealth < golem.getMaxHealth()
					&& golem.getHealth() >= golem.getMaxHealth())
				HumilityQuestService.recordDeed(server, pending.playerId, HumilityDeedType.REPAIR_NATURAL_GOLEM);
		}
	}

	public static boolean hasPending() {
		return !PENDING.isEmpty();
	}

	public static void clear() {
		PENDING.clear();
	}

	private record PendingRepair(ResourceKey<Level> dimension, UUID golemId, UUID playerId, float previousHealth, long checkAt) {
	}
}
