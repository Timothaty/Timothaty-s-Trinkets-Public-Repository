package net.timothaty.timothatystrinkets.mechanics.active_ability;

import net.timothaty.timothatystrinkets.util.TimothatysTrinketsDebug;

import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;

public class ActiveAbilityKeyHandler {
	public static void handle(Player entity) {
		if (entity == null || entity.level().isClientSide()) {
			return;
		}
		TimothatysTrinketsDebug.insatiable(entity, "Active Ability key handler fired", ChatFormatting.GRAY);
		TimothatysTrinketsActiveAbilityHandler.activate(entity);
	}
}
