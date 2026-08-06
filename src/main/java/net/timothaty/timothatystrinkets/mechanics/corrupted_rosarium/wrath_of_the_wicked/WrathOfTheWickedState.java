package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.wrath_of_the_wicked;

import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumCombination;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumHelper;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumState;
import net.timothaty.timothatystrinkets.network.WrathOfTheWickedVisualStateMessage;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.network.PacketDistributor;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class WrathOfTheWickedState {
	private static final Map<UUID, WrathOfTheWickedSession> SESSIONS = new HashMap<>();
	private static final long NO_AURA_TICK = Long.MIN_VALUE;
	private static final int AURA_INTERVAL_TICKS = 4;

	private WrathOfTheWickedState() {
	}

	public static boolean start(
			ServerPlayer player,
			int rosariumRevision,
			SlotContext sourceSlot,
			boolean venomSphereSynergy
	) {
		if (player == null
				|| sourceSlot == null
				|| sourceSlot.cosmetic()
				|| SESSIONS.containsKey(player.getUUID()))
			return false;

		WrathOfTheWickedSession session = new WrathOfTheWickedSession(
				player.level().dimension(),
				player.level().getGameTime(),
				rosariumRevision,
				sourceSlot.identifier(),
				sourceSlot.index(),
				venomSphereSynergy
		);
		SESSIONS.put(player.getUUID(), session);
		player.stopUsingItem();
		applyModifier(
				player,
				Attributes.MOVEMENT_SPEED,
				WrathOfTheWickedData.MOVEMENT_SPEED_MODIFIER_ID,
				WrathOfTheWickedData.MOVEMENT_SPEED_MULTIPLIER,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
		);
		applyModifier(
				player,
				Attributes.KNOCKBACK_RESISTANCE,
				WrathOfTheWickedData.KNOCKBACK_RESISTANCE_MODIFIER_ID,
				WrathOfTheWickedData.KNOCKBACK_RESISTANCE_BONUS,
				AttributeModifier.Operation.ADD_VALUE
		);
		sync(player, session, true);
		return true;
	}

	public static void enableFlamingEmberSynergy(ServerPlayer player) {
		WrathOfTheWickedSession session = player == null
				? null
				: SESSIONS.get(player.getUUID());
		if (session != null)
			session.enableFlamingEmberSynergy();
	}

	public static boolean isActive(LivingEntity entity) {
		return entity != null && SESSIONS.containsKey(entity.getUUID());
	}

	public static boolean matchesSourceSlot(
			LivingEntity entity,
			String slotIdentifier,
			int slotIndex
	) {
		if (entity == null || slotIdentifier == null)
			return false;
		WrathOfTheWickedSession session = SESSIONS.get(entity.getUUID());
		return session != null && session.matchesSourceSlot(slotIdentifier, slotIndex);
	}

	public static void tick(ServerPlayer player) {
		WrathOfTheWickedSession session = player == null ? null : SESSIONS.get(player.getUUID());
		if (session == null)
			return;
		if (TimothatysTrinketsStunHelper.isStunned(player)
				|| TimothatysTrinketsStunHelper.isStaggered(player)) {
			finish(player, true);
			return;
		}
		if (!isSessionValid(player, session)) {
			finish(player, true);
			return;
		}

		long elapsedLong = player.level().getGameTime() - session.startGameTime();
		if (elapsedLong < 0L) {
			finish(player, true);
			return;
		}
		int elapsed = (int) Math.min(Integer.MAX_VALUE, elapsedLong);
		if (elapsed >= WrathOfTheWickedData.DURATION_TICKS) {
			finish(player, true);
			return;
		}

		if (!session.rotationLocked()
				&& elapsed >= WrathOfTheWickedData.ANCHOR_START_TICK) {
			session.lockRotation(player.getYRot());
			removeModifier(
					player,
					Attributes.MOVEMENT_SPEED,
					WrathOfTheWickedData.MOVEMENT_SPEED_MODIFIER_ID
			);
			sync(player, session, true);
		}

		tickMovement(player, session, elapsed);
		tickCombat(player, session, elapsed);
		if (elapsed >= WrathOfTheWickedData.ANCHOR_END_TICK) {
			releaseMovement(player, session);
		}
	}

	public static void interrupt(LivingEntity entity) {
		if (entity instanceof ServerPlayer player)
			finish(player, player.isAlive() && !player.isDeadOrDying());
	}

	public static void finish(ServerPlayer player, boolean applyWeakness) {
		if (player == null)
			return;
		WrathOfTheWickedSession session = SESSIONS.remove(player.getUUID());
		if (session == null)
			return;

		releaseMovement(player, session);
		sync(player, session, false);
		if (applyWeakness && player.isAlive() && !player.isDeadOrDying()) {
			player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WrathOfTheWickedData.WEAKNESS_TICKS, 0));
		}
	}

	public static void syncTo(ServerPlayer trackingPlayer, ServerPlayer targetPlayer) {
		if (trackingPlayer == null || targetPlayer == null)
			return;
		WrathOfTheWickedSession session = SESSIONS.get(targetPlayer.getUUID());
		if (session != null) {
			PacketDistributor.sendToPlayer(
					trackingPlayer,
					visualMessage(targetPlayer, session, true)
			);
		} else {
			PacketDistributor.sendToPlayer(
					trackingPlayer,
					new WrathOfTheWickedVisualStateMessage(
							targetPlayer.getId(),
							targetPlayer.level().getGameTime(),
							targetPlayer.getYRot(),
							false,
							false
					)
			);
		}
	}

	public static void stopAll(Iterable<ServerPlayer> players) {
		if (players != null) {
			for (ServerPlayer player : players) {
				finish(player, player.isAlive() && !player.isDeadOrDying());
			}
		}
		SESSIONS.clear();
	}

	private static boolean isSessionValid(ServerPlayer player, WrathOfTheWickedSession session) {
		if (!player.isAlive()
				|| player.isDeadOrDying()
				|| player.isRemoved()
				|| player.isSpectator()
				|| !player.level().dimension().equals(session.dimension())) {
			return false;
		}
		SlotResult activeRosarium = CorruptedRosariumHelper.findActiveRosariumResult(player).orElse(null);
		if (activeRosarium == null
				|| !session.matchesSourceSlot(
						activeRosarium.slotContext().identifier(),
						activeRosarium.slotContext().index()
				)) {
			return false;
		}
		CorruptedRosariumState state = CorruptedRosariumState.get(player);
		return state != null
				&& state.revision() == session.rosariumRevision()
				&& state.hasCombination(CorruptedRosariumCombination.WRATH_OF_THE_WICKED);
	}

	private static void tickMovement(ServerPlayer player, WrathOfTheWickedSession session, int elapsed) {
		if (elapsed >= WrathOfTheWickedData.DECELERATION_START_TICK
				&& elapsed < WrathOfTheWickedData.ANCHOR_START_TICK) {
			Vec3 sourceVelocity = session.decelerationVelocity();
			if (sourceVelocity == null) {
				sourceVelocity = player.getDeltaMovement();
				session.setDecelerationVelocity(sourceVelocity);
			}
			double remaining = (WrathOfTheWickedData.ANCHOR_START_TICK - elapsed)
					/ (double) (WrathOfTheWickedData.ANCHOR_START_TICK
							- WrathOfTheWickedData.DECELERATION_START_TICK);
			player.setDeltaMovement(
					sourceVelocity.x * remaining,
					player.getDeltaMovement().y,
					sourceVelocity.z * remaining
			);
			player.hurtMarked = true;
		}

		if (elapsed >= WrathOfTheWickedData.ANCHOR_START_TICK
				&& elapsed < WrathOfTheWickedData.ANCHOR_END_TICK) {
			if (!session.anchorApplied()) {
				session.applyAnchor(player.position(), player.isNoGravity());
				player.setNoGravity(true);
			}
			Vec3 anchor = session.anchor();
			player.setDeltaMovement(Vec3.ZERO);
			player.fallDistance = 0.0F;
			if (anchor != null)
				player.setPos(anchor.x, anchor.y, anchor.z);
			player.hurtMarked = true;
		}
	}

	private static void tickCombat(ServerPlayer player, WrathOfTheWickedSession session, int elapsed) {
		if (elapsed >= WrathOfTheWickedData.PULSE_SERVER_START_TICK
				&& elapsed <= Math.ceil(
						WrathOfTheWickedData.PULSE_VISUAL_START_TICK
								+ WrathOfTheWickedData.PULSE_DURATION_TICKS
				)) {
			if (session.pulseOrigin() == null)
				session.setPulseOrigin(player.position());
			double previousRadius = WrathOfTheWickedData.pulseRadius(elapsed - 1.0F);
			double currentRadius = WrathOfTheWickedData.pulseRadius(elapsed);
			WrathOfTheWickedCombat.applyPulseRing(
					player,
					session,
					previousRadius,
					currentRadius
			);
		}

		while (session.nextLaserStage() < WrathOfTheWickedData.LASER_STAGE_COUNT
				&& elapsed >= WrathOfTheWickedData.ANCHOR_START_TICK
						+ session.nextLaserStage() * WrathOfTheWickedData.LASER_STAGE_INTERVAL_TICKS) {
			int stage = session.nextLaserStage();
			session.advanceLaserStage();
			WrathOfTheWickedCombat.applyLaserStage(player, session, stage);
		}

		while (session.flamingEmberSynergy()
				&& session.nextFireWaveIndex() < WrathOfTheWickedData.fireWaveCount()
				&& elapsed >= WrathOfTheWickedData.fireWaveTick(
						session.nextFireWaveIndex()
				)) {
			int waveStartTick = WrathOfTheWickedData.fireWaveTick(
					session.nextFireWaveIndex()
			);
			session.advanceFireWave();
			WrathOfTheWickedSession.FireWave wave = session.startFireWave(
					waveStartTick,
					player.position()
			);
			WrathOfTheWickedParticles.emitFireWave(player, wave.origin());
		}
		tickFireWaves(player, session, elapsed);

		long gameTime = player.level().getGameTime();
		if (session.lastAuraTick() == NO_AURA_TICK
				|| gameTime - session.lastAuraTick() >= AURA_INTERVAL_TICKS) {
			session.setLastAuraTick(gameTime);
			WrathOfTheWickedParticles.emitAura(player);
		}
	}

	private static void tickFireWaves(
			ServerPlayer player,
			WrathOfTheWickedSession session,
			int elapsed
	) {
		Iterator<WrathOfTheWickedSession.FireWave> iterator =
				session.activeFireWaves().iterator();
		while (iterator.hasNext()) {
			WrathOfTheWickedSession.FireWave wave = iterator.next();
			int age = elapsed - wave.startElapsedTick();
			if (age < 0)
				continue;

			double currentRadius = WrathOfTheWickedData.fireWaveRadius(age);
			WrathOfTheWickedCombat.applyFireWaveRing(
					player,
					wave,
					wave.processedRadius(),
					currentRadius
			);
			wave.setProcessedRadius(currentRadius);
			if (age >= WrathOfTheWickedData.FIRE_WAVE_SPREAD_TICKS)
				iterator.remove();
		}
	}

	private static void releaseMovement(ServerPlayer player, WrathOfTheWickedSession session) {
		if (session.movementReleased())
			return;
		session.markMovementReleased();
		if (session.anchorApplied()) {
			player.setNoGravity(session.previousNoGravity());
			session.clearAnchor();
		}
		removeModifier(player, Attributes.MOVEMENT_SPEED, WrathOfTheWickedData.MOVEMENT_SPEED_MODIFIER_ID);
		removeModifier(
				player,
				Attributes.KNOCKBACK_RESISTANCE,
				WrathOfTheWickedData.KNOCKBACK_RESISTANCE_MODIFIER_ID
		);
	}

	private static void applyModifier(
			ServerPlayer player,
			net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
			net.minecraft.resources.ResourceLocation id,
			double amount,
			AttributeModifier.Operation operation
	) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null)
			return;
		instance.removeModifier(id);
		instance.addTransientModifier(new AttributeModifier(id, amount, operation));
	}

	private static void removeModifier(
			ServerPlayer player,
			net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
			net.minecraft.resources.ResourceLocation id
	) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance != null)
			instance.removeModifier(id);
	}

	private static void sync(ServerPlayer player, WrathOfTheWickedSession session, boolean active) {
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, visualMessage(player, session, active));
	}

	private static WrathOfTheWickedVisualStateMessage visualMessage(
			ServerPlayer player,
			WrathOfTheWickedSession session,
			boolean active
	) {
		return new WrathOfTheWickedVisualStateMessage(
				player.getId(),
				session.startGameTime(),
				session.initialYaw(),
				session.rotationLocked(),
				active
		);
	}
}
