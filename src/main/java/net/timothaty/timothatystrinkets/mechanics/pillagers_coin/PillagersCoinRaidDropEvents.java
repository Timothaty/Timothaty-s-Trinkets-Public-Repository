package net.timothaty.timothatystrinkets.mechanics.pillagers_coin;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class PillagersCoinRaidDropEvents {
	private static final Map<UUID, PendingKill> PENDING_KILLS = new HashMap<>();

	private PillagersCoinRaidDropEvents() {
	}

	@SubscribeEvent
	public static void onRaiderDamaged(LivingDamageEvent.Post event) {
		if (!(event.getEntity() instanceof Raider raider)
				|| !(event.getSource().getEntity() instanceof ServerPlayer player)
				|| event.getSource().getDirectEntity() != player
				|| event.getNewDamage() <= 0.0F
				|| raider.getHealth() > 0.0F
				|| !raider.hasActiveRaid())
			return;
		PENDING_KILLS.put(raider.getUUID(), new PendingKill(player.getUUID(), raider.level().getGameTime()));
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRaiderDeath(LivingDeathEvent event) {
		if (!(event.getEntity() instanceof Raider raider))
			return;
		PendingKill pending = PENDING_KILLS.remove(raider.getUUID());
		if (event.isCanceled()
				|| pending == null
				|| !(event.getSource().getEntity() instanceof ServerPlayer player)
				|| event.getSource().getDirectEntity() != player
				|| !pending.playerId.equals(player.getUUID())
				|| pending.gameTime != raider.level().getGameTime())
			return;
		if (raider.getRandom().nextDouble() < PillagersCoinData.RAID_DROP_CHANCE)
			raider.spawnAtLocation(new ItemStack(TimothatysTrinketsModItems.PILLAGERS_COIN.get()));
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		PENDING_KILLS.clear();
	}

	@SubscribeEvent
	public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
		PENDING_KILLS.remove(event.getEntity().getUUID());
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		PENDING_KILLS.clear();
	}

	private record PendingKill(UUID playerId, long gameTime) {
	}
}
