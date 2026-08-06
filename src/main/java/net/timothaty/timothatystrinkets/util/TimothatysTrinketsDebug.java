package net.timothaty.timothatystrinkets.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class TimothatysTrinketsDebug {
	private TimothatysTrinketsDebug() {}

	public static final boolean DEBUG_MESSAGES = true;

	public static final boolean INSATIABLE_DEBUG = DEBUG_MESSAGES;
	public static final boolean DAMNATION_ALTAR_DEBUG = DEBUG_MESSAGES;
	public static final boolean FARMERS_RING_DEBUG = DEBUG_MESSAGES;
	public static final boolean BLIGHT_SPAWN_DEBUG = DEBUG_MESSAGES;
	public static final boolean DUELISTS_GAUNTLET_DEBUG = DEBUG_MESSAGES;

	public static void insatiable(Player player, String message, ChatFormatting color) {
		playerMessage(INSATIABLE_DEBUG, player, "[TT Insatiable] " + message, color, false);
	}

	public static void send(Player player, String category, String message, ChatFormatting color) {
		String prefix = category == null || category.isEmpty() ? "[TT] " : "[TT " + category + "] ";
		playerMessage(DEBUG_MESSAGES, player, prefix + message, color, false);
	}

	public static void altar(Player player, String message) {
		playerMessage(DAMNATION_ALTAR_DEBUG, player, "[Altar] " + message, ChatFormatting.DARK_PURPLE, true);
	}

	public static void farmersRingLog(String message) {
		log(FARMERS_RING_DEBUG, "[FarmerRing] " + message);
	}

	private static void playerMessage(boolean enabled, Player player, String message, ChatFormatting color, boolean actionBar) {
		if (!enabled || player == null)
			return;
		player.displayClientMessage(Component.literal(message).withStyle(color), actionBar);
	}

	private static void log(boolean enabled, String message) {
		if (!enabled)
			return;
		System.out.println(message);
	}
}
