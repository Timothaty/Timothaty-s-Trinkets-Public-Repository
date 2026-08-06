package net.timothaty.timothatystrinkets.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class StrikerOfTheMorningStarCurios {
	private StrikerOfTheMorningStarCurios() {
	}

	public static boolean isStrikerEquipped(Player player) {
		return !getEquippedStrikerStack(player).isEmpty();
	}

	public static ItemStack getEquippedStrikerStack(Player player) {
		return TimothatysCuriosHelper.findCurio(player, StrikerOfTheMorningStarData.STRIKER_ID);
	}

	public static boolean isStrikerStack(ItemStack stack) {
		return TimothatysCuriosHelper.isStackOf(stack, StrikerOfTheMorningStarData.STRIKER_ID);
	}
}
