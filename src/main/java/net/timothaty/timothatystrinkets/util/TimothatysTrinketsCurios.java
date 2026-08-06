package net.timothaty.timothatystrinkets.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

@Deprecated(forRemoval = false)
public final class TimothatysTrinketsCurios {
	private TimothatysTrinketsCurios() {
	}

	public static void forEachEquippedStack(Player player, Consumer<ItemStack> consumer) {
		TimothatysCuriosHelper.forEachEquippedStack(player, consumer);
	}

	public static void removeCooldownsForEquippedItems(Player player) {
		TimothatysCuriosHelper.removeCooldownsForEquippedItems(player);
	}
}
