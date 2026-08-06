package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris;

import net.timothaty.timothatystrinkets.mechanics.active_ability.ActiveAbilityCastLock;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumCombination;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumData;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumHelper;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumState;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HubrisActivationState {
	private static final Map<UUID, Session> SESSIONS = new HashMap<>();

	private HubrisActivationState() {
	}

	public static boolean begin(
			ServerPlayer player,
			HubrisAnimationVariant variant,
			ItemStack weaponSnapshot,
			int rosariumRevision,
			SlotContext sourceSlot
	) {
		if (player == null || variant == null || weaponSnapshot == null || weaponSnapshot.isEmpty()
				|| sourceSlot == null || isCasting(player))
			return false;

		long startGameTime = player.level().getGameTime();
		long token = player.getRandom().nextLong()
				^ startGameTime * 0x9E3779B97F4A7C15L
				^ player.getUUID().getLeastSignificantBits();
		Session session = new Session(
				token,
				startGameTime,
				player.getMainArm(),
				player.getInventory().selected,
				variant,
				weaponSnapshot.copy(),
				rosariumRevision,
				sourceSlot.identifier(),
				sourceSlot.index()
		);
		SESSIONS.put(player.getUUID(), session);
		player.stopUsingItem();
		ActiveAbilityCastLock.lock(player, HubrisData.CAST_LOCK_ID, HubrisData.ACTIVATION_TICKS);
		HubrisVisuals.syncActivation(player, session.snapshot(), true);
		return true;
	}

	public static void tick(ServerPlayer player) {
		Session session = session(player);
		if (session == null)
			return;

		long elapsed = player.level().getGameTime() - session.startGameTime;
		if (!isSessionValid(player, session)) {
			cancel(player);
			return;
		}

		player.getInventory().selected = session.selectedHotbarSlot;
		player.setSprinting(false);

		if (!session.soundPlayed && elapsed >= HubrisData.SOUND_TICK) {
			session.soundPlayed = true;
			HubrisVisuals.playActivationSound(player, session.variant);
		}
		if (elapsed >= HubrisData.ACTIVATION_TICKS) {
			HubrisVisuals.emitApplicationBurst(player);
			if (!HubrisState.start(
				player,
				session.sessionToken,
				session.rosariumRevision,
				session.sourceSlotIdentifier,
				session.sourceSlotIndex
			)) {
				cancel(player);
				return;
			}
			finish(player);
		}
	}

	public static boolean isCasting(Player player) {
		return session(player) != null;
	}

	public static Snapshot getSnapshot(Player player) {
		Session session = session(player);
		return session == null ? null : session.snapshot();
	}

	public static boolean matchesSourceSlot(Player player, String identifier, int index) {
		Session session = session(player);
		return session != null
				&& session.sourceSlotIndex == index
				&& session.sourceSlotIdentifier.equals(identifier);
	}

	public static void cancel(ServerPlayer player) {
		end(player);
	}

	public static void finish(ServerPlayer player) {
		end(player);
	}

	public static void clear(Player player) {
		if (player != null)
			SESSIONS.remove(player.getUUID());
	}

	public static void clearAll() {
		SESSIONS.clear();
	}

	private static void end(ServerPlayer player) {
		if (player == null)
			return;
		Session removed = SESSIONS.remove(player.getUUID());
		ActiveAbilityCastLock.unlock(player, HubrisData.CAST_LOCK_ID);
		if (removed != null)
			HubrisVisuals.syncActivation(player, removed.snapshot(), false);
	}

	private static boolean isSessionValid(ServerPlayer player, Session session) {
		if (!player.isAlive() || player.isDeadOrDying() || player.isRemoved() || player.isSpectator())
			return false;
		if (!ItemStack.isSameItemSameComponents(session.weaponSnapshot, player.getMainHandItem()))
			return false;

		CorruptedRosariumState.markDirty(player);
		CorruptedRosariumState.refreshNow(player);
		if (CorruptedRosariumState.getRevision(player) != session.rosariumRevision)
			return false;
		SlotResult result = CorruptedRosariumHelper.findActiveRosariumResult(player).orElse(null);
		if (result == null
				|| !session.sourceSlotIdentifier.equals(result.slotContext().identifier())
				|| session.sourceSlotIndex != result.slotContext().index())
			return false;
		return CorruptedRosariumData.getCombination(result.stack())
				.filter(combination -> combination == CorruptedRosariumCombination.HUBRIS)
				.isPresent();
	}

	private static Session session(Player player) {
		return player == null ? null : SESSIONS.get(player.getUUID());
	}

	public record Snapshot(
			long sessionToken,
			long startGameTime,
			HumanoidArm mainArm,
			int selectedHotbarSlot,
			HubrisAnimationVariant variant,
			ItemStack weaponSnapshot,
			int rosariumRevision,
			String sourceSlotIdentifier,
			int sourceSlotIndex
	) {
	}

	private static final class Session {
		private final long sessionToken;
		private final long startGameTime;
		private final HumanoidArm mainArm;
		private final int selectedHotbarSlot;
		private final HubrisAnimationVariant variant;
		private final ItemStack weaponSnapshot;
		private final int rosariumRevision;
		private final String sourceSlotIdentifier;
		private final int sourceSlotIndex;
		private boolean soundPlayed;

		private Session(
				long sessionToken,
				long startGameTime,
				HumanoidArm mainArm,
				int selectedHotbarSlot,
				HubrisAnimationVariant variant,
				ItemStack weaponSnapshot,
				int rosariumRevision,
				String sourceSlotIdentifier,
				int sourceSlotIndex
		) {
			this.sessionToken = sessionToken;
			this.startGameTime = startGameTime;
			this.mainArm = mainArm;
			this.selectedHotbarSlot = selectedHotbarSlot;
			this.variant = variant;
			this.weaponSnapshot = weaponSnapshot;
			this.rosariumRevision = rosariumRevision;
			this.sourceSlotIdentifier = sourceSlotIdentifier;
			this.sourceSlotIndex = sourceSlotIndex;
		}

		private Snapshot snapshot() {
			return new Snapshot(
					sessionToken,
					startGameTime,
					mainArm,
					selectedHotbarSlot,
					variant,
					weaponSnapshot,
					rosariumRevision,
					sourceSlotIdentifier,
					sourceSlotIndex
			);
		}
	}
}
