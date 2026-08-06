package net.timothaty.timothatystrinkets.util;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Optional;

public final class CuriosHandsSlotHelper {
	public static final String HANDS_SLOT_IDENTIFIER = "hands";

	private CuriosHandsSlotHelper() {
	}

	public static Optional<EquippedHandCurio> findEquipped(LivingEntity entity, Item item) {
		if (entity == null || item == null || !ModList.get().isLoaded("curios")) {
			return Optional.empty();
		}

		return CuriosApi.getCuriosInventory(entity).flatMap(curiosInventory -> {
			for (ICurioStacksHandler stacksHandler : curiosInventory.getCurios().values()) {
				if (!HANDS_SLOT_IDENTIFIER.equals(stacksHandler.getIdentifier())) {
					continue;
				}

				IDynamicStackHandler equippedStacks = stacksHandler.getStacks();
				int slots = Math.min(stacksHandler.getSlots(), equippedStacks.getSlots());
				for (int slot = 0; slot < slots; slot++) {
					ItemStack stack = equippedStacks.getStackInSlot(slot);
					if (stack.is(item)) {
						return Optional.of(new EquippedHandCurio(stack, slot));
					}
				}
			}

			return Optional.empty();
		});
	}

	public static ItemStack findEquippedStack(LivingEntity entity, Item item) {
		return findEquipped(entity, item).map(EquippedHandCurio::stack).orElse(ItemStack.EMPTY);
	}

	public static int findEquippedSlot(LivingEntity entity, Item item) {
		return findEquipped(entity, item).map(EquippedHandCurio::slotIndex).orElse(-1);
	}

	public static InteractionHand interactionHandForSlot(int slotIndex) {
		return Math.floorMod(slotIndex, 2) == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
	}

	public static HumanoidArm physicalArmForSlot(LivingEntity entity, int slotIndex) {
		InteractionHand hand = interactionHandForSlot(slotIndex);
		return hand == InteractionHand.MAIN_HAND ? entity.getMainArm() : entity.getMainArm().getOpposite();
	}

	public static boolean areItemsOnSamePhysicalArm(LivingEntity entity, Item first, Item second) {
		return areItemsEquippedOnArm(entity, HumanoidArm.RIGHT, first, second)
				|| areItemsEquippedOnArm(entity, HumanoidArm.LEFT, first, second);
	}

	public static boolean areItemsEquippedOnArm(LivingEntity entity, HumanoidArm arm, Item first, Item second) {
		return isItemEquippedOnArm(entity, first, arm) && isItemEquippedOnArm(entity, second, arm);
	}

	public static boolean isItemEquippedOnArm(LivingEntity entity, Item item, HumanoidArm arm) {
		if (entity == null || item == null || arm == null || !ModList.get().isLoaded("curios")) {
			return false;
		}

		return CuriosApi.getCuriosInventory(entity).map(curiosInventory -> {
			for (ICurioStacksHandler stacksHandler : curiosInventory.getCurios().values()) {
				if (!HANDS_SLOT_IDENTIFIER.equals(stacksHandler.getIdentifier())) {
					continue;
				}

				IDynamicStackHandler equippedStacks = stacksHandler.getStacks();
				int slots = Math.min(stacksHandler.getSlots(), equippedStacks.getSlots());
				for (int slot = 0; slot < slots; slot++) {
					if (physicalArmForSlot(entity, slot) == arm && equippedStacks.getStackInSlot(slot).is(item)) {
						return true;
					}
				}
			}

			return false;
		}).orElse(false);
	}

	public record EquippedHandCurio(ItemStack stack, int slotIndex) {
	}
}
