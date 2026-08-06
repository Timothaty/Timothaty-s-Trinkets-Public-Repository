package net.timothaty.timothatystrinkets.mechanics.active_ability;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class ActiveAbilityCastLock {
	private static final Map<UUID, LockEntry> LOCKS = new HashMap<>();

	private ActiveAbilityCastLock() {
	}

	public static void lock(ServerPlayer player, ResourceLocation sourceId, int durationTicks) {
		if (player == null || sourceId == null || durationTicks <= 0)
			return;
		LOCKS.put(player.getUUID(), new LockEntry(sourceId, player.level().getGameTime() + durationTicks));
	}

	public static void unlock(ServerPlayer player, ResourceLocation sourceId) {
		if (player == null || sourceId == null)
			return;
		LockEntry entry = LOCKS.get(player.getUUID());
		if (entry != null && entry.sourceId.equals(sourceId))
			LOCKS.remove(player.getUUID());
	}

	public static boolean isLocked(Player player) {
		if (player == null)
			return false;
		LockEntry entry = LOCKS.get(player.getUUID());
		if (entry == null)
			return false;
		if (!player.isAlive() || player.isDeadOrDying() || player.isRemoved()
				|| player.level().getGameTime() >= entry.endGameTime) {
			LOCKS.remove(player.getUUID());
			return false;
		}
		return true;
	}

	public static void clear(Player player) {
		if (player != null)
			LOCKS.remove(player.getUUID());
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Pre event) {
		if (!event.getEntity().level().isClientSide())
			isLocked(event.getEntity());
	}

	@SubscribeEvent
	public static void onPlayerDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof Player player)
			clear(player);
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		clear(event.getEntity());
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		clear(event.getOriginal());
		clear(event.getEntity());
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		LOCKS.clear();
	}

	private record LockEntry(ResourceLocation sourceId, long endGameTime) {
	}
}
