package net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility;

import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestActionBarScheduler;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestCeremonyService;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestDialogue;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestEffects;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestProgress;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestRewardService;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestSavedData;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;

import java.util.UUID;

public final class HumilityQuestService {
	private HumilityQuestService() {
	}

	public static boolean begin(ServerPlayer player, Villager cleric) {
		ClericQuestSavedData data = ClericQuestSavedData.get(player.serverLevel());
		ClericQuestProgress progress = data.getOrCreate(player.getUUID());
		if (progress.humilityCompleted() || progress.humilityStage() != HumilityStage.NONE)
			return false;
		progress.beginHumility(cleric.getUUID(), player.serverLevel().dimension());
		data.changed(player.getUUID());
		ClericQuestDialogue.show(cleric, player, "dialogue.timothatys_trinkets.cleric.humility.started");
		return true;
	}

	public static boolean recordDeed(MinecraftServer server, UUID playerId, HumilityDeedType deed) {
		if (server == null || playerId == null || deed == null)
			return false;
		ClericQuestSavedData data = ClericQuestSavedData.get(server.overworld());
		ClericQuestProgress progress = data.get(playerId);
		if (progress == null || progress.humilityStage() != HumilityStage.DEEDS_ACTIVE || (progress.humilityDeedMask() & deed.bit()) != 0)
			return false;
		progress.setHumilityDeedMask(progress.humilityDeedMask() | deed.bit());
		int completed = Integer.bitCount(progress.humilityDeedMask());
		if (completed >= 3)
			progress.setHumilityStage(HumilityStage.REWARD_READY);
		data.changed(playerId);

		ServerPlayer player = server.getPlayerList().getPlayer(playerId);
		if (player != null) {
			ClericQuestEffects.playDeedAccomplished(player);
			ClericQuestEffects.confirmation(player);
			ClericQuestActionBarScheduler.show(player, Component.translatable("message.timothatys_trinkets.cleric_quest.humility.deed_progress", completed));
			if (completed >= 3)
				player.sendSystemMessage(Component.translatable("message.timothatys_trinkets.cleric_quest.humility.reward_ready"));
		}
		return true;
	}

	public static boolean beginRewardCeremony(ServerLevel level, Villager cleric, ServerPlayer player) {
		ClericQuestProgress progress = ClericQuestSavedData.get(level).get(player.getUUID());
		return progress != null
			&& progress.humilityStage() == HumilityStage.REWARD_READY
			&& isBoundTo(progress, level, cleric)
			&& ClericQuestCeremonyService.begin(level, cleric, player, ClericQuestCeremonyService.CeremonyKind.HUMILITY_REWARD);
	}

	public static void finishRewardCeremony(ServerLevel level, Villager cleric, ServerPlayer player) {
		ClericQuestSavedData data = ClericQuestSavedData.get(level);
		ClericQuestProgress progress = data.get(player.getUUID());
		if (progress == null || progress.humilityStage() != HumilityStage.REWARD_READY || !isBoundTo(progress, level, cleric))
			return;
		if (!ClericQuestRewardService.grantHumility(level, cleric, player))
			return;
		progress.completeHumility();
		data.changed(player.getUUID());
	}

	public static boolean isBoundTo(ClericQuestProgress progress, ServerLevel level, Villager cleric) {
		return progress.humilityClericId() != null
			&& progress.humilityClericId().equals(cleric.getUUID())
			&& progress.humilityClericDimension() != null
			&& progress.humilityClericDimension().equals(level.dimension());
	}
}
