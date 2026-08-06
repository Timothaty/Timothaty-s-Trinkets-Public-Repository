package net.timothaty.timothatystrinkets.mechanics.armlet_gauntlet_synergy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioChangeEvent;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.mechanics.champions_gauntlet.ChampionsGauntletEvents;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumHelper;
import net.timothaty.timothatystrinkets.mechanics.undead_knights_armlet.UndeadKnightsArmletEvents;
import net.timothaty.timothatystrinkets.util.CuriosBraceletSlotHelper;
import net.timothaty.timothatystrinkets.util.CuriosHandsSlotHelper;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsEquipState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class ArmletGauntletSynergyState {
	private static final int SAFETY_RESYNC_INTERVAL_TICKS = 40;
	private static final Map<UUID, MutableState> SERVER_STATES = new HashMap<>();
	private static final Map<UUID, MutableState> CLIENT_STATES = new HashMap<>();

	private ArmletGauntletSynergyState() {
	}

	public static Snapshot get(Player player) {
		MutableState state = player == null ? null : stateMap(player).get(player.getUUID());
		return state == null ? Snapshot.EMPTY : state.snapshot;
	}

	public static Snapshot getOrRefresh(Player player) {
		if (player == null) {
			return Snapshot.EMPTY;
		}
		MutableState state = stateMap(player).get(player.getUUID());
		if (state == null || state.dirty) {
			return refreshFromCurios(player);
		}
		refreshPhysicalArm(player, state);
		return state.snapshot;
	}

	public static int getRevision(Player player) {
		return get(player).revision();
	}

	public static boolean isGauntletActive(Player player) {
		MutableState state = player == null ? null : stateMap(player).get(player.getUUID());
		return state != null
				? state.gauntletActive
				: TimothatysTrinketsEquipState.has(player, TimothatysTrinketsEquipState.CHAMPIONS_GAUNTLET);
	}

	public static boolean isArmletActive(Player player) {
		MutableState state = player == null ? null : stateMap(player).get(player.getUUID());
		return state != null
				? state.armletActive
				: TimothatysTrinketsEquipState.has(player, TimothatysTrinketsEquipState.UNDEAD_KNIGHTS_ARMLET);
	}

	public static boolean isPhysicalSynergyOnArm(Player player, HumanoidArm arm) {
		if (player == null || arm == null) {
			return false;
		}
		MutableState state = stateMap(player).get(player.getUUID());
		boolean clientSafetyRefresh = state != null && player.level().isClientSide()
				&& player.level().getGameTime() - state.lastCuriosRefreshGameTime >= SAFETY_RESYNC_INTERVAL_TICKS;
		if (state == null || state.dirty || clientSafetyRefresh) {
			refreshFromCurios(player);
			state = stateMap(player).get(player.getUUID());
		}
		if (state != null) {
			refreshPhysicalArm(player, state);
		}
		return state != null && state.physicalGauntlet && state.physicalArmlet
				&& state.snapshot.physicalArm() == arm && state.armletSlot >= 0
				&& CuriosHandsSlotHelper.physicalArmForSlot(player, state.armletSlot) == arm;
	}

	public static void invalidate(Player player) {
		if (player == null) {
			return;
		}
		MutableState state = stateMap(player).get(player.getUUID());
		if (state != null) {
			state.dirty = true;
		}
	}

	public static void onSuppressionChanged(Player player) {
		recomputeActiveStates(player);
		SoulEmpowerHelper.refreshAttributeModifiers(player);
	}

	public static void recomputeActiveStates(Player player) {
		if (player == null) {
			return;
		}
		MutableState state = stateMap(player).get(player.getUUID());
		if (state == null || state.dirty) {
			refreshFromCurios(player);
			return;
		}
		applySnapshot(player, state, state.physicalGauntlet, state.physicalArmlet,
				state.gauntletSlot, state.armletSlot);
	}

	public static Snapshot refreshFromCurios(Player player) {
		if (player == null) {
			return Snapshot.EMPTY;
		}

		MutableState state = stateMap(player).computeIfAbsent(player.getUUID(), ignored -> new MutableState());
		long gameTime = player.level().getGameTime();
		if (!state.dirty && state.lastCuriosRefreshGameTime == gameTime) {
			return state.snapshot;
		}
		boolean foundGauntlet = false;
		boolean foundArmlet = false;
		int gauntletSlot = -1;
		int armletSlot = -1;

		if (ModList.get().isLoaded("curios")) {
			ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(player).orElse(null);
			if (inventory != null) {
				for (ICurioStacksHandler handler : inventory.getCurios().values()) {
					String identifier = handler.getIdentifier();
					boolean hands = CuriosHandsSlotHelper.HANDS_SLOT_IDENTIFIER.equals(identifier);
					boolean bracelet = CuriosBraceletSlotHelper.BRACELET_SLOT_IDENTIFIER.equals(identifier);
					if (!hands && !bracelet) {
						continue;
					}

					IDynamicStackHandler stacks = handler.getStacks();
					int slots = Math.min(handler.getSlots(), stacks.getSlots());
					for (int slot = 0; slot < slots; slot++) {
						ItemStack stack = stacks.getStackInSlot(slot);
						if (hands && !foundGauntlet && stack.is(TimothatysTrinketsModItems.CHAMPIONS_GAUNTLET.get())) {
							foundGauntlet = true;
							gauntletSlot = slot;
						} else if (bracelet && !foundArmlet && stack.is(TimothatysTrinketsModItems.UNDEAD_KNIGHTS_ARMLET.get())) {
							foundArmlet = true;
							armletSlot = slot;
						}
					}
					if (foundGauntlet && foundArmlet) {
						break;
					}
				}
			}
		}

		state.dirty = false;
		state.lastCuriosRefreshGameTime = gameTime;
		applySnapshot(player, state, foundGauntlet, foundArmlet, gauntletSlot, armletSlot);
		return state.snapshot;
	}

	@SubscribeEvent
	public static void onCurioChanged(CurioChangeEvent event) {
		if (!(event.getEntity() instanceof Player player)) {
			return;
		}
		if (!isRelevant(event.getFrom()) && !isRelevant(event.getTo())) {
			return;
		}
		invalidate(player);
		refreshFromCurios(player);
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}
		MutableState state = SERVER_STATES.get(player.getUUID());
		if (state == null || state.dirty) {
			refreshFromCurios(player);
			return;
		}

		refreshPhysicalArm(player, state);
		if (Math.floorMod(player.tickCount + player.getId(), SAFETY_RESYNC_INTERVAL_TICKS) == 0) {
			refreshFromCurios(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof Player player) {
			SERVER_STATES.remove(player.getUUID());
			refreshFromCurios(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		Player player = event.getEntity();
		SERVER_STATES.remove(player.getUUID());
		refreshFromCurios(player);
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		stateMap(event.getEntity()).remove(event.getEntity().getUUID());
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		SERVER_STATES.clear();
	}

	public static void clear(Player player) {
		if (player != null) {
			stateMap(player).remove(player.getUUID());
		}
	}

	public static void clearClientStates() {
		CLIENT_STATES.clear();
	}

	private static Map<UUID, MutableState> stateMap(Player player) {
		return player.level().isClientSide() ? CLIENT_STATES : SERVER_STATES;
	}

	private static void refreshPhysicalArm(Player player, MutableState state) {
		if (!state.physicalGauntlet || state.gauntletSlot < 0) {
			return;
		}
		HumanoidArm arm = CuriosHandsSlotHelper.physicalArmForSlot(player, state.gauntletSlot);
		if (arm != state.snapshot.physicalArm()) {
			applySnapshot(player, state, state.physicalGauntlet, state.physicalArmlet,
					state.gauntletSlot, state.armletSlot);
		}
	}

	private static void applySnapshot(Player player, MutableState state, boolean physicalGauntlet,
			boolean physicalArmlet, int gauntletSlot, int armletSlot) {
		boolean suppressed = HolyRosariumHelper.suppressesUnholyRelics(player);
		boolean gauntletActive = physicalGauntlet && !suppressed;
		boolean armletActive = physicalArmlet && !suppressed;
		int normalizedGauntletSlot = physicalGauntlet ? gauntletSlot : -1;
		int normalizedArmletSlot = physicalArmlet ? armletSlot : -1;
		InteractionHand interactionHand = normalizedGauntletSlot < 0
				? null
				: CuriosHandsSlotHelper.interactionHandForSlot(normalizedGauntletSlot);
		HumanoidArm physicalArm = normalizedGauntletSlot < 0
				? null
				: CuriosHandsSlotHelper.physicalArmForSlot(player, normalizedGauntletSlot);
		boolean synergyActive = gauntletActive && armletActive;
		boolean oldGauntletActive = state.gauntletActive;
		boolean oldArmletSoloActive = state.armletActive && !state.gauntletActive;

		boolean changed = !state.initialized
				|| state.physicalGauntlet != physicalGauntlet
				|| state.physicalArmlet != physicalArmlet
				|| state.gauntletActive != gauntletActive
				|| state.armletActive != armletActive
				|| state.gauntletSlot != normalizedGauntletSlot
				|| state.armletSlot != normalizedArmletSlot
				|| state.snapshot.interactionHand() != interactionHand
				|| state.snapshot.physicalArm() != physicalArm;

		state.physicalGauntlet = physicalGauntlet;
		state.physicalArmlet = physicalArmlet;
		state.gauntletActive = gauntletActive;
		state.armletActive = armletActive;
		state.gauntletSlot = normalizedGauntletSlot;
		state.armletSlot = normalizedArmletSlot;
		state.dirty = false;
		if (changed) {
			state.revision++;
			state.snapshot = new Snapshot(synergyActive, normalizedGauntletSlot,
					interactionHand, physicalArm, state.revision);
		}

		if (TimothatysTrinketsEquipState.has(player, TimothatysTrinketsEquipState.CHAMPIONS_GAUNTLET) != gauntletActive) {
			TimothatysTrinketsEquipState.set(player, TimothatysTrinketsEquipState.CHAMPIONS_GAUNTLET, gauntletActive);
		}
		if (TimothatysTrinketsEquipState.has(player, TimothatysTrinketsEquipState.UNDEAD_KNIGHTS_ARMLET) != armletActive) {
			TimothatysTrinketsEquipState.set(player, TimothatysTrinketsEquipState.UNDEAD_KNIGHTS_ARMLET, armletActive);
		}

		if (!player.level().isClientSide()) {
			boolean gauntletChanged = !state.initialized || oldGauntletActive != gauntletActive;
			boolean armletSoloActive = armletActive && !gauntletActive;
			boolean armletChanged = !state.initialized || oldArmletSoloActive != armletSoloActive;
			if (gauntletChanged) {
				ChampionsGauntletEvents.onMechanicalActiveStateChanged(player, gauntletActive);
			}
			if (armletChanged) {
				UndeadKnightsArmletEvents.onMechanicalActiveStateChanged(player, armletSoloActive);
			}
		}
		state.initialized = true;
	}

	private static boolean isRelevant(ItemStack stack) {
		return stack != null && (stack.is(TimothatysTrinketsModItems.CHAMPIONS_GAUNTLET.get())
				|| stack.is(TimothatysTrinketsModItems.UNDEAD_KNIGHTS_ARMLET.get()));
	}

	public record Snapshot(boolean synergyActive, int gauntletSlot, InteractionHand interactionHand,
			HumanoidArm physicalArm, int revision) {
		private static final Snapshot EMPTY = new Snapshot(false, -1, null, null, 0);
	}

	private static final class MutableState {
		private boolean initialized;
		private boolean dirty = true;
		private boolean physicalGauntlet;
		private boolean physicalArmlet;
		private boolean gauntletActive;
		private boolean armletActive;
		private int gauntletSlot = -1;
		private int armletSlot = -1;
		private int revision;
		private long lastCuriosRefreshGameTime = Long.MIN_VALUE;
		private Snapshot snapshot = Snapshot.EMPTY;
	}
}
