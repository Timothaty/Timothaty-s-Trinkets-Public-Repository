package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public final class CorruptedRosariumHelper {
	private CorruptedRosariumHelper() {
	}

	public static ItemStack findActiveRosarium(Player player) {
		Optional<SlotResult> result = findActiveRosariumResult(player);
		return result.isPresent() ? result.get().stack() : ItemStack.EMPTY;
	}

	public static Optional<SlotResult> findActiveRosariumResult(Player player) {
		CorruptedRosariumState state = CorruptedRosariumState.get(player);
		if (state == null || !state.active())
			return Optional.empty();

		SlotResult result = resolveCachedRosarium(player, state);
		if (result != null)
			return Optional.of(result);

		CorruptedRosariumState.markDirty(player);
		CorruptedRosariumState.refreshNow(player);
		state = CorruptedRosariumState.get(player);
		if (state == null || !state.active())
			return Optional.empty();
		return Optional.ofNullable(resolveCachedRosarium(player, state));
	}

	public static boolean isActiveRosarium(Player player, SlotContext slotContext, ItemStack stack) {
		if (slotContext == null || stack == null || stack.isEmpty())
			return false;

		CorruptedRosariumState state = CorruptedRosariumState.get(player);
		return state != null
				&& state.active()
				&& state.matches(slotContext)
				&& stack.is(TimothatysTrinketsModItems.CORRUPTED_ROSARY.get());
	}

	public static Set<CorruptedRosariumBead> getActiveBeads(Player player) {
		CorruptedRosariumState state = CorruptedRosariumState.get(player);
		if (state == null || !state.active() || state.beadMask() == 0)
			return Collections.emptySet();

		EnumSet<CorruptedRosariumBead> beads = EnumSet.noneOf(CorruptedRosariumBead.class);
		for (CorruptedRosariumBead bead : CorruptedRosariumBead.values()) {
			if ((state.beadMask() & bead.bit()) != 0)
				beads.add(bead);
		}
		return beads;
	}

	public static Optional<CorruptedRosariumCombination> getActiveCombination(Player player) {
		CorruptedRosariumState state = CorruptedRosariumState.get(player);
		return state == null ? Optional.empty() : state.combination();
	}

	public static boolean hasActiveCombination(Player player, CorruptedRosariumCombination combination) {
		CorruptedRosariumState state = CorruptedRosariumState.get(player);
		return state != null && state.hasCombination(combination);
	}

	public static boolean hasActiveCombination(
			Player player,
			CorruptedRosariumBead first,
			CorruptedRosariumBead second
	) {
		CorruptedRosariumState state = CorruptedRosariumState.get(player);
		return state != null && state.hasCombination(first, second);
	}

	private static SlotResult resolveCachedRosarium(Player player, CorruptedRosariumState state) {
		ICuriosItemHandler handler = CuriosApi.getCuriosInventory(player).orElse(null);
		if (handler == null)
			return null;

		ICurioStacksHandler stacksHandler = handler.getCurios().get(state.slotIdentifier());
		if (stacksHandler == null)
			return null;
		IDynamicStackHandler stacks = stacksHandler.getStacks();
		if (state.slotIndex() < 0 || state.slotIndex() >= stacks.getSlots())
			return null;

		NonNullList<Boolean> activeStates = stacksHandler.getActiveStates();
		if (state.slotIndex() < activeStates.size() && !activeStates.get(state.slotIndex()))
			return null;

		ItemStack stack = stacks.getStackInSlot(state.slotIndex());
		if (!stack.is(TimothatysTrinketsModItems.CORRUPTED_ROSARY.get()))
			return null;

		NonNullList<Boolean> renders = stacksHandler.getRenders();
		boolean visible = state.slotIndex() < renders.size() && renders.get(state.slotIndex());
		SlotContext context = new SlotContext(
				state.slotIdentifier(),
				player,
				state.slotIndex(),
				false,
				visible
		);
		return new SlotResult(context, stack);
	}
}
