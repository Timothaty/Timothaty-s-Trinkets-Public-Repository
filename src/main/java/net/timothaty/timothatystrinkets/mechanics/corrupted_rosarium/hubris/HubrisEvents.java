package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.fire.CustomSweepVisualState;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.SweepAttackEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import top.theillusivec4.curios.api.event.CurioChangeEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class HubrisEvents {
	private HubrisEvents() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onPlayerTick(PlayerTickEvent.Pre event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		HubrisActivationState.tick(player);
		HubrisState.tick(player);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
		HubrisStrikeResolver.amplifyIncomingDamage(event);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDamagePost(LivingDamageEvent.Post event) {
		HubrisStrikeResolver.markDamageApplied(event);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
	public static void onSweepAttack(SweepAttackEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			HubrisStrikeResolver.markActualSweep(player, event.getTarget(), event.isSweeping());
	}

	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
	public static void onShieldBlock(LivingShieldBlockEvent event) {
		if (event.isCanceled() || !event.getBlocked() || event.getBlockedDamage() <= 0.0F)
			return;
		HubrisStrikeResolver.markDefended(
				event.getEntity(),
				event.getDamageSource(),
				HubrisStrikeResolver.DefenseKind.VANILLA_SHIELD
		);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onEffectApplicable(MobEffectEvent.Applicable event) {
		MobEffectInstance instance = event.getEffectInstance();
		if (instance != null
				&& instance.getEffect().value() == TimothatysTrinketsModMobEffects.HUBRIS.get()
				&& !(event.getEntity() instanceof Player))
			event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onCurioChanged(CurioChangeEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		if (HubrisActivationState.matchesSourceSlot(player, event.getIdentifier(), event.getSlotIndex()))
			HubrisActivationState.cancel(player);
		if (HubrisState.matchesSourceSlot(player, event.getIdentifier(), event.getSlotIndex()))
			HubrisState.end(player, HubrisEndReason.DISPELLED, true);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onHubrisRemoved(MobEffectEvent.Remove event) {
		if (event.isCanceled()
				|| !(event.getEntity() instanceof ServerPlayer player)
				|| event.getEffect().value() != TimothatysTrinketsModMobEffects.HUBRIS.get()
				|| !HubrisState.hasActiveSession(player))
			return;
		HubrisState.end(player, HubrisEndReason.DISPELLED, false);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onHubrisExpired(MobEffectEvent.Expired event) {
		if (event.isCanceled()
				|| !(event.getEntity() instanceof ServerPlayer player)
				|| !isHubris(event.getEffectInstance())
				|| !HubrisState.hasActiveSession(player))
			return;
		HubrisState.end(player, HubrisEndReason.EXPIRED, false);
	}

	@SubscribeEvent
	public static void onPlayerDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			clearPlayer(player, HubrisEndReason.DEATH);
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			clearPlayer(player, HubrisEndReason.LOGOUT);
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			clearPlayer(player, HubrisEndReason.DIMENSION_CHANGE);
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		if (event.getOriginal() instanceof ServerPlayer original)
			clearPlayer(original, HubrisEndReason.CLONE);
		if (event.getEntity() instanceof ServerPlayer player)
			clearPlayer(player, HubrisEndReason.CLONE);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		HubrisActivationState.clearAll();
		HubrisState.clearAll(event.getServer());
		CustomSweepVisualState.clearAll();
	}

	private static void clearPlayer(ServerPlayer player, HubrisEndReason reason) {
		HubrisActivationState.cancel(player);
		HubrisState.end(player, reason, true);
		HubrisStrikeResolver.clear(player);
		CustomSweepVisualState.clear(player);
	}

	private static boolean isHubris(MobEffectInstance instance) {
		return instance != null && instance.getEffect().value() == TimothatysTrinketsModMobEffects.HUBRIS.get();
	}
}
