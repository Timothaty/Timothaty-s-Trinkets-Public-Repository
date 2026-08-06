package net.timothaty.timothatystrinkets.mechanics.debtlord;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class DebtlordProgressionHandler {
	private static final String DEFEATED_KEY = "tt_debtlord_defeated";

	private DebtlordProgressionHandler() {
	}

	public static boolean hasDefeatedDebtlord(ServerPlayer player) {
		return player.getPersistentData().getBoolean(DEFEATED_KEY);
	}

	public static void markDebtlordDefeated(ServerPlayer player) {
		player.getPersistentData().putBoolean(DEFEATED_KEY, true);
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;
		if (!(event.getOriginal() instanceof ServerPlayer oldPlayer)) return;
		if (!oldPlayer.getPersistentData().contains(DEFEATED_KEY, Tag.TAG_BYTE)) return;

		newPlayer.getPersistentData().putBoolean(DEFEATED_KEY, oldPlayer.getPersistentData().getBoolean(DEFEATED_KEY));
	}
}
