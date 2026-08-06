package net.timothaty.timothatystrinkets.mechanics.cleric_quests.sacrament;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class SacramentFastingEvents {
	private SacramentFastingEvents() {
	}

	@SubscribeEvent
	public static void onItemUseFinished(LivingEntityUseItemEvent.Finish event) {
		if (event.getEntity() instanceof ServerPlayer player && event.getItem().has(DataComponents.FOOD))
			SacramentQuestService.breakFastWithFood(player);
	}
}
