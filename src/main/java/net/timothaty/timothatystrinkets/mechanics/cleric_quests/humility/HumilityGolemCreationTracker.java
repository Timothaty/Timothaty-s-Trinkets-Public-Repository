package net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility;

import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaVillageRules;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestProgress;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestSavedData;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class HumilityGolemCreationTracker {
	private static final long MAX_AGE_TICKS = 10L;
	private static final long REVERSE_ORDER_MAX_AGE_TICKS = 1L;
	private static final double MATCH_DISTANCE_SQR = 16.0D;
	private static final List<PendingCreation> PENDING = new ArrayList<>();
	private static final List<RecentGolem> RECENT_GOLEMS = new ArrayList<>();

	private HumilityGolemCreationTracker() {
	}

	public static void track(ServerLevel level, BlockPos pumpkinPos, UUID playerId, ActionType actionType) {
		ClericQuestProgress progress = ClericQuestSavedData.get(level).get(playerId);
		if (progress == null
				|| progress.humilityStage() != HumilityStage.DEEDS_ACTIVE
				|| (progress.humilityDeedMask() & HumilityDeedType.CREATE_VILLAGE_GOLEM.bit()) != 0
				|| (actionType == ActionType.CARVING && !AnathemaVillageRules.isVillageTerritory(level, pumpkinPos)))
			return;
		long now = level.getGameTime();
		PENDING.removeIf(existing -> existing.dimension.equals(level.dimension())
			&& existing.pos.equals(pumpkinPos)
			&& existing.playerId.equals(playerId)
			&& existing.actionType == actionType);
		PendingCreation pending = new PendingCreation(level.dimension(), pumpkinPos.immutable(), playerId, now, actionType);
		PENDING.add(pending);
		RecentGolem recent = findRecentMatch(level, pending);
		if (recent != null) {
			PENDING.remove(pending);
			RECENT_GOLEMS.remove(recent);
			HumilityQuestService.recordDeed(level.getServer(), playerId, HumilityDeedType.CREATE_VILLAGE_GOLEM);
		}
	}

	public static void match(ServerLevel level, IronGolem golem) {
		if (!golem.isPlayerCreated() || !AnathemaVillageRules.isVillageTerritory(level, golem.blockPosition()))
			return;
		long now = level.getGameTime();
		if (PENDING.isEmpty()) {
			RECENT_GOLEMS.add(new RecentGolem(level.dimension(), golem.blockPosition().immutable(), now + REVERSE_ORDER_MAX_AGE_TICKS));
			return;
		}
		PendingCreation best = null;
		double bestDistance = MATCH_DISTANCE_SQR;
		for (PendingCreation pending : PENDING) {
			if (!pending.dimension.equals(level.dimension()) || pending.gameTime + MAX_AGE_TICKS < now)
				continue;
			double distance = golem.distanceToSqr(pending.pos.getX() + 0.5D, pending.pos.getY() + 0.5D, pending.pos.getZ() + 0.5D);
			if (distance <= bestDistance) {
				best = pending;
				bestDistance = distance;
			}
		}
		if (best != null) {
			PENDING.remove(best);
			HumilityQuestService.recordDeed(level.getServer(), best.playerId, HumilityDeedType.CREATE_VILLAGE_GOLEM);
		} else {
			RECENT_GOLEMS.add(new RecentGolem(level.dimension(), golem.blockPosition().immutable(), now + REVERSE_ORDER_MAX_AGE_TICKS));
		}
	}

	public static void cleanup(long now) {
		if (PENDING.isEmpty() && RECENT_GOLEMS.isEmpty())
			return;
		Iterator<PendingCreation> iterator = PENDING.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().gameTime + MAX_AGE_TICKS < now)
				iterator.remove();
		}
		RECENT_GOLEMS.removeIf(recent -> recent.expiresAt < now);
	}

	public static boolean hasPending() {
		return !PENDING.isEmpty() || !RECENT_GOLEMS.isEmpty();
	}

	public static void clear() {
		PENDING.clear();
		RECENT_GOLEMS.clear();
	}

	private static RecentGolem findRecentMatch(ServerLevel level, PendingCreation pending) {
		long now = level.getGameTime();
		RecentGolem best = null;
		double bestDistance = MATCH_DISTANCE_SQR;
		for (RecentGolem recent : RECENT_GOLEMS) {
			if (!recent.dimension.equals(level.dimension()) || recent.expiresAt < now)
				continue;
			double distance = recent.pos.distSqr(pending.pos);
			if (distance <= bestDistance) {
				best = recent;
				bestDistance = distance;
			}
		}
		return best;
	}

	public enum ActionType {
		PLACEMENT,
		CARVING
	}

	private record PendingCreation(ResourceKey<Level> dimension, BlockPos pos, UUID playerId, long gameTime, ActionType actionType) {
	}

	private record RecentGolem(ResourceKey<Level> dimension, BlockPos pos, long expiresAt) {
	}
}
