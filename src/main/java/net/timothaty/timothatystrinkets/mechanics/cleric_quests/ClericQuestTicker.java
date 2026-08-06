package net.timothaty.timothatystrinkets.mechanics.cleric_quests;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.display.ClericQuestRewardDisplayController;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility.HumilityGolemCreationTracker;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility.HumilityGolemRepairTracker;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.sacrament.SacramentQuestService;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.sacrament.SacramentTargetMarkerManager;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.UUID;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class ClericQuestTicker {
	private ClericQuestTicker() {
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		MinecraftServer server = event.getServer();
		if (!ClericQuestRuntimeManager.isInitialized())
			ClericQuestSavedData.get(server.overworld());
		if (HumilityGolemRepairTracker.hasPending())
			HumilityGolemRepairTracker.tick(server);
		if (ClericQuestCeremonyService.hasCeremonies())
			ClericQuestCeremonyService.tick(server);
		if (VillagerGiftThrower.hasSequences())
			VillagerGiftThrower.tick(server);

		long now = server.overworld().getGameTime();
		SacramentTargetMarkerManager.tick(server, now);
		if (HumilityGolemCreationTracker.hasPending())
			HumilityGolemCreationTracker.cleanup(now);
		if (Math.floorMod(now, 20L) != 0L)
			return;
		if (ClericQuestRuntimeManager.hasActiveFastingPlayers()) {
			for (UUID playerId : ClericQuestRuntimeManager.activeFastingPlayers()) {
				ServerPlayer player = server.getPlayerList().getPlayer(playerId);
				if (player != null)
					SacramentQuestService.tickFastingSecond(player);
			}
		}
		if (ClericQuestActionBarScheduler.hasActiveMessages())
			ClericQuestActionBarScheduler.tick(server, now);
		ClericQuestRewardDisplayController.tick(server);
	}
}
