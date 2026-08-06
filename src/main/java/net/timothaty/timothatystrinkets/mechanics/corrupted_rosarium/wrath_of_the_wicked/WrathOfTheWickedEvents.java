package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.wrath_of_the_wicked;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import top.theillusivec4.curios.api.event.CurioChangeEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class WrathOfTheWickedEvents {
	private WrathOfTheWickedEvents() {
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (event.getEntity() instanceof ServerPlayer player)
			WrathOfTheWickedState.tick(player);
	}

	@SubscribeEvent
	public static void onControlEffectAdded(MobEffectEvent.Added event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;

		MobEffectInstance effect = event.getEffectInstance();
		if (effect != null
				&& (effect.is(TimothatysTrinketsModMobEffects.STUNNED)
						|| effect.is(TimothatysTrinketsModMobEffects.STAGGER))
				&& WrathOfTheWickedState.isActive(player)) {
			WrathOfTheWickedState.interrupt(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			WrathOfTheWickedState.finish(player, false);
	}

	@SubscribeEvent
	public static void onCurioChanged(CurioChangeEvent event) {
		if (event.getEntity() instanceof ServerPlayer player
				&& WrathOfTheWickedState.matchesSourceSlot(
						player,
						event.getIdentifier(),
						event.getSlotIndex()
				)) {
			WrathOfTheWickedState.finish(player, player.isAlive() && !player.isDeadOrDying());
		}
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			WrathOfTheWickedState.finish(player, player.isAlive() && !player.isDeadOrDying());
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			WrathOfTheWickedState.finish(player, player.isAlive() && !player.isDeadOrDying());
	}

	@SubscribeEvent
	public static void onStartTracking(PlayerEvent.StartTracking event) {
		if (event.getEntity() instanceof ServerPlayer trackingPlayer
				&& event.getTarget() instanceof ServerPlayer targetPlayer) {
			WrathOfTheWickedState.syncTo(trackingPlayer, targetPlayer);
		}
	}

	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		WrathOfTheWickedState.stopAll(event.getServer().getPlayerList().getPlayers());
	}
}
