package net.timothaty.timothatystrinkets.mechanics.cleric_quests.sacrament;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility.HumilityDeedEvents;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class SacramentHuntEvents {
	private SacramentHuntEvents() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDeath(LivingDeathEvent event) {
		ServerPlayer killer = HumilityDeedEvents.resolvePlayerKiller(event.getSource(), event.getEntity());
		if (killer == null)
			return;
		ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
		SacramentQuestService.recordHuntKill(killer, typeId);
	}
}
