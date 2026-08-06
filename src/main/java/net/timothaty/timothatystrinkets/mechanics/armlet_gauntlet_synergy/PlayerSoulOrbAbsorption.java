package net.timothaty.timothatystrinkets.mechanics.armlet_gauntlet_synergy;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.advancement.TimothatysTrinketsCriteriaTriggers;
import net.timothaty.timothatystrinkets.entity.SoulOrbEntity;
import net.timothaty.timothatystrinkets.entity.SoulOrbPullPhysics;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet.DuelistGuardState;
import net.timothaty.timothatystrinkets.network.SoulOrbAbsorptionPulseMessage;
import net.timothaty.timothatystrinkets.network.SoulOrbAbsorptionVisualStateMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class PlayerSoulOrbAbsorption {
	private static final Map<UUID, AbsorptionSession> SESSIONS = new HashMap<>();
	private static final int VISUAL_RESYNC_INTERVAL_TICKS = 30;
	private static final int EQUIPMENT_REVALIDATION_INTERVAL_TICKS = 5;
	private static final int LINE_OF_SIGHT_REVALIDATION_INTERVAL_TICKS = 3;
	private static final int HOLD_SOUND_INTERVAL_TICKS = 11;
	private static final double PARTIAL_ORB_RELEASE_SPEED = 0.08D;

	private PlayerSoulOrbAbsorption() {
	}

	public static void receiveHoldingState(ServerPlayer player, boolean holding) {
		if (player == null) {
			return;
		}
		if (!holding) {
			clearSession(player);
			return;
		}
		ArmletGauntletSynergyState.Snapshot snapshot = ArmletGauntletSynergyState.getOrRefresh(player);
		if (SESSIONS.containsKey(player.getUUID()) || !ArmletGauntletSynergyHelper.canChannel(player, snapshot)) {
			return;
		}

		SoulOrbEntity orb = ArmletGauntletSynergyHelper.findBestAvailableSoulOrbWithMinimumDot(
				player, ArmletGauntletSynergyData.ACQUIRE_MIN_DOT);
		if (orb == null || !orb.isAvailableForSoulAbsorption()) {
			return;
		}
		HumanoidArm activeArm = snapshot.physicalArm();
		InteractionHand activeHand = snapshot.interactionHand();
		int gauntletSlot = snapshot.gauntletSlot();
		if (activeArm == null || activeHand == null || gauntletSlot < 0) {
			return;
		}

		orb.setSoulAbsorptionTarget(player);
		if (!orb.isSoulAbsorbedBy(player)) {
			return;
		}
		AbsorptionSession session = new AbsorptionSession(
				orb.getUUID(), player.level().dimension(), activeArm, activeHand, gauntletSlot,
				snapshot.revision(), player.tickCount);
		SESSIONS.put(player.getUUID(), session);
		syncPullingState(player, true, activeArm);
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (event.getEntity() instanceof ServerPlayer player && SESSIONS.containsKey(player.getUUID())) {
			tickSession(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			clearSession(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			clearSession(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			clearSession(player);
		}
	}

	@SubscribeEvent
	public static void onStartTracking(PlayerEvent.StartTracking event) {
		if (!(event.getEntity() instanceof ServerPlayer trackingPlayer)
				|| !(event.getTarget() instanceof ServerPlayer targetPlayer)) {
			return;
		}

		AbsorptionSession session = SESSIONS.get(targetPlayer.getUUID());
		if (session != null) {
			PacketDistributor.sendToPlayer(trackingPlayer, visualStateMessage(targetPlayer, true, session.activeArm()));
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		SESSIONS.clear();
	}

	public static boolean isActivelyPulling(Player player) {
		return player != null && SESSIONS.containsKey(player.getUUID());
	}

	public static HumanoidArm getActivePullingArm(Player player) {
		if (player == null) {
			return null;
		}
		AbsorptionSession session = SESSIONS.get(player.getUUID());
		return session == null ? null : session.activeArm();
	}

	private static void tickSession(ServerPlayer player) {
		AbsorptionSession session = SESSIONS.get(player.getUUID());
		if (session == null || !passesCheapSessionChecks(player, session)) {
			clearSession(player);
			return;
		}

		ArmletGauntletSynergyState.Snapshot current = ArmletGauntletSynergyState.get(player);
		if (!matchesEquipment(session, current)) {
			clearSession(player);
			return;
		}
		if (player.tickCount - session.lastEquipmentRevalidationTick >= EQUIPMENT_REVALIDATION_INTERVAL_TICKS) {
			session.lastEquipmentRevalidationTick = player.tickCount;
			ArmletGauntletSynergyState.Snapshot refreshed = ArmletGauntletSynergyState.refreshFromCurios(player);
			if (!matchesEquipment(session, refreshed)) {
				clearSession(player);
				return;
			}
		}

		Entity entity = player.serverLevel().getEntity(session.orbUuid());
		if (!(entity instanceof SoulOrbEntity orb) || orb.isRemoved() || !orb.hasSoulValue()
				|| !orb.isSoulAbsorbedBy(player) || !isWithinRange(player, orb)) {
			clearSession(player);
			return;
		}
		if (player.tickCount - session.lastLineOfSightCheckTick >= LINE_OF_SIGHT_REVALIDATION_INTERVAL_TICKS) {
			session.lastLineOfSightCheckTick = player.tickCount;
			if (!ArmletGauntletSynergyHelper.isSoulOrbInViewWithMinimumDot(
					player, orb, ArmletGauntletSynergyData.RETAIN_MIN_DOT)) {
				clearSession(player);
				return;
			}
		}
		if (player.tickCount % VISUAL_RESYNC_INTERVAL_TICKS == 0) {
			syncPullingState(player, true, session.activeArm());
		}

		Vec3 hand = ArmletGauntletSynergyHelper.getHandPosition(player, session.activeArm());
		if (player.tickCount >= session.nextHoldSoundTick) {
			player.level().playSound(null, hand.x, hand.y, hand.z, TimothatysTrinketsModSounds.SOUL_EMPOWER_HOLD_LOOP.get(),
					SoundSource.PLAYERS, 1.0F, 1.0F);
			session.nextHoldSoundTick = player.tickCount + HOLD_SOUND_INTERVAL_TICKS;
		}
		if (SoulOrbPullPhysics.pullOrReached(orb, hand)) {
			collect(player, orb, hand);
		}
	}

	private static void collect(ServerPlayer player, SoulOrbEntity orb, Vec3 hand) {
		if (SoulEmpowerHelper.getLevel(player) >= ArmletGauntletSynergyData.MAX_SOUL_EMPOWER_LEVEL) {
			clearSession(player);
			return;
		}
		if (!orb.consumeOneSoulUnit()) {
			clearSession(player);
			return;
		}

		AbsorptionSession session = SESSIONS.remove(player.getUUID());
		if (session != null) {
			syncPullingState(player, false, session.activeArm());
		}
		orb.clearSoulAbsorptionTarget();
		if (orb.hasSoulValue()) {
			releasePartiallyConsumedOrb(player, orb, hand);
		} else {
			orb.discard();
		}
		player.level().playSound(null, hand.x, hand.y, hand.z, TimothatysTrinketsModSounds.SOUL_COLLECT.get(), SoundSource.PLAYERS, 0.9F,
				1.0F + player.getRandom().nextFloat() * 0.2F);
		player.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, hand.x, hand.y, hand.z, 10, 0.10D, 0.10D, 0.10D, 0.015D);
		SoulEmpowerHelper.addLevel(player);
		TimothatysTrinketsCriteriaTriggers.triggerAbsorbSoulOrbWithDuality(player);
		PacketDistributor.sendToPlayer(player, new SoulOrbAbsorptionPulseMessage());
	}

	private static boolean passesCheapSessionChecks(ServerPlayer player, AbsorptionSession session) {
		return player.isAlive() && !player.isDeadOrDying() && !player.isRemoved()
				&& !player.isSpectator() && !player.isSleeping() && !player.isUsingItem()
				&& !DuelistGuardState.isGuarding(player)
				&& SoulEmpowerHelper.getLevel(player) < ArmletGauntletSynergyData.MAX_SOUL_EMPOWER_LEVEL
				&& player.level().dimension().equals(session.dimension())
				&& session.activeHand() != null && player.getItemInHand(session.activeHand()).isEmpty();
	}

	private static boolean matchesEquipment(AbsorptionSession session, ArmletGauntletSynergyState.Snapshot snapshot) {
		return snapshot != null && snapshot.synergyActive()
				&& snapshot.revision() == session.equipmentRevision()
				&& snapshot.gauntletSlot() == session.gauntletSlot()
				&& snapshot.interactionHand() == session.activeHand()
				&& snapshot.physicalArm() == session.activeArm();
	}

	private static boolean isWithinRange(ServerPlayer player, SoulOrbEntity orb) {
		double dx = orb.getX() - player.getX();
		double dy = orb.getY() + orb.getBbHeight() * 0.5D - player.getEyeY();
		double dz = orb.getZ() - player.getZ();
		return dx * dx + dy * dy + dz * dz <= ArmletGauntletSynergyData.MAX_RANGE_SQR;
	}

	private static void releasePartiallyConsumedOrb(ServerPlayer player, SoulOrbEntity orb, Vec3 hand) {
		double dx = orb.getX() - hand.x;
		double dy = orb.getY() + orb.getBbHeight() * 0.5D - hand.y;
		double dz = orb.getZ() - hand.z;
		double distanceSqr = dx * dx + dy * dy + dz * dz;
		if (distanceSqr < 1.0E-6D) {
			double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
			dx = Math.cos(angle);
			dy = 0.25D;
			dz = Math.sin(angle);
			distanceSqr = dx * dx + dy * dy + dz * dz;
		}
		double scale = PARTIAL_ORB_RELEASE_SPEED / Math.sqrt(distanceSqr);
		orb.setDeltaMovement(dx * scale, Math.max(0.025D, dy * scale), dz * scale);
		orb.hurtMarked = true;
		orb.hasImpulse = true;
	}

	private static void clearSession(ServerPlayer player) {
		AbsorptionSession session = SESSIONS.remove(player.getUUID());
		if (session == null) {
			return;
		}
		syncPullingState(player, false, session.activeArm());

		MinecraftServer server = player.getServer();
		if (server == null) {
			return;
		}
		ServerLevel level = server.getLevel(session.dimension());
		if (level == null) {
			return;
		}
		Entity entity = level.getEntity(session.orbUuid());
		if (entity instanceof SoulOrbEntity orb && orb.isSoulAbsorbedBy(player.getUUID())) {
			orb.clearSoulAbsorptionTarget();
		}
	}

	private static void syncPullingState(ServerPlayer player, boolean pulling, HumanoidArm arm) {
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, visualStateMessage(player, pulling, arm));
	}

	private static SoulOrbAbsorptionVisualStateMessage visualStateMessage(ServerPlayer player, boolean pulling, HumanoidArm arm) {
		return new SoulOrbAbsorptionVisualStateMessage(player.getId(), pulling, arm == HumanoidArm.RIGHT);
	}

	private static final class AbsorptionSession {
		private final UUID orbUuid;
		private final ResourceKey<Level> dimension;
		private final HumanoidArm activeArm;
		private final InteractionHand activeHand;
		private final int gauntletSlot;
		private final int equipmentRevision;
		private int lastLineOfSightCheckTick;
		private int lastEquipmentRevalidationTick;
		private int nextHoldSoundTick;

		private AbsorptionSession(UUID orbUuid, ResourceKey<Level> dimension, HumanoidArm activeArm,
				InteractionHand activeHand, int gauntletSlot, int equipmentRevision, int startedTick) {
			this.orbUuid = orbUuid;
			this.dimension = dimension;
			this.activeArm = activeArm;
			this.activeHand = activeHand;
			this.gauntletSlot = gauntletSlot;
			this.equipmentRevision = equipmentRevision;
			this.lastLineOfSightCheckTick = startedTick;
			this.lastEquipmentRevalidationTick = startedTick;
			this.nextHoldSoundTick = startedTick;
		}

		private UUID orbUuid() { return orbUuid; }
		private ResourceKey<Level> dimension() { return dimension; }
		private HumanoidArm activeArm() { return activeArm; }
		private InteractionHand activeHand() { return activeHand; }
		private int gauntletSlot() { return gauntletSlot; }
		private int equipmentRevision() { return equipmentRevision; }
	}
}
