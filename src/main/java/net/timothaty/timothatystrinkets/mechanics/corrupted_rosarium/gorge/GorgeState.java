package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.gorge;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.healing.RelicHealingService;
import net.timothaty.timothatystrinkets.mechanics.healing.RelicHealingType;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import top.theillusivec4.curios.api.SlotContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GorgeState {
	private static final Map<UUID, Session> SESSIONS = new HashMap<>();

	private GorgeState() {
	}

	public static boolean begin(
			ServerPlayer player,
			GorgeData.Restoration restoration,
			int rosariumRevision,
			SlotContext sourceSlot
	) {
		if (player == null
				|| restoration == null
				|| sourceSlot == null
				|| sourceSlot.cosmetic()
				|| SESSIONS.containsKey(player.getUUID()))
			return false;

		SESSIONS.put(
				player.getUUID(),
				new Session(
						restoration.totalHealing(),
						restoration.totalHunger(),
						player.level().getGameTime(),
						rosariumRevision,
						sourceSlot.identifier(),
						sourceSlot.index()
				)
		);
		return true;
	}

	public static boolean hasActiveSession(Player player) {
		return player != null && SESSIONS.containsKey(player.getUUID());
	}

	public static boolean isAbilityActive(Player player) {
		if (player == null)
			return false;
		return player.level().isClientSide()
				? player.hasEffect(TimothatysTrinketsModMobEffects.GORGE)
				: hasActiveSession(player);
	}

	public static boolean matchesSourceSlot(Player player, String slotIdentifier, int slotIndex) {
		if (player == null || slotIdentifier == null)
			return false;

		Session session = SESSIONS.get(player.getUUID());
		return session != null
				&& session.sourceSlotIndex == slotIndex
				&& session.sourceSlotIdentifier.equals(slotIdentifier);
	}

	public static boolean matchesRosariumRevision(Player player, int revision) {
		if (player == null)
			return false;
		Session session = SESSIONS.get(player.getUUID());
		return session != null && session.rosariumRevision == revision;
	}

	public static void tick(ServerPlayer player) {
		if (player == null)
			return;

		Session session = SESSIONS.get(player.getUUID());
		if (session == null)
			return;

		long now = player.level().getGameTime();
		long deadline = session.startGameTime + GorgeData.DURATION_TICKS;
		while (session.nextDeliveryGameTime <= now
				&& session.nextDeliveryGameTime <= deadline
				&& !session.isEmpty()) {
			deliverOneSecond(player, session);
			session.nextDeliveryGameTime += GorgeData.DELIVERY_INTERVAL_TICKS;
		}

		if (session.isEmpty() || now >= deadline)
			finishSuccessfully(player, true);
	}

	public static boolean finishNormally(ServerPlayer player) {
		if (player == null)
			return false;

		Session session = SESSIONS.get(player.getUUID());
		if (session == null)
			return false;

		long deadline = session.startGameTime + GorgeData.DURATION_TICKS;
		while (session.nextDeliveryGameTime <= deadline
				&& !session.isEmpty()) {
			deliverOneSecond(player, session);
			session.nextDeliveryGameTime += GorgeData.DELIVERY_INTERVAL_TICKS;
		}
		if (SESSIONS.remove(player.getUUID()) == null)
			return false;
		queuePenalty(player);
		return true;
	}

	public static boolean interruptWithPenalty(ServerPlayer player, boolean removeEffect) {
		if (player == null)
			return false;

		Session session = SESSIONS.remove(player.getUUID());
		if (session == null)
			return false;

		if (removeEffect)
			player.removeEffect(TimothatysTrinketsModMobEffects.GORGE);
		applyPenalty(player);
		return true;
	}

	static boolean interruptWithDeferredPenalty(ServerPlayer player) {
		if (player == null)
			return false;

		Session session = SESSIONS.remove(player.getUUID());
		if (session == null)
			return false;

		queuePenalty(player);
		return true;
	}

	public static boolean cancelWithoutPenalty(Player player, boolean removeEffect) {
		if (player == null)
			return false;

		boolean removed = SESSIONS.remove(player.getUUID()) != null;
		if (removeEffect && !player.level().isClientSide())
			player.removeEffect(TimothatysTrinketsModMobEffects.GORGE);
		return removed;
	}

	public static Snapshot getSnapshot(Player player) {
		if (player == null)
			return null;
		Session session = SESSIONS.get(player.getUUID());
		return session == null ? null : new Snapshot(
				session.remainingHealing,
				session.remainingHunger,
				session.startGameTime,
				session.rosariumRevision,
				session.sourceSlotIdentifier,
				session.sourceSlotIndex
		);
	}

	public static void clearAll() {
		SESSIONS.clear();
	}

	static boolean tryScheduleDigestiveSurge(ServerPlayer player) {
		if (player == null
				|| !player.isAlive()
				|| player.isDeadOrDying()
				|| player.isRemoved()
				|| !player.hasEffect(TimothatysTrinketsModMobEffects.GORGE)
				|| player.getHealth() >= player.getMaxHealth()) {
			return false;
		}

		Session session = SESSIONS.get(player.getUUID());
		if (session == null
				|| session.remainingHealing <= 0.0F
				|| session.digestiveSurgePending) {
			return false;
		}

		long now = player.level().getGameTime();
		if (now < session.nextDigestiveSurgeGameTime)
			return false;

		session.digestiveSurgePending = true;
		session.nextDigestiveSurgeGameTime =
				now + GorgeData.DIGESTIVE_SURGE_COOLDOWN_TICKS;
		long sessionStartGameTime = session.startGameTime;
		TimothatysTrinketsMod.queueServerWork(
				1,
				() -> executeDigestiveSurge(player, sessionStartGameTime)
		);
		return true;
	}

	private static boolean finishSuccessfully(ServerPlayer player, boolean removeEffect) {
		Session removed = SESSIONS.remove(player.getUUID());
		if (removed == null)
			return false;

		if (removeEffect)
			player.removeEffect(TimothatysTrinketsModMobEffects.GORGE);
		applyPenalty(player);
		return true;
	}

	private static void deliverOneSecond(ServerPlayer player, Session session) {
		if (session.remainingHealing > 0.0F && player.getHealth() < player.getMaxHealth()) {
			float missingHealth = Math.max(0.0F, player.getMaxHealth() - player.getHealth());
			float healed = Math.min(1.0F, Math.min(session.remainingHealing, missingHealth));
			if (healed > 0.0F) {
				float actualHealing = RelicHealingService.heal(
						player,
						healed,
						RelicHealingType.NATURAL
				);
				session.remainingHealing = Math.max(0.0F, session.remainingHealing - actualHealing);
			}
		}

		if (session.remainingHunger > 0 && player.getFoodData().getFoodLevel() < 20) {
			int hungerBefore = player.getFoodData().getFoodLevel();
			GorgeData.addHungerWithoutSaturation(player, 1);
			int actualHunger = Math.max(0, player.getFoodData().getFoodLevel() - hungerBefore);
			session.remainingHunger = Math.max(0, session.remainingHunger - actualHunger);
		}
	}

	private static void executeDigestiveSurge(
			ServerPlayer player,
			long expectedSessionStartGameTime
	) {
		if (player == null)
			return;

		Session session = SESSIONS.get(player.getUUID());
		if (session == null
				|| session.startGameTime != expectedSessionStartGameTime) {
			return;
		}

		session.digestiveSurgePending = false;
		if (!player.isAlive()
				|| player.isDeadOrDying()
				|| player.isRemoved()
				|| !player.hasEffect(TimothatysTrinketsModMobEffects.GORGE)
				|| session.remainingHealing <= 0.0F) {
			return;
		}

		float missingHealth = Math.max(
				0.0F,
				player.getMaxHealth() - player.getHealth()
		);
		float requested = Math.min(
				GorgeData.DIGESTIVE_SURGE_HEALING,
				Math.min(session.remainingHealing, missingHealth)
		);
		if (requested <= 0.0F)
			return;

		float actualHealing = RelicHealingService.heal(
				player,
				requested,
				RelicHealingType.NATURAL
		);
		if (actualHealing <= 0.0F)
			return;

		session.remainingHealing = Math.max(
				0.0F,
				session.remainingHealing - actualHealing
		);
		GorgeVisuals.emitDigestiveSurge(player);
	}

	private static void applyPenalty(ServerPlayer player) {
		if (!player.isAlive()
				|| player.isDeadOrDying()
				|| player.isRemoved())
			return;

		player.addEffect(new MobEffectInstance(
				MobEffects.WEAKNESS,
				GorgeData.PENALTY_DURATION_TICKS,
				0,
				false,
				true,
				true
		));
		player.addEffect(new MobEffectInstance(
				MobEffects.MOVEMENT_SLOWDOWN,
				GorgeData.PENALTY_DURATION_TICKS,
				0,
				false,
				true,
				true
		));
	}

	private static void queuePenalty(ServerPlayer player) {
		TimothatysTrinketsMod.queueServerWork(
				1,
				() -> applyPenalty(player)
		);
	}

	private static final class Session {
		private final long startGameTime;
		private final int rosariumRevision;
		private final String sourceSlotIdentifier;
		private final int sourceSlotIndex;
		private float remainingHealing;
		private int remainingHunger;
		private long nextDeliveryGameTime;
		private long nextDigestiveSurgeGameTime;
		private boolean digestiveSurgePending;

		private Session(
				int totalHealing,
				int totalHunger,
				long startGameTime,
				int rosariumRevision,
				String sourceSlotIdentifier,
				int sourceSlotIndex
		) {
			this.remainingHealing = totalHealing;
			this.remainingHunger = totalHunger;
			this.startGameTime = startGameTime;
			this.nextDeliveryGameTime = startGameTime + GorgeData.DELIVERY_INTERVAL_TICKS;
			this.rosariumRevision = rosariumRevision;
			this.sourceSlotIdentifier = sourceSlotIdentifier;
			this.sourceSlotIndex = sourceSlotIndex;
		}

		private boolean isEmpty() {
			return remainingHealing <= 0.0F && remainingHunger <= 0;
		}
	}

	public record Snapshot(
			float remainingHealing,
			int remainingHunger,
			long startGameTime,
			int rosariumRevision,
			String sourceSlotIdentifier,
			int sourceSlotIndex
	) {
	}
}
