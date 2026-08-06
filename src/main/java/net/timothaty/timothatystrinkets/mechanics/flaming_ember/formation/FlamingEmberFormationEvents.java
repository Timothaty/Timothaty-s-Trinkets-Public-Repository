package net.timothaty.timothatystrinkets.mechanics.flaming_ember.formation;

import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class FlamingEmberFormationEvents {
	private FlamingEmberFormationEvents() {
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (event.getEntity() instanceof ServerPlayer player)
			FlamingEmberFormationProcess.tick(player);
	}

	@SubscribeEvent
	public static void onPlayerDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			FlamingEmberFormationProcess.clearAll(player);
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			FlamingEmberFormationProcess.clearAll(player);
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			FlamingEmberFormationProcess.clearAll(player);
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			FlamingEmberFormationProcess.clearAll(player);
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		if (event.getEntity() instanceof ServerPlayer player)
			FlamingEmberFormationProcess.clearAll(player);
	}

	@SubscribeEvent
	public static void onItemToss(ItemTossEvent event) {
		if (event.getPlayer() instanceof ServerPlayer player)
			FlamingEmberFormationProcess.handleToss(player, event.getEntity().getItem());
	}
}
