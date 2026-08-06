package net.timothaty.timothatystrinkets.mechanics.cleric_quests;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class ClericQuestActionBarScheduler {
	private static final int DURATION_TICKS = 300;
	private static final int RESEND_INTERVAL_TICKS = 20;
	private static final Map<UUID, Entry> ACTIVE = new HashMap<>();

	private ClericQuestActionBarScheduler() {
	}

	public static void show(ServerPlayer player, Component message) {
		long now = player.serverLevel().getGameTime();
		player.displayClientMessage(message, true);
		long nextAlignedSend = (Math.floorDiv(now, RESEND_INTERVAL_TICKS) + 1L) * RESEND_INTERVAL_TICKS;
		ACTIVE.put(player.getUUID(), new Entry(message, now + DURATION_TICKS, nextAlignedSend));
	}

	public static void tick(MinecraftServer server, long now) {
		if (ACTIVE.isEmpty())
			return;
		Iterator<Map.Entry<UUID, Entry>> iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Entry> mapEntry = iterator.next();
			Entry entry = mapEntry.getValue();
			if (now >= entry.expiresAt) {
				iterator.remove();
				continue;
			}
			ServerPlayer player = server.getPlayerList().getPlayer(mapEntry.getKey());
			if (player == null)
				continue;
			if (now >= entry.nextSendAt) {
				player.displayClientMessage(entry.message, true);
				entry.nextSendAt = now + RESEND_INTERVAL_TICKS;
			}
		}
	}

	public static boolean hasActiveMessages() {
		return !ACTIVE.isEmpty();
	}

	public static void clear() {
		ACTIVE.clear();
	}

	private static final class Entry {
		private final Component message;
		private final long expiresAt;
		private long nextSendAt;

		private Entry(Component message, long expiresAt, long nextSendAt) {
			this.message = message;
			this.expiresAt = expiresAt;
			this.nextSendAt = nextSendAt;
		}
	}
}
