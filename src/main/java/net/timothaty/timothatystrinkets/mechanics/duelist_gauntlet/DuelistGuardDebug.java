package net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsDebug;

public final class DuelistGuardDebug {
	private DuelistGuardDebug() {
	}

	public static void show(Player player, String message, ChatFormatting color) {
		if (TimothatysTrinketsDebug.DUELISTS_GAUNTLET_DEBUG && player != null) {
			player.displayClientMessage(Component.literal("[Duelist] " + message).withStyle(color), true);
		}
	}
}
