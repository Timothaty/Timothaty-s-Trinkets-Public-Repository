package net.timothaty.timothatystrinkets.mechanics.cleric_quests;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility.HumilityGolemCreationTracker;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility.HumilityGolemRepairTracker;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.sacrament.SacramentStage;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class ClericQuestLifecycleEvents {
	private ClericQuestLifecycleEvents() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDeath(LivingDeathEvent event) {
		if (!(event.getEntity().level() instanceof ServerLevel level))
			return;
		if (event.getEntity() instanceof ServerPlayer player) {
			onPlayerDeath(level, player);
			return;
		}
		if (event.getEntity() instanceof Villager cleric
				&& !cleric.isBaby()
				&& cleric.getVillagerData().getProfession() == VillagerProfession.CLERIC)
			onClericDeath(level, cleric);
	}

	private static void onPlayerDeath(ServerLevel level, ServerPlayer player) {
		ClericQuestSavedData data = ClericQuestSavedData.get(level);
		ClericQuestProgress progress = data.get(player.getUUID());
		if (progress == null)
			return;
		SacramentStage stage = progress.sacramentStage();
		if (stage == SacramentStage.HUNT_ACTIVE
				|| stage == SacramentStage.HUNT_RETURN
				|| stage == SacramentStage.FAST_ACTIVE
				|| stage == SacramentStage.FAST_RETURN) {
			progress.setSacramentStage(SacramentStage.RESTART_REQUIRED);
			progress.setSacramentKilledMask(0);
			progress.setFastingSeconds(0);
			progress.setFastingHasStarted(false);
			progress.setDesertExitGraceSeconds(0);
			data.changed(player.getUUID());
			player.displayClientMessage(Component.translatable("message.timothatys_trinkets.cleric_quest.sacrament.restart_required"), true);
		}
		ClericQuestCeremonyService.cancelForPlayer(level, player.getUUID());
	}

	private static void onClericDeath(ServerLevel level, Villager cleric) {
		Set<UUID> players = ClericQuestRuntimeManager.activePlayersForCleric(cleric.getUUID());
		if (players.isEmpty())
			return;
		ClericQuestSavedData data = ClericQuestSavedData.get(level);
		for (UUID playerId : players) {
			ClericQuestProgress progress = data.get(playerId);
			if (progress == null)
				continue;
			if (cleric.getUUID().equals(progress.humilityClericId()))
				progress.resetIncompleteHumility();
			if (cleric.getUUID().equals(progress.sacramentClericId()))
				progress.resetIncompleteSacrament();
			data.changed(playerId);
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
			if (player != null)
				player.displayClientMessage(Component.translatable("message.timothatys_trinkets.cleric_quest.cleric_dead"), true);
		}
		ClericQuestCeremonyService.cancelForCleric(level, cleric);
		VillagerGiftThrower.cancelForCleric(cleric.getUUID());
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		ClericQuestRuntimeManager.clear();
		HumilityGolemRepairTracker.clear();
		HumilityGolemCreationTracker.clear();
		net.timothaty.timothatystrinkets.mechanics.cleric_quests.display.ClericQuestRewardDisplayController.clear();
	}
}
