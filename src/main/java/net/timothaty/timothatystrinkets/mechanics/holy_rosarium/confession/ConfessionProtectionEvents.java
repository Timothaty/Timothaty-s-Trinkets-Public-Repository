package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.confession;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumBead;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumHelper;
import net.timothaty.timothatystrinkets.mechanics.necromancer.NecromancerAllyHelper;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class ConfessionProtectionEvents {
	private ConfessionProtectionEvents() {
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
		LivingEntity target = event.getEntity();
		if (target.level().isClientSide()
				|| (!(target instanceof AbstractVillager) && !(target instanceof IronGolem)))
			return;

		LivingEntity responsibleAttacker = NecromancerAllyHelper.getLivingAttacker(event.getSource());
		if (responsibleAttacker instanceof Player player
				&& HolyRosariumHelper.hasActiveCombination(
						player,
						HolyRosariumBead.HUMILITY,
						HolyRosariumBead.SACRAMENT
				)) {
			event.setCanceled(true);
		}
	}
}
