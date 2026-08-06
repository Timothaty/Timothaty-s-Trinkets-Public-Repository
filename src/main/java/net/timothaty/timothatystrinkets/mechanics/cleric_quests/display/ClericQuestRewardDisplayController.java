package net.timothaty.timothatystrinkets.mechanics.cleric_quests.display;

import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaVillagerBlessingState;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaVillagerFearEvents;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestProgress;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestRuntimeManager;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestSavedData;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility.HumilityStage;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.sacrament.SacramentStage;
import net.timothaty.timothatystrinkets.mechanics.pillagers_coin.PillagersCoinExtortionManager;
import net.timothaty.timothatystrinkets.mechanics.pillagers_coin.PillagersCoinVillagerFearData;
import net.timothaty.timothatystrinkets.mechanics.pillagers_coin.PillagersCoinVillagerFearEvents;
import net.timothaty.timothatystrinkets.mechanics.pillagers_coin.PillagersCoinVillagerRuntimeState;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClericQuestRewardDisplayController {
	private static final double DISPLAY_DISTANCE_SQR = 16.0D;
	private static final Map<UUID, Displayed> DISPLAYED = new HashMap<>();

	private ClericQuestRewardDisplayController() {
	}

	public static void tick(MinecraftServer server) {
		Map<UUID, Candidate> desired = new HashMap<>();
		ClericQuestSavedData data = ClericQuestSavedData.get(server.overworld());
		for (UUID playerId : ClericQuestRuntimeManager.rewardReadyPlayers()) {
			ServerPlayer player = server.getPlayerList().getPlayer(playerId);
			ClericQuestProgress progress = data.get(playerId);
			if (player == null || progress == null)
				continue;
			byte displayType = progress.humilityStage() == HumilityStage.REWARD_READY
					? ClericQuestRewardDisplayState.HUMILITY
					: progress.sacramentStage() == SacramentStage.FAST_RETURN
						? ClericQuestRewardDisplayState.SACRAMENT
						: ClericQuestRewardDisplayState.NONE;
			if (displayType == ClericQuestRewardDisplayState.NONE)
				continue;
			UUID clericId = progress.activeClericId();
			ResourceKey<Level> dimension = progress.activeClericDimension();
			Villager cleric = ClericQuestRuntimeManager.getLoadedCleric(server, clericId, dimension);
			if (cleric == null || player.serverLevel() != cleric.level() || player.distanceToSqr(cleric) > DISPLAY_DISTANCE_SQR || isSuppressed(cleric, playerId))
				continue;
			double distance = player.distanceToSqr(cleric);
			Candidate previous = desired.get(clericId);
			if (previous == null || distance < previous.distance || (distance == previous.distance && compareUuid(playerId, previous.playerId) < 0))
				desired.put(clericId, new Candidate(playerId, dimension, displayType, distance));
		}

		for (Map.Entry<UUID, Displayed> entry : Map.copyOf(DISPLAYED).entrySet()) {
			if (!desired.containsKey(entry.getKey())) {
				setDisplay(server, entry.getKey(), entry.getValue().dimension, ClericQuestRewardDisplayState.NONE);
				DISPLAYED.remove(entry.getKey());
			}
		}
		for (Map.Entry<UUID, Candidate> entry : desired.entrySet()) {
			Displayed previous = DISPLAYED.get(entry.getKey());
			Candidate candidate = entry.getValue();
			if (previous == null || previous.type != candidate.type || !previous.dimension.equals(candidate.dimension))
				setDisplay(server, entry.getKey(), candidate.dimension, candidate.type);
			DISPLAYED.put(entry.getKey(), new Displayed(candidate.dimension, candidate.type));
		}
	}

	public static void hide(Villager villager) {
		if (villager instanceof ClericQuestRewardDisplayState state)
			state.timothatys_trinkets$setClericQuestRewardDisplay(ClericQuestRewardDisplayState.NONE);
		DISPLAYED.remove(villager.getUUID());
	}

	public static void clear() {
		DISPLAYED.clear();
	}

	private static boolean isSuppressed(Villager cleric, UUID playerId) {
		long now = cleric.level().getGameTime();
		return PillagersCoinExtortionManager.hasSession(cleric)
			|| ClericQuestRuntimeManager.isClericBusy(cleric.getUUID())
			|| PillagersCoinVillagerFearData.fears(cleric, playerId)
			|| PillagersCoinVillagerFearEvents.shouldHideFromCoin(cleric, now)
			|| AnathemaVillagerFearEvents.shouldHideFromAnathema(cleric, now)
			|| cleric instanceof AnathemaVillagerBlessingState blessing && blessing.timothatys_trinkets$isBlessingsAnimationActive()
			|| cleric instanceof PillagersCoinVillagerRuntimeState runtime && (runtime.timothatys_trinkets$isExtortionVisualActive() || runtime.timothatys_trinkets$isFearVisualActive());
	}

	private static void setDisplay(MinecraftServer server, UUID clericId, ResourceKey<Level> dimension, byte type) {
		Villager cleric = ClericQuestRuntimeManager.getLoadedCleric(server, clericId, dimension);
		if (cleric instanceof ClericQuestRewardDisplayState state)
			state.timothatys_trinkets$setClericQuestRewardDisplay(type);
	}

	private static int compareUuid(UUID left, UUID right) {
		int most = Long.compareUnsigned(left.getMostSignificantBits(), right.getMostSignificantBits());
		return most != 0 ? most : Long.compareUnsigned(left.getLeastSignificantBits(), right.getLeastSignificantBits());
	}

	private record Candidate(UUID playerId, ResourceKey<Level> dimension, byte type, double distance) {
	}

	private record Displayed(ResourceKey<Level> dimension, byte type) {
	}
}
