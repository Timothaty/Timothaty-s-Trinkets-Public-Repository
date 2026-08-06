package net.timothaty.timothatystrinkets.mechanics.cleric_quests;

import net.timothaty.timothatystrinkets.mechanics.cleric_quests.sacrament.SacramentStage;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.sacrament.SacramentTargetMarkerManager;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ClericQuestRuntimeManager {
	private static final Map<UUID, Set<UUID>> ACTIVE_PLAYERS_BY_CLERIC = new HashMap<>();
	private static final Map<UUID, UUID> ACTIVE_CLERIC_BY_PLAYER = new HashMap<>();
	private static final Set<UUID> ACTIVE_FASTING_PLAYERS = new HashSet<>();
	private static final Set<UUID> ACTIVE_HUNTING_PLAYERS = new HashSet<>();
	private static final Set<UUID> REWARD_READY_PLAYERS = new HashSet<>();
	private static ClericQuestSavedData indexedData;

	private ClericQuestRuntimeManager() {
	}

	public static void ensureIndexed(ClericQuestSavedData data) {
		if (indexedData == data)
			return;
		indexedData = data;
		ACTIVE_PLAYERS_BY_CLERIC.clear();
		ACTIVE_CLERIC_BY_PLAYER.clear();
		ACTIVE_FASTING_PLAYERS.clear();
		ACTIVE_HUNTING_PLAYERS.clear();
		REWARD_READY_PLAYERS.clear();
		for (Map.Entry<UUID, ClericQuestProgress> entry : data.entries())
			updatePlayerIndex(entry.getKey(), entry.getValue());
	}

	public static boolean isInitialized() {
		return indexedData != null;
	}

	public static void updatePlayerIndex(UUID playerId, ClericQuestProgress progress) {
		removePlayerIndex(playerId);
		UUID clericId = progress.activeClericId();
		if (clericId != null) {
			ACTIVE_CLERIC_BY_PLAYER.put(playerId, clericId);
			ACTIVE_PLAYERS_BY_CLERIC.computeIfAbsent(clericId, ignored -> new HashSet<>()).add(playerId);
		}
		if (progress.sacramentStage() == SacramentStage.FAST_ACTIVE)
			ACTIVE_FASTING_PLAYERS.add(playerId);
		if (progress.sacramentStage() == SacramentStage.HUNT_ACTIVE)
			ACTIVE_HUNTING_PLAYERS.add(playerId);
		if (progress.humilityStage() == net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility.HumilityStage.REWARD_READY
				|| progress.sacramentStage() == SacramentStage.FAST_RETURN)
			REWARD_READY_PLAYERS.add(playerId);
	}

	public static void removePlayerIndex(UUID playerId) {
		UUID oldCleric = ACTIVE_CLERIC_BY_PLAYER.remove(playerId);
		if (oldCleric != null) {
			Set<UUID> players = ACTIVE_PLAYERS_BY_CLERIC.get(oldCleric);
			if (players != null) {
				players.remove(playerId);
				if (players.isEmpty())
					ACTIVE_PLAYERS_BY_CLERIC.remove(oldCleric);
			}
		}
		ACTIVE_FASTING_PLAYERS.remove(playerId);
		ACTIVE_HUNTING_PLAYERS.remove(playerId);
		REWARD_READY_PLAYERS.remove(playerId);
	}

	public static Set<UUID> activePlayersForCleric(UUID clericId) {
		Set<UUID> players = ACTIVE_PLAYERS_BY_CLERIC.get(clericId);
		return players == null ? Set.of() : Set.copyOf(players);
	}

	public static Set<UUID> activeFastingPlayers() {
		return Set.copyOf(ACTIVE_FASTING_PLAYERS);
	}

	public static Set<UUID> activeHuntingPlayers() {
		return Set.copyOf(ACTIVE_HUNTING_PLAYERS);
	}

	public static Set<UUID> rewardReadyPlayers() {
		return Set.copyOf(REWARD_READY_PLAYERS);
	}

	public static boolean hasActiveFastingPlayers() {
		return !ACTIVE_FASTING_PLAYERS.isEmpty();
	}

	public static boolean hasActiveHuntingPlayers() {
		return !ACTIVE_HUNTING_PLAYERS.isEmpty();
	}

	public static Villager getLoadedCleric(MinecraftServer server, UUID clericId, ResourceKey<Level> dimension) {
		if (server == null || clericId == null || dimension == null)
			return null;
		ServerLevel level = server.getLevel(dimension);
		if (level == null)
			return null;
		Entity entity = level.getEntity(clericId);
		return entity instanceof Villager villager ? villager : null;
	}

	public static boolean isClericBusy(UUID clericId) {
		return ClericQuestCeremonyService.hasCeremony(clericId) || VillagerGiftThrower.hasSequence(clericId);
	}

	public static void cancelCeremonyForCleric(UUID clericId) {
		ClericQuestCeremonyService.cancelForCleric(clericId);
	}

	public static void cancelCeremonyForCleric(ServerLevel level, Villager cleric) {
		ClericQuestCeremonyService.cancelForCleric(level, cleric);
	}

	public static void clear() {
		indexedData = null;
		ACTIVE_PLAYERS_BY_CLERIC.clear();
		ACTIVE_CLERIC_BY_PLAYER.clear();
		ACTIVE_FASTING_PLAYERS.clear();
		ACTIVE_HUNTING_PLAYERS.clear();
		REWARD_READY_PLAYERS.clear();
		ClericQuestCeremonyService.clear();
		ClericQuestActionBarScheduler.clear();
		VillagerGiftThrower.clear();
		SacramentTargetMarkerManager.clear();
	}
}
