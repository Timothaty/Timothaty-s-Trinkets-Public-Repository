package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.gorge;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumCombination;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumHelper;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumState;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import top.theillusivec4.curios.api.event.CurioChangeEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class GorgeEvents {
	private GorgeEvents() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onPlayerTick(PlayerTickEvent.Pre event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || !GorgeState.hasActiveSession(player))
			return;

		if (!player.isAlive()) {
			GorgeState.cancelWithoutPenalty(player, true);
			return;
		}
		if (!player.hasEffect(TimothatysTrinketsModMobEffects.GORGE)) {
			GorgeState.interruptWithPenalty(player, false);
			return;
		}
		if (!CorruptedRosariumHelper.hasActiveCombination(
				player,
				CorruptedRosariumCombination.GORGE
		) || !GorgeState.matchesRosariumRevision(
				player,
				CorruptedRosariumState.getRevision(player)
		)) {
			GorgeState.interruptWithPenalty(player, true);
			return;
		}

		GorgeState.tick(player);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onCurioChanged(CurioChangeEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)
				|| !GorgeState.matchesSourceSlot(
						player,
						event.getIdentifier(),
						event.getSlotIndex()
				))
			return;

		GorgeState.interruptWithPenalty(player, true);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onGorgeRemoved(MobEffectEvent.Remove event) {
		if (!(event.getEntity() instanceof ServerPlayer player)
				|| event.getEffect().value() != TimothatysTrinketsModMobEffects.GORGE.get())
			return;

		GorgeState.interruptWithDeferredPenalty(player);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onGorgeExpired(MobEffectEvent.Expired event) {
		if (!(event.getEntity() instanceof ServerPlayer player)
				|| !isGorge(event.getEffectInstance()))
			return;

		GorgeState.finishNormally(player);
	}

	@SubscribeEvent
	public static void onPlayerDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			GorgeState.cancelWithoutPenalty(player, true);
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			GorgeState.cancelWithoutPenalty(player, true);
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		GorgeState.cancelWithoutPenalty(event.getOriginal(), true);
		if (event.getEntity() instanceof ServerPlayer player)
			GorgeState.cancelWithoutPenalty(player, true);
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			GorgeState.cancelWithoutPenalty(player, true);
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			GorgeState.cancelWithoutPenalty(player, true);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		GorgeState.clearAll();
	}

	private static boolean isGorge(MobEffectInstance instance) {
		return instance != null
				&& instance.getEffect().value() == TimothatysTrinketsModMobEffects.GORGE.get();
	}
}
