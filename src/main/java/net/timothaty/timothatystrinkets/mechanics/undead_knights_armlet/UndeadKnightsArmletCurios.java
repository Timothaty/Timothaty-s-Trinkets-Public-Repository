package net.timothaty.timothatystrinkets.mechanics.undead_knights_armlet;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumHelper;
import net.timothaty.timothatystrinkets.util.CuriosBraceletSlotHelper;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class UndeadKnightsArmletCurios {
	private UndeadKnightsArmletCurios() {
	}

	public static boolean hasArmletEquipped(LivingEntity entity) {
		return !getArmlet(entity).isEmpty();
	}

	public static ItemStack getArmlet(LivingEntity entity) {
		ItemStack stack = CuriosBraceletSlotHelper.findEquippedStack(entity, TimothatysTrinketsModItems.UNDEAD_KNIGHTS_ARMLET.get());
		return HolyRosariumHelper.isUnholyRelicSuppressed(entity, stack) ? ItemStack.EMPTY : stack;
	}
}
