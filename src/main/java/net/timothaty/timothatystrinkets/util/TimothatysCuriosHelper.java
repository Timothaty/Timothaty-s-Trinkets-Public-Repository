package net.timothaty.timothatystrinkets.util;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.items.IItemHandler;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.function.Consumer;

public final class TimothatysCuriosHelper {
	private static final EntityCapability<IItemHandler, Void> CURIOS_INVENTORY =
			EntityCapability.createVoid(ResourceLocation.fromNamespaceAndPath("curios", "item_handler"), IItemHandler.class);

	private static final ResourceLocation VOID_SPHERE_ID = trinketId("void_sphere");
	private static final ResourceLocation ECHO_SPHERE_ID = trinketId("echo_sphere");
	private static final ResourceLocation FIRE_SPHERE_ID = trinketId("fire_sphere");
	private static final ResourceLocation VENOM_SPHERE_ID = trinketId("venom_sphere");

	private static final Set<ResourceLocation> EXCLUSIVE_SPHERE_IDS = Set.of(
			VOID_SPHERE_ID,
			ECHO_SPHERE_ID,
			FIRE_SPHERE_ID,
			VENOM_SPHERE_ID
	);

	private TimothatysCuriosHelper() {
	}

	public static boolean hasCurio(LivingEntity entity, ResourceLocation itemId) {
		return !findCurio(entity, itemId).isEmpty();
	}

	public static ItemStack findCurio(LivingEntity entity, ResourceLocation itemId) {
		if (entity == null || itemId == null)
			return ItemStack.EMPTY;

		IItemHandler curios = getCurios(entity);
		if (curios == null)
			return ItemStack.EMPTY;

		if (isExclusiveSphere(itemId)) {
			ItemStack activeSphere = findActiveExclusiveSphere(curios);
			return isStackOf(activeSphere, itemId) ? activeSphere : ItemStack.EMPTY;
		}

		for (int slot = 0; slot < curios.getSlots(); slot++) {
			ItemStack stack = curios.getStackInSlot(slot);
			if (isStackOf(stack, itemId))
				return stack;
		}

		return ItemStack.EMPTY;
	}

	public static boolean hasCurio(LivingEntity entity, Item item) {
		return !findCurio(entity, item).isEmpty();
	}

	public static ItemStack findCurio(LivingEntity entity, Item item) {
		if (entity == null || item == null)
			return ItemStack.EMPTY;

		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
		return findCurio(entity, itemId);
	}

	public static boolean hasAnyExclusiveSphere(LivingEntity entity) {
		return !findActiveExclusiveSphere(entity).isEmpty();
	}

	public static boolean isActiveExclusiveSphere(LivingEntity entity, ResourceLocation itemId) {
		if (itemId == null || !isExclusiveSphere(itemId))
			return false;
		return isStackOf(findActiveExclusiveSphere(entity), itemId);
	}

	public static boolean isActiveExclusiveSphere(LivingEntity entity, Item item) {
		if (item == null)
			return false;
		return isActiveExclusiveSphere(entity, BuiltInRegistries.ITEM.getKey(item));
	}

	public static ItemStack findActiveExclusiveSphere(LivingEntity entity) {
		if (entity == null)
			return ItemStack.EMPTY;

		IItemHandler curios = getCurios(entity);
		if (curios == null)
			return ItemStack.EMPTY;

		return findActiveExclusiveSphere(curios);
	}

	public static void forEachEquippedStack(LivingEntity entity, Consumer<ItemStack> consumer) {
		if (entity == null || consumer == null)
			return;

		IItemHandler curios = getCurios(entity);
		if (curios == null)
			return;

		for (int slot = 0; slot < curios.getSlots(); slot++) {
			ItemStack stack = curios.getStackInSlot(slot);
			if (!stack.isEmpty()) {
				consumer.accept(stack);
			}
		}
	}

	public static void removeCooldownsForEquippedItems(Player player) {
		if (player == null)
			return;

		forEachEquippedStack(player, stack -> player.getCooldowns().removeCooldown(stack.getItem()));
	}

	public static boolean isStackOf(ItemStack stack, ResourceLocation itemId) {
		if (stack == null || stack.isEmpty() || itemId == null)
			return false;

		return itemId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
	}

	public static boolean isStackOf(ItemStack stack, Item item) {
		if (stack == null || stack.isEmpty() || item == null)
			return false;

		return stack.getItem() == item;
	}

	public static boolean isExclusiveSphere(ResourceLocation itemId) {
		return itemId != null && EXCLUSIVE_SPHERE_IDS.contains(itemId);
	}

	private static ItemStack findActiveExclusiveSphere(IItemHandler curios) {
		for (int slot = 0; slot < curios.getSlots(); slot++) {
			ItemStack stack = curios.getStackInSlot(slot);
			if (isExclusiveSphereStack(stack))
				return stack;
		}
		return ItemStack.EMPTY;
	}

	private static boolean isExclusiveSphereStack(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return false;
		return isExclusiveSphere(BuiltInRegistries.ITEM.getKey(stack.getItem()));
	}

	private static ResourceLocation trinketId(String path) {
		return ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, path);
	}

	private static IItemHandler getCurios(LivingEntity entity) {
		return entity.getCapability(CURIOS_INVENTORY);
	}
}
