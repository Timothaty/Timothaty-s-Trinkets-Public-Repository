package net.timothaty.timothatystrinkets.mechanics.holy_rosarium;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.armlet_gauntlet_synergy.ArmletGauntletSynergyState;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;

import net.neoforged.fml.ModList;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class HolyRosariumState {
	private static final String NO_SLOT = "";
	private static final Map<Player, HolyRosariumState> STATES = Collections.synchronizedMap(new WeakHashMap<>());

	private boolean active;
	private int beadMask;
	private String slotIdentifier = NO_SLOT;
	private int slotIndex = -1;
	private boolean cosmetic;
	private boolean bonusesSuppressed;
	private int revision;
	private boolean dirty = true;
	private boolean initialized;
	private long lastRefreshGameTime = Long.MIN_VALUE;

	private HolyRosariumState() {
	}

	public static HolyRosariumState get(Player player) {
		if (player == null)
			return null;

		HolyRosariumState state = stateFor(player);
		if (state.dirty)
			refreshNow(player);
		return state;
	}

	public static int getRevision(Player player) {
		HolyRosariumState state = get(player);
		return state == null ? 0 : state.revision;
	}

	public static void markDirty(Player player) {
		if (player != null)
			stateFor(player).dirty = true;
	}

	public static boolean refreshIfDirty(Player player) {
		if (player == null)
			return false;

		HolyRosariumState state = stateFor(player);
		if (state.dirty) {
			refreshNow(player);
			return true;
		}
		return false;
	}

	public static void refreshNow(Player player) {
		if (player == null)
			return;

		HolyRosariumState state = stateFor(player);
		long gameTime = player.level().getGameTime();
		if (state.initialized && !state.dirty && state.lastRefreshGameTime == gameTime)
			return;
		boolean active = false;
		int beadMask = 0;
		String slotIdentifier = NO_SLOT;
		int slotIndex = -1;
		boolean cosmetic = false;
		ICuriosItemHandler inventory = null;

		if (ModList.get().isLoaded("curios")) {
			inventory = CuriosApi.getCuriosInventory(player).orElse(null);
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
						if (!stack.is(TimothatysTrinketsModItems.HOLY_ROSARIUM.get()))
							continue;

						active = true;
						beadMask = HolyRosariumData.getKnownBeadMask(stack);
						slotIdentifier = entry.getKey();
						slotIndex = index;
						break search;
					}
				}
			}
		}

		boolean bonusesSuppressed = player.hasEffect(TimothatysTrinketsModMobEffects.ANATHEMA);
		state.lastRefreshGameTime = gameTime;
		publish(player, state, active, beadMask, slotIdentifier, slotIndex, cosmetic, bonusesSuppressed);
	}

	public static void setBonusesSuppressed(Player player, boolean suppressed) {
		if (player == null)
			return;

		HolyRosariumState state = get(player);
		if (state == null || state.bonusesSuppressed == suppressed)
			return;

		publish(
				player,
				state,
				state.active,
				state.beadMask,
				state.slotIdentifier,
				state.slotIndex,
				state.cosmetic,
				suppressed
		);
	}

	public static void forget(Player player) {
		if (player == null)
			return;
		STATES.remove(player);
		HolyRosariumModifierService.forget(player);
	}

	private static HolyRosariumState stateFor(Player player) {
		return STATES.computeIfAbsent(player, ignored -> new HolyRosariumState());
	}

	private static void publish(
			Player player,
			HolyRosariumState state,
			boolean active,
			int beadMask,
			String slotIdentifier,
			int slotIndex,
			boolean cosmetic,
			boolean bonusesSuppressed
	) {
		state.dirty = false;
		boolean suppressedUnholyRelicsBefore = state.suppressesUnholyRelics();
		boolean changed = !state.initialized
				|| state.active != active
				|| state.beadMask != beadMask
				|| !state.slotIdentifier.equals(slotIdentifier)
				|| state.slotIndex != slotIndex
				|| state.cosmetic != cosmetic
				|| state.bonusesSuppressed != bonusesSuppressed;
		if (!changed) {
			HolyRosariumModifierService.applyIfNeeded(player, state);
			return;
		}

		state.active = active;
		state.beadMask = beadMask;
		state.slotIdentifier = slotIdentifier;
		state.slotIndex = slotIndex;
		state.cosmetic = cosmetic;
		state.bonusesSuppressed = bonusesSuppressed;
		state.initialized = true;
		state.revision++;

		HolyRosariumModifierService.applyIfNeeded(player, state);
		if (suppressedUnholyRelicsBefore != state.suppressesUnholyRelics())
			ArmletGauntletSynergyState.onSuppressionChanged(player);
	}

	public boolean active() {
		return active;
	}

	public boolean bonusesActive() {
		return active && !bonusesSuppressed;
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

	public boolean cosmetic() {
		return cosmetic;
	}

	public boolean bonusesSuppressed() {
		return bonusesSuppressed;
	}

	public int revision() {
		return revision;
	}

	public boolean dirty() {
		return dirty;
	}

	public boolean hasCombination(HolyRosariumBead first, HolyRosariumBead second) {
		if (!bonusesActive() || first == null || second == null || first == second)
			return false;
		return hasExactBeads(first, second);
	}

	private boolean hasExactBeads(HolyRosariumBead first, HolyRosariumBead second) {
		int requiredMask = first.bit() | second.bit();
		return Integer.bitCount(beadMask) == HolyRosariumData.SLOT_COUNT
				&& (beadMask & requiredMask) == requiredMask;
	}

	public boolean suppressesUnholyRelics() {
		return active;
	}

	public boolean matches(SlotContext slotContext) {
		return slotContext != null
				&& active
				&& slotIndex == slotContext.index()
				&& cosmetic == slotContext.cosmetic()
				&& slotIdentifier.equals(slotContext.identifier());
	}
}
