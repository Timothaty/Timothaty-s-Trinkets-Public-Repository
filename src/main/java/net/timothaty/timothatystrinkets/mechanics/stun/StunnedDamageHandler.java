package net.timothaty.timothatystrinkets.mechanics.stun;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class StunnedDamageHandler {
	private StunnedDamageHandler() {
	}


	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
		if (event == null || event.getSource() == null)
			return;

		Entity attackerEntity = event.getSource().getEntity();
		if (attackerEntity instanceof LivingEntity attacker && TimothatysTrinketsStunHelper.isStunned(attacker)) {
			event.setNewDamage(0.0F);
			return;
		}

		LivingEntity victim = event.getEntity();
		if (victim != null && TimothatysTrinketsStunHelper.isStaggered(victim)) {
			event.setNewDamage(event.getNewDamage() * 1.05F);
		}
	}
}
