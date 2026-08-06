package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.neoforged.fml.ModList;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public final class CorruptedRosariumState {
	private static final String NO_SLOT = "";
	private static final Map<Player, CorruptedRosariumState> STATES = Collections.synchronizedMap(new WeakHashMap<>());

	private boolean active;
	private int beadMask;
	private String slotIdentifier = NO_SLOT;
	private int slotIndex = -1;
	private int revision;
	private boolean dirty = true;
	private boolean initialized;
	private long lastRefreshGameTime = Long.MIN_VALUE;

	private CorruptedRosariumState() {
	}

	public static CorruptedRosariumState get(Player player) {
		if (player == null)
			return null;

		CorruptedRosariumState state = stateFor(player);
		if (state.dirty)
			refreshNow(player);
		return state;
	}

	public static int getRevision(Player player) {
		CorruptedRosariumState state = get(player);
		return state == null ? 0 : state.revision;
	}

	public static void markDirty(Player player) {
		if (player != null)
			stateFor(player).dirty = true;
	}

	public static boolean refreshIfDirty(Player player) {
		if (player == null)
			return false;

		CorruptedRosariumState state = stateFor(player);
		if (!state.dirty)
			return false;

		refreshNow(player);
		return true;
	}

	public static void refreshNow(Player player) {
		if (player == null)
			return;

		CorruptedRosariumState state = stateFor(player);
		long gameTime = player.level().getGameTime();
		if (state.initialized && !state.dirty && state.lastRefreshGameTime == gameTime)
			return;

		boolean active = false;
		int beadMask = 0;
		String slotIdentifier = NO_SLOT;
		int slotIndex = -1;

		if (ModList.get().isLoaded("curios")) {
			ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(player).orElse(null);
			if (inventory != null) {
				search:
				for (Map.Entry<String, ICurioStacksHandler> entry : inventory.getCurios().entrySet()) {
					ICurioStacksHandler stacksHandler = entry.getValue();
					IDynamicStackHandler stacks = stacksHandler.getStacks();
					NonNullList<Boolean> activeStates = stacksHandler.getActiveStates();
					for (int index = 0; index < stacks.getSlots(); index++) {
						if (index < activeStates.size() && !activeStates.get(index))
							continue;

						ItemStack stack = stacks.getStackInSlot(index);
						if (!stack.is(TimothatysTrinketsModItems.CORRUPTED_ROSARY.get()))
							continue;

						active = true;
						beadMask = CorruptedRosariumData.getKnownBeadMask(stack);
						slotIdentifier = entry.getKey();
						slotIndex = index;
						break search;
					}
				}
			}
		}

		state.lastRefreshGameTime = gameTime;
		publish(state, active, beadMask, slotIdentifier, slotIndex);
	}

	public static void forget(Player player) {
		if (player != null)
			STATES.remove(player);
	}

	public static void clearAll() {
		STATES.clear();
	}

	private static CorruptedRosariumState stateFor(Player player) {
		return STATES.computeIfAbsent(player, ignored -> new CorruptedRosariumState());
	}

	private static void publish(
			CorruptedRosariumState state,
			boolean active,
			int beadMask,
			String slotIdentifier,
			int slotIndex
	) {
		state.dirty = false;
		boolean changed = !state.initialized
				|| state.active != active
				|| state.beadMask != beadMask
				|| !state.slotIdentifier.equals(slotIdentifier)
				|| state.slotIndex != slotIndex;
		if (!changed)
			return;

		state.active = active;
		state.beadMask = beadMask;
		state.slotIdentifier = slotIdentifier;
		state.slotIndex = slotIndex;
		state.initialized = true;
		state.revision++;
	}

	public boolean active() {
		return active;
	}

	public int beadMask() {
		return beadMask;
	}

	public String slotIdentifier() {
		return slotIdentifier;
	}

	public int slotIndex() {
		return slotIndex;
	}

	public int revision() {
		return revision;
	}

	public boolean dirty() {
		return dirty;
	}

	public Optional<CorruptedRosariumCombination> combination() {
		return active ? CorruptedRosariumCombination.fromMask(beadMask) : Optional.empty();
	}

	public boolean hasCombination(CorruptedRosariumCombination combination) {
		return active && combination != null && combination.matches(beadMask);
	}

	public boolean hasCombination(CorruptedRosariumBead first, CorruptedRosariumBead second) {
		if (!active || first == null || second == null || first == second)
			return false;

		int requiredMask = first.bit() | second.bit();
		return Integer.bitCount(beadMask) == CorruptedRosariumData.SLOT_COUNT
				&& beadMask == requiredMask;
	}

	public boolean matches(SlotContext slotContext) {
		return slotContext != null
				&& active
				&& !slotContext.cosmetic()
				&& slotIndex == slotContext.index()
				&& slotIdentifier.equals(slotContext.identifier());
	}
}
