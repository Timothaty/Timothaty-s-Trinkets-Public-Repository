package net.timothaty.timothatystrinkets.util;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumHelper;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class VampiricFangsCurios {
	private VampiricFangsCurios() {
	}

	public static final ResourceLocation FANGS_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "fangs");

	public static ItemStack getEquippedFangs(Player player) {
		ItemStack stack = TimothatysCuriosHelper.findCurio(player, FANGS_ID);
		return HolyRosariumHelper.isUnholyRelicSuppressed(player, stack) ? ItemStack.EMPTY : stack;
	}

	public static boolean hasEquippedFangs(Player player) {
		return !getEquippedFangs(player).isEmpty();
	}

	public static boolean isFangs(ItemStack stack) {
		return TimothatysCuriosHelper.isStackOf(stack, FANGS_ID);
	}
}
