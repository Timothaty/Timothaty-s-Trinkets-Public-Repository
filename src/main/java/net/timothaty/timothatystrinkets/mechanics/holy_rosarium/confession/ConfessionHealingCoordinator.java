package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.confession;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumBead;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import top.theillusivec4.curios.api.event.CurioChangeEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class ConfessionHealingCoordinator {
	private static final Map<UUID, Reservation> BY_PLAYER = new HashMap<>();
	private static final Map<UUID, Reservation> BY_CLERIC = new HashMap<>();

	private ConfessionHealingCoordinator() {
	}

	@SubscribeEvent
	public static void onPlayerDamaged(LivingDamageEvent.Post event) {
		if (event.getEntity() instanceof ServerPlayer player && event.getNewDamage() > 0.0F)
			evaluateCandidate(player);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onCurioChanged(CurioChangeEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			evaluateCandidate(player);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onAnathemaAdded(MobEffectEvent.Added event) {
		if (event.getEntity() instanceof ServerPlayer player
				&& event.getEffectInstance().getEffect().value() == TimothatysTrinketsModMobEffects.ANATHEMA.get())
			evaluateCandidate(player);
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;

		Reservation reservation = BY_PLAYER.get(player.getUUID());
		if (reservation != null && !isReservationValid(player.serverLevel(), reservation))
			release(reservation);

		if (Math.floorMod(player.tickCount + player.getId(), ConfessionData.FALLBACK_CHECK_INTERVAL_TICKS) == 0)
			evaluateCandidate(player);
	}

	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		releaseForEntity(event.getEntity());
	}

	@SubscribeEvent
	public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
		releaseForEntity(event.getEntity());
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		releaseForPlayer(event.getEntity().getUUID());
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		releaseForPlayer(event.getEntity().getUUID());
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		long cooldownUntil = event.getOriginal().getPersistentData().getLong(ConfessionData.HEAL_COOLDOWN_UNTIL_TAG);
		if (cooldownUntil > 0L)
			event.getEntity().getPersistentData().putLong(ConfessionData.HEAL_COOLDOWN_UNTIL_TAG, cooldownUntil);
		releaseForPlayer(event.getOriginal().getUUID());
		releaseForPlayer(event.getEntity().getUUID());
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		BY_PLAYER.clear();
		BY_CLERIC.clear();
	}

	static ServerPlayer getAssignedTarget(Villager cleric) {
		if (!(cleric.level() instanceof ServerLevel level))
			return null;
		Reservation reservation = BY_CLERIC.get(cleric.getUUID());
		if (reservation == null || !isReservationValid(level, reservation)) {
			if (reservation != null)
				release(reservation);
			return null;
		}
		return level.getPlayerByUUID(reservation.playerId()) instanceof ServerPlayer player ? player : null;
	}

	static boolean ownsReservation(Villager cleric, ServerPlayer player) {
		if (cleric == null || player == null)
			return false;
		Reservation reservation = BY_CLERIC.get(cleric.getUUID());
		return reservation != null
				&& reservation.playerId().equals(player.getUUID())
				&& isReservationValid(player.serverLevel(), reservation);
	}

	static boolean ownsCommittedReservation(Villager cleric, ServerPlayer player) {
		if (cleric == null || player == null)
			return false;
		Reservation reservation = BY_CLERIC.get(cleric.getUUID());
		return reservation != null
				&& reservation.healCommitted()
				&& reservation.playerId().equals(player.getUUID())
				&& isReservationValid(player.serverLevel(), reservation);
	}

	static void releaseForCleric(Villager cleric) {
		if (cleric != null)
			releaseForCleric(cleric.getUUID());
	}

	static boolean commitSuccessfulHeal(ServerPlayer player, Villager cleric) {
		if (player == null || cleric == null || !(cleric.level() instanceof ServerLevel level))
			return false;
		Reservation reservation = BY_CLERIC.get(cleric.getUUID());
		if (reservation == null
				|| reservation.healCommitted()
				|| !reservation.playerId().equals(player.getUUID())
				|| !isReservationPairUsable(level, reservation, player, cleric)
				|| !ConfessionClericHealingGoal.isSafeToContinueCommittedCast(cleric)) {
			return false;
		}

		reservation.commitHeal();
		player.getPersistentData().putLong(
				ConfessionData.HEAL_COOLDOWN_UNTIL_TAG,
				player.level().getGameTime() + ConfessionData.HEAL_COOLDOWN_TICKS
		);
		return true;
	}

	static boolean completeCommittedHeal(ServerPlayer player, Villager cleric) {
		if (player == null || cleric == null)
			return false;
		Reservation reservation = BY_CLERIC.get(cleric.getUUID());
		if (reservation == null
				|| !reservation.healCommitted()
				|| !reservation.playerId().equals(player.getUUID())
				|| BY_PLAYER.get(player.getUUID()) != reservation) {
			return false;
		}
		release(reservation);
		return true;
	}

	static boolean isEligiblePlayer(ServerPlayer player) {
		return player != null
				&& player.isAlive()
				&& !player.isDeadOrDying()
				&& !player.isRemoved()
				&& !player.isSpectator()
				&& player.getHealth() < player.getMaxHealth()
				&& player.getHealth() <= player.getMaxHealth() * ConfessionData.CANDIDATE_HEALTH_RATIO
				&& !hasCooldown(player)
				&& HolyRosariumHelper.hasActiveCombination(
						player,
						HolyRosariumBead.HUMILITY,
						HolyRosariumBead.SACRAMENT
				);
	}

	static boolean isEmergency(ServerPlayer player) {
		return player != null
				&& player.getHealth() <= player.getMaxHealth() * ConfessionData.EMERGENCY_HEALTH_RATIO;
	}

	static boolean hasCooldown(ServerPlayer player) {
		return player != null
				&& player.level().getGameTime()
				< player.getPersistentData().getLong(ConfessionData.HEAL_COOLDOWN_UNTIL_TAG);
	}

	private static void evaluateCandidate(ServerPlayer player) {
		Reservation existing = BY_PLAYER.get(player.getUUID());
		if (existing != null) {
			if (isReservationValid(player.serverLevel(), existing))
				return;
			release(existing);
		}

		if (!isEligiblePlayer(player))
			return;

		boolean emergency = isEmergency(player);
		AABB searchArea = player.getBoundingBox().inflate(ConfessionData.CLERIC_SEARCH_RADIUS);
		Villager cleric = player.serverLevel().getEntitiesOfClass(Villager.class, searchArea, candidate ->
				!BY_CLERIC.containsKey(candidate.getUUID())
						&& ConfessionClericHealingGoal.isEligibleForAssignment(candidate, emergency)
		).stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
		if (cleric == null)
			return;

		Reservation reservation = new Reservation(
				player.getUUID(),
				cleric.getUUID(),
				player.level().getGameTime() + ConfessionData.RESERVATION_TIMEOUT_TICKS
		);
		BY_PLAYER.put(reservation.playerId(), reservation);
		BY_CLERIC.put(reservation.clericId(), reservation);
	}

	private static boolean isReservationValid(ServerLevel level, Reservation reservation) {
		if (level == null || reservation == null || level.getGameTime() >= reservation.expiresAt()
				|| BY_PLAYER.get(reservation.playerId()) != reservation
				|| BY_CLERIC.get(reservation.clericId()) != reservation) {
			return false;
		}
		ServerPlayer player = level.getPlayerByUUID(reservation.playerId()) instanceof ServerPlayer serverPlayer
				? serverPlayer
				: null;
		Entity entity = level.getEntity(reservation.clericId());
		if (!(entity instanceof Villager cleric) || !isReservationPairUsable(level, reservation, player, cleric))
			return false;

		if (reservation.healCommitted())
			return ConfessionClericHealingGoal.isSafeToContinueCommittedCast(cleric);
		return isEligiblePlayer(player)
				&& ConfessionClericHealingGoal.isEligibleForAssignment(cleric, isEmergency(player));
	}

	private static boolean isReservationPairUsable(
			ServerLevel level,
			Reservation reservation,
			ServerPlayer player,
			Villager cleric
	) {
		return player != null
				&& cleric != null
				&& player.level() == level
				&& cleric.level() == level
				&& player.isAlive()
				&& !player.isDeadOrDying()
				&& !player.isRemoved()
				&& cleric.isAlive()
				&& !cleric.isDeadOrDying()
				&& !cleric.isRemoved()
				&& player.getUUID().equals(reservation.playerId())
				&& cleric.getUUID().equals(reservation.clericId())
				&& level.getGameTime() < reservation.expiresAt()
				&& BY_PLAYER.get(reservation.playerId()) == reservation
				&& BY_CLERIC.get(reservation.clericId()) == reservation
				&& player.distanceToSqr(cleric) <= ConfessionData.MAX_RESERVATION_DISTANCE_SQR;
	}

	private static void releaseForEntity(Entity entity) {
		if (entity instanceof ServerPlayer player)
			releaseForPlayer(player.getUUID());
		else if (entity instanceof Villager villager)
			releaseForCleric(villager.getUUID());
	}

	private static void releaseForPlayer(UUID playerId) {
		Reservation reservation = BY_PLAYER.get(playerId);
		if (reservation != null)
			release(reservation);
	}

	private static void releaseForCleric(UUID clericId) {
		Reservation reservation = BY_CLERIC.get(clericId);
		if (reservation != null)
			release(reservation);
	}

	private static void release(Reservation reservation) {
		BY_PLAYER.remove(reservation.playerId(), reservation);
		BY_CLERIC.remove(reservation.clericId(), reservation);
	}

	private static final class Reservation {
		private final UUID playerId;
		private final UUID clericId;
		private final long expiresAt;
		private boolean healCommitted;

		private Reservation(UUID playerId, UUID clericId, long expiresAt) {
			this.playerId = playerId;
			this.clericId = clericId;
			this.expiresAt = expiresAt;
		}

		private UUID playerId() {
			return this.playerId;
		}

		private UUID clericId() {
			return this.clericId;
		}

		private long expiresAt() {
			return this.expiresAt;
		}

		private boolean healCommitted() {
			return this.healCommitted;
		}

		private void commitHeal() {
			this.healCommitted = true;
		}
	}
}
