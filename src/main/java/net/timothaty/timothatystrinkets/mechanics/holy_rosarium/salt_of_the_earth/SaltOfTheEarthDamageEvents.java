package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.salt_of_the_earth;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.api.damage.HolyDamageApi;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumBead;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumHelper;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class SaltOfTheEarthDamageEvents {
	private SaltOfTheEarthDamageEvents() {
	}

	@SubscribeEvent
	public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
		if (event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F)
			return;
		if (!event.getSource().is(HolyDamageApi.HOLY_DAMAGE_TAG))
			return;

		Entity causingEntity = event.getSource().getEntity();
		if (!(causingEntity instanceof Player player)
				|| !player.isAlive()
				|| player.isRemoved()
				|| player.getHealth() <= 0.0F)
			return;
		if (!HolyRosariumHelper.hasActiveCombination(
				player,
				HolyRosariumBead.HUMILITY,
				HolyRosariumBead.RESURRECTION
		))
			return;

		event.setAmount(SaltOfTheEarth.modifyHolyDamage(player, event.getAmount()));
	}
}
