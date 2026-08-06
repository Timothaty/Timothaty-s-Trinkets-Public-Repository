package net.timothaty.timothatystrinkets.mechanics.champions_gauntlet;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumHelper;
import net.timothaty.timothatystrinkets.util.CuriosHandsSlotHelper;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class ChampionsGauntletCurios {
	private ChampionsGauntletCurios() {
	}

	public static boolean hasGauntletEquipped(LivingEntity entity) {
		return !getGauntlet(entity).isEmpty();
	}

	public static ItemStack getGauntlet(LivingEntity entity) {
		ItemStack stack = CuriosHandsSlotHelper.findEquippedStack(entity, TimothatysTrinketsModItems.CHAMPIONS_GAUNTLET.get());
		return HolyRosariumHelper.isUnholyRelicSuppressed(entity, stack) ? ItemStack.EMPTY : stack;
	}
}
