package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumCombination;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumData;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumHelper;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumState;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import top.theillusivec4.curios.api.SlotResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HubrisState {
	private static final Map<UUID, Session> SESSIONS = new HashMap<>();

	private HubrisState() {
	}

	public static boolean start(
			ServerPlayer player,
			long sessionToken,
			int rosariumRevision,
			String sourceSlotIdentifier,
			int sourceSlotIndex
	) {
		if (player == null || !player.isAlive() || player.isDeadOrDying() || player.isRemoved()
				|| player.isSpectator() || sourceSlotIdentifier == null || hasActiveSession(player))
			return false;

		removeMaxHealthModifier(player);
		MobEffectInstance effect = new MobEffectInstance(
				TimothatysTrinketsModMobEffects.HUBRIS,
				HubrisData.DURATION_TICKS,
				0,
				false,
				false,
				true
		);
		if (!player.addEffect(effect, player))
			return false;
		if (!applyMaxHealthModifier(player)) {
			player.removeEffect(TimothatysTrinketsModMobEffects.HUBRIS);
			return false;
		}

		long startGameTime = player.level().getGameTime();
		Session session = new Session(
				sessionToken,
				startGameTime,
				startGameTime + HubrisData.DURATION_TICKS,
				HubrisData.INITIAL_THORNS,
				rosariumRevision,
				sourceSlotIdentifier,
				sourceSlotIndex
		);
		SESSIONS.put(player.getUUID(), session);
		HubrisVisuals.syncHubrisState(
				player,
				session.sessionToken,
				session.startGameTime,
				session.endGameTime,
				session.remainingThorns,
				true
		);
		return true;
	}

	public static void tick(ServerPlayer player) {
		Session session = session(player);
		boolean hasHubrisEffect = player.hasEffect(TimothatysTrinketsModMobEffects.HUBRIS);
		if (session == null && !hasHubrisEffect)
			return;
		if (!hasRequiredRosarium(player, session)) {
			if (session != null)
				end(player, HubrisEndReason.DISPELLED, true);
			else
				player.removeEffect(TimothatysTrinketsModMobEffects.HUBRIS);
			return;
		}
		if (session == null)
			return;

		long now = player.level().getGameTime();
		if (!player.isAlive() || player.isDeadOrDying() || player.isRemoved()) {
			end(player, HubrisEndReason.INVALID, true);
			return;
		}
		if (!player.hasEffect(TimothatysTrinketsModMobEffects.HUBRIS)) {
			end(player, HubrisEndReason.DISPELLED, false);
			return;
		}
		if (now >= session.endGameTime) {
			end(player, HubrisEndReason.EXPIRED, true);
			return;
		}

		if (now > session.startGameTime
				&& Math.floorMod(now - session.startGameTime, HubrisData.VISUAL_KEEPALIVE_INTERVAL_TICKS) == 0) {
			HubrisVisuals.syncHubrisState(
					player,
					session.sessionToken,
					session.startGameTime,
					session.endGameTime,
					session.remainingThorns,
					true
			);
		}
	}

	public static boolean hasActiveSession(Player player) {
		return session(player) != null;
	}

	public static boolean matchesSourceSlot(Player player, String slotIdentifier, int slotIndex) {
		Session session = session(player);
		return session != null && session.matchesSourceSlot(slotIdentifier, slotIndex);
	}

	public static Strike currentStrike(Player player) {
		Session session = session(player);
		if (session == null || session.remainingThorns <= 0 || session.pendingEndAfterAttack)
			return null;
		int strikeIndex = HubrisData.INITIAL_THORNS - session.remainingThorns;
		return new Strike(strikeIndex, HubrisData.strikeMultiplier(strikeIndex));
	}

	public static boolean consumeThorn(ServerPlayer player) {
		Session session = session(player);
		if (session == null || session.remainingThorns <= 0 || session.pendingEndAfterAttack)
			return false;

		session.remainingThorns--;
		session.pendingEndAfterAttack = session.remainingThorns == 0;
		HubrisVisuals.syncHubrisState(
				player,
				session.sessionToken,
				session.startGameTime,
				session.endGameTime,
				session.remainingThorns,
				true
		);
		return true;
	}

	public static void finishPendingEnd(ServerPlayer player) {
		Session session = session(player);
		if (session != null && session.pendingEndAfterAttack && session.remainingThorns == 0)
			end(player, HubrisEndReason.DEPLETED, true);
	}

	public static void end(ServerPlayer player, HubrisEndReason reason, boolean removeEffect) {
		if (player == null || reason == null)
			return;

		Session removed = SESSIONS.remove(player.getUUID());
		removeMaxHealthModifier(player);
		HubrisStrikeResolver.clear(player);
		if (removeEffect && player.hasEffect(TimothatysTrinketsModMobEffects.HUBRIS))
			player.removeEffect(TimothatysTrinketsModMobEffects.HUBRIS);

		if (removed == null)
			return;
		HubrisVisuals.syncHubrisState(
				player,
				removed.sessionToken,
				removed.startGameTime,
				removed.endGameTime,
				0,
				false
		);
		if (reason.appliesSlowness())
			scheduleSlowness(player);
	}

	public static void clearAll(MinecraftServer server) {
		if (server != null) {
			for (UUID playerId : new ArrayList<>(SESSIONS.keySet())) {
				ServerPlayer player = server.getPlayerList().getPlayer(playerId);
				if (player != null)
					end(player, HubrisEndReason.SERVER_STOP, true);
			}
		}
		SESSIONS.clear();
		HubrisStrikeResolver.clearAll();
	}

	private static Session session(Player player) {
		return player == null ? null : SESSIONS.get(player.getUUID());
	}

	private static boolean hasRequiredRosarium(ServerPlayer player, Session session) {
		SlotResult activeRosarium = CorruptedRosariumHelper.findActiveRosariumResult(player).orElse(null);
		if (activeRosarium == null
				|| CorruptedRosariumData.getCombination(activeRosarium.stack())
						.filter(combination -> combination == CorruptedRosariumCombination.HUBRIS)
						.isEmpty())
			return false;
		if (session == null)
			return true;
		if (!session.matchesSourceSlot(
				activeRosarium.slotContext().identifier(),
				activeRosarium.slotContext().index()
		))
			return false;

		CorruptedRosariumState state = CorruptedRosariumState.get(player);
		return state != null && state.revision() == session.rosariumRevision;
	}

	private static boolean applyMaxHealthModifier(ServerPlayer player) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null)
			return false;
		maxHealth.removeModifier(HubrisData.MAX_HEALTH_MODIFIER_ID);
		maxHealth.addTransientModifier(new AttributeModifier(
				HubrisData.MAX_HEALTH_MODIFIER_ID,
				HubrisData.MAX_HEALTH_PENALTY,
				AttributeModifier.Operation.ADD_VALUE
		));
		player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
		return true;
	}

	private static void removeMaxHealthModifier(ServerPlayer player) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth != null)
			maxHealth.removeModifier(HubrisData.MAX_HEALTH_MODIFIER_ID);
	}

	private static void scheduleSlowness(ServerPlayer player) {
		UUID playerId = player.getUUID();
		MinecraftServer server = player.getServer();
		if (server == null)
			return;
		TimothatysTrinketsMod.queueServerWork(1, () -> {
			ServerPlayer current = server.getPlayerList().getPlayer(playerId);
			if (current == null || !current.isAlive() || current.isDeadOrDying() || current.isRemoved())
				return;
			current.addEffect(new MobEffectInstance(
					MobEffects.MOVEMENT_SLOWDOWN,
					HubrisData.SLOWNESS_DURATION_TICKS,
					0,
					false,
					true,
					true
			));
		});
	}

	public record Strike(int index, float multiplier) {
	}

	private static final class Session {
		private final long sessionToken;
		private final long startGameTime;
		private final long endGameTime;
		private final int rosariumRevision;
		private final String sourceSlotIdentifier;
		private final int sourceSlotIndex;
		private int remainingThorns;
		private boolean pendingEndAfterAttack;

		private Session(
				long sessionToken,
				long startGameTime,
				long endGameTime,
				int remainingThorns,
				int rosariumRevision,
				String sourceSlotIdentifier,
				int sourceSlotIndex
		) {
			this.sessionToken = sessionToken;
			this.startGameTime = startGameTime;
			this.endGameTime = endGameTime;
			this.remainingThorns = remainingThorns;
			this.rosariumRevision = rosariumRevision;
			this.sourceSlotIdentifier = sourceSlotIdentifier;
			this.sourceSlotIndex = sourceSlotIndex;
		}

		private boolean matchesSourceSlot(String slotIdentifier, int slotIndex) {
			return slotIdentifier != null
					&& sourceSlotIndex == slotIndex
					&& sourceSlotIdentifier.equals(slotIdentifier);
		}
	}
}
