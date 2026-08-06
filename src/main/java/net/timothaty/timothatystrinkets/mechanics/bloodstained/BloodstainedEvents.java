package net.timothaty.timothatystrinkets.mechanics.bloodstained;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.npc.Villager;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class BloodstainedEvents {
	private BloodstainedEvents() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onVillagerKilled(LivingDeathEvent event) {
		if (event.isCanceled()
				|| !(event.getEntity() instanceof Villager villager)
				|| villager.isBaby()
				|| !(villager.level() instanceof ServerLevel)
				|| !(event.getSource().getEntity() instanceof ServerPlayer player)
				|| event.getSource().getDirectEntity() != player
				|| !player.getMainHandItem().is(BloodstainedData.BLOODSTAINING_WEAPONS))
			return;
		BloodstainedHelper.applyOrRefresh(player);
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player)
				|| Math.floorMod(player.tickCount, BloodstainedData.WASH_CHECK_INTERVAL_TICKS) != 0)
			return;
		MobEffectInstance effect = player.getEffect(TimothatysTrinketsModMobEffects.BLOODSTAINED);
		if (effect == null)
			return;

		int reduction = 0;
		if (player.isInWater())
			reduction = BloodstainedData.WATER_DURATION_REDUCTION;
		else if (player.serverLevel().isRainingAt(player.blockPosition()))
			reduction = BloodstainedData.RAIN_DURATION_REDUCTION;
		if (reduction > 0)
			BloodstainedHelper.replaceDuration(player, effect, effect.getDuration() - reduction);
	}
}
