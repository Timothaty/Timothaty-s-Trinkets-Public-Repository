package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import top.theillusivec4.curios.api.event.CurioChangeEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class CorruptedRosariumStateEvents {
	private static final int SAFETY_RESYNC_INTERVAL_TICKS = 40;

	private CorruptedRosariumStateEvents() {
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Pre event) {
		Player player = event.getEntity();
		if (player == null || player.level().isClientSide())
			return;

		boolean refreshedDirtyState = CorruptedRosariumState.refreshIfDirty(player);
		if (!refreshedDirtyState
				&& Math.floorMod(player.tickCount + player.getId(), SAFETY_RESYNC_INTERVAL_TICKS) == 0)
			CorruptedRosariumState.refreshNow(player);
	}

	@SubscribeEvent
	public static void onCurioChanged(CurioChangeEvent event) {
		if (!(event.getEntity() instanceof Player player))
			return;
		if (!isRosarium(event.getFrom()) && !isRosarium(event.getTo()))
			return;

		CorruptedRosariumState.markDirty(player);
		CorruptedRosariumState.refreshNow(player);
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		initialize(event.getEntity());
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		CorruptedRosariumState.forget(event.getOriginal());
		initialize(event.getEntity());
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		initialize(event.getEntity());
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		CorruptedRosariumState.forget(event.getEntity());
	}

	@SubscribeEvent
	public static void onPlayerDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof Player player)
			CorruptedRosariumState.forget(player);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		CorruptedRosariumState.clearAll();
	}

	public static void onRosariumDataChanged(Player player) {
		if (player == null)
			return;
		CorruptedRosariumState.markDirty(player);
		CorruptedRosariumState.refreshNow(player);
	}

	private static void initialize(Player player) {
		if (player == null)
			return;
		CorruptedRosariumState.forget(player);
		CorruptedRosariumState.refreshNow(player);
	}

	private static boolean isRosarium(ItemStack stack) {
		return stack != null && stack.is(TimothatysTrinketsModItems.CORRUPTED_ROSARY.get());
	}
}
