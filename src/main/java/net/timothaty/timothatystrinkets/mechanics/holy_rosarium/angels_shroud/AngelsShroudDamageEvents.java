package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.angels_shroud;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.healing.RelicHealingService;
import net.timothaty.timothatystrinkets.mechanics.healing.RelicHealingType;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumBead;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumHelper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class AngelsShroudDamageEvents {
	private AngelsShroudDamageEvents() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;

		float finalDamage = event.getNewDamage();
		if (finalDamage <= 0.0F || event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD))
			return;

		boolean shroudActiveBeforeHit = player.hasEffect(TimothatysTrinketsModMobEffects.ANGELS_SHROUD);
		if (shroudActiveBeforeHit) {
			event.setNewDamage(0.0F);
			RelicHealingService.heal(player, finalDamage, RelicHealingType.HOLY);
			return;
		}

		if (!player.isAlive() || player.isDeadOrDying() || player.isRemoved()
				|| player.isCreative() || player.isSpectator()) {
			return;
		}
		if (player.getCooldowns().isOnCooldown(TimothatysTrinketsModItems.HOLY_ROSARIUM.get()))
			return;
		if (!HolyRosariumHelper.hasActiveCombination(
				player,
				HolyRosariumBead.PENANCE,
				HolyRosariumBead.RESURRECTION
		)) {
			return;
		}

		float resultingHealth = player.getHealth() - finalDamage;
		float thresholdHealth = player.getMaxHealth() * AngelsShroudData.HEALTH_THRESHOLD_RATIO;
		if (resultingHealth > 0.0F && resultingHealth <= thresholdHealth)
			AngelsShroudActivation.tryActivate(player);
	}
}
