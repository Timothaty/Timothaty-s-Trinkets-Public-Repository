package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.gorge;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class GorgeFoodUseEvents {
	private GorgeFoodUseEvents() {
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onFoodUseStart(LivingEntityUseItemEvent.Start event) {
		if (event.getEntity() instanceof Player player
				&& GorgeState.isAbilityActive(player)
				&& event.getItem().getFoodProperties(player) != null) {
			event.setCanceled(true);
		}
	}
}
