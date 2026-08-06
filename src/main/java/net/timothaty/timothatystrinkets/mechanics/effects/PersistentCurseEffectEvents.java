package net.timothaty.timothatystrinkets.mechanics.effects;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class PersistentCurseEffectEvents {
	private PersistentCurseEffectEvents() {
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onMobEffectRemove(MobEffectEvent.Remove event) {
		if (event.getCure() == null) {
			return;
		}

		if (event.getEffect().value() == TimothatysTrinketsModMobEffects.ALTARS_CURSE.get()
				|| event.getEffect().value() == TimothatysTrinketsModMobEffects.BLOODSTAINED.get()) {
			event.setCanceled(true);
		}
	}
}
