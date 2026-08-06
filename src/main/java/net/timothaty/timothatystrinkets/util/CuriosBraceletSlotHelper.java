package net.timothaty.timothatystrinkets.util;

import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Optional;

public final class CuriosBraceletSlotHelper {
	public static final String BRACELET_SLOT_IDENTIFIER = "bracelet";

	private CuriosBraceletSlotHelper() {
	}

	public static Optional<EquippedBraceletCurio> findEquipped(LivingEntity entity, Item item) {
		if (entity == null || item == null || !ModList.get().isLoaded("curios")) {
			return Optional.empty();
		}

		return CuriosApi.getCuriosInventory(entity).flatMap(curiosInventory -> {
			for (ICurioStacksHandler stacksHandler : curiosInventory.getCurios().values()) {
				if (!BRACELET_SLOT_IDENTIFIER.equals(stacksHandler.getIdentifier())) {
					continue;
				}

				IDynamicStackHandler equippedStacks = stacksHandler.getStacks();
				int slots = Math.min(stacksHandler.getSlots(), equippedStacks.getSlots());
				for (int slot = 0; slot < slots; slot++) {
					ItemStack stack = equippedStacks.getStackInSlot(slot);
					if (stack.is(item)) {
						return Optional.of(new EquippedBraceletCurio(stack, slot));
					}
				}
			}

			return Optional.empty();
		});
	}

	public static ItemStack findEquippedStack(LivingEntity entity, Item item) {
		return findEquipped(entity, item).map(EquippedBraceletCurio::stack).orElse(ItemStack.EMPTY);
	}

	public static boolean isItemEquippedOnArm(LivingEntity entity, Item item, HumanoidArm arm) {
		if (entity == null || item == null || arm == null || !ModList.get().isLoaded("curios")) {
			return false;
		}

		return CuriosApi.getCuriosInventory(entity).map(curiosInventory -> {
			for (ICurioStacksHandler stacksHandler : curiosInventory.getCurios().values()) {
				if (!BRACELET_SLOT_IDENTIFIER.equals(stacksHandler.getIdentifier())) {
					continue;
				}

				IDynamicStackHandler equippedStacks = stacksHandler.getStacks();
				int slots = Math.min(stacksHandler.getSlots(), equippedStacks.getSlots());
				for (int slot = 0; slot < slots; slot++) {
					if (CuriosHandsSlotHelper.physicalArmForSlot(entity, slot) == arm && equippedStacks.getStackInSlot(slot).is(item)) {
						return true;
					}
				}
			}

			return false;
		}).orElse(false);
	}

	public record EquippedBraceletCurio(ItemStack stack, int slotIndex) {
	}
}
