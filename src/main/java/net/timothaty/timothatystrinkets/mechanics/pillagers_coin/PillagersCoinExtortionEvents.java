package net.timothaty.timothatystrinkets.mechanics.pillagers_coin;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestRuntimeManager;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.display.ClericQuestRewardDisplayController;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class PillagersCoinExtortionEvents {
	private PillagersCoinExtortionEvents() {
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onVillagerIncomingDamage(LivingIncomingDamageEvent event) {
		if (!(event.getEntity() instanceof Villager villager)
				|| !(villager.level() instanceof ServerLevel level)
				|| event.getAmount() <= 0.0F)
			return;

		if (PillagersCoinExtortionManager.hasSession(villager)) {
			PillagersCoinExtortionManager.endForIncomingDamage(level, villager, event.getSource());
			return;
		}
		if (villager.isBaby()
				|| !PillagersCoinHelper.isDirectPlayerMelee(event.getSource())
				|| !(event.getSource().getEntity() instanceof ServerPlayer robber)
				|| !PillagersCoinHelper.hasUsableCoin(robber)
				|| PillagersCoinVillagerFearData.fears(villager, robber.getUUID()))
			return;

		if (PillagersCoinExtortionManager.startAttempt(level, villager, robber)) {
			ClericQuestRuntimeManager.cancelCeremonyForCleric(level, villager);
			ClericQuestRewardDisplayController.hide(villager);
			event.setCanceled(true);
		}
	}
}
