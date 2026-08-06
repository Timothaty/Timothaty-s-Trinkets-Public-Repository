package net.timothaty.timothatystrinkets.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class DrumsOfHasteCurios {
	private DrumsOfHasteCurios() {
	}

	public static boolean isDrumsEquipped(Player player) {
		return TimothatysCuriosHelper.hasCurio(player, DrumsOfHasteData.DRUMS_ID);
	}

	public static ItemStack getEquippedDrumsStack(Player player) {
		return TimothatysCuriosHelper.findCurio(player, DrumsOfHasteData.DRUMS_ID);
	}

	public static boolean isDrumsStack(ItemStack stack) {
		return TimothatysCuriosHelper.isStackOf(stack, DrumsOfHasteData.DRUMS_ID);
	}
}
