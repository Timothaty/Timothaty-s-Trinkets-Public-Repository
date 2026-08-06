package net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.util.TimothatysCuriosHelper;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class DuelistGauntletCurios {
	private DuelistGauntletCurios() {
	}

	public static boolean hasGauntletEquipped(LivingEntity entity) {
		return !getGauntlet(entity).isEmpty();
	}

	public static ItemStack getGauntlet(LivingEntity entity) {
		return TimothatysCuriosHelper.findCurio(entity, TimothatysTrinketsModItems.DUELISTS_GAUNTLET.get());
	}
}
