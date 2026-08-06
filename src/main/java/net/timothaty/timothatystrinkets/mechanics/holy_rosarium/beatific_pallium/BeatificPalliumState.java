package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.beatific_pallium;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.entity.BeatificPalliumEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModEntities;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.network.BeatificPalliumImpactMessage;
import net.timothaty.timothatystrinkets.network.BeatificPalliumShatterMessage;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class BeatificPalliumState {
	private static final int SHATTER_RGB = 0xFFE36A;
	private static final Map<UUID, Session> SESSIONS = new HashMap<>();
	private static final List<Session> SESSION_TICK_BUFFER = new ArrayList<>();

	private BeatificPalliumState() {
	}

	public static boolean activate(ServerPlayer caster, LivingEntity target) {
		if (caster == null || target == null || !(target.level() instanceof ServerLevel level)
				|| !target.isAlive() || target.isDeadOrDying() || target.isRemoved()
				|| SESSIONS.containsKey(target.getUUID())
				|| target.hasEffect(TimothatysTrinketsModMobEffects.BEATIFIC_PALLIUM)) {
			return false;
		}

		BeatificPalliumEntity visual = new BeatificPalliumEntity(TimothatysTrinketsModEntities.BEATIFIC_PALLIUM.get(), level);
		visual.configure(target);
		if (!level.addFreshEntity(visual))
			return false;

		long start = level.getGameTime();
		Session session = new Session(
				target.getUUID(),
				caster.getUUID(),
				level.dimension(),
				visual.getUUID(),
				visual.getId(),
				start
		);
		SESSIONS.put(target.getUUID(), session);
		MobEffectInstance effect = new MobEffectInstance(
				TimothatysTrinketsModMobEffects.BEATIFIC_PALLIUM,
				BeatificPalliumData.DURATION_TICKS,
				0,
				false,
				false,
				true
		);
		if (!target.addEffect(effect, caster)) {
			SESSIONS.remove(target.getUUID(), session);
			visual.discard();
			return false;
		}
		session.lastKnownEffectDuration = BeatificPalliumData.DURATION_TICKS;
		return true;
	}

	static Session activeSession(LivingEntity target) {
		if (target == null)
			return null;
		Session session = SESSIONS.get(target.getUUID());
		return session != null
				&& session.phase == Phase.ACTIVE
				&& target.hasEffect(TimothatysTrinketsModMobEffects.BEATIFIC_PALLIUM)
				? session
				: null;
	}

	static BeatificPalliumEntity visualEntity(ServerLevel level, Session session) {
		if (level == null || session == null)
			return null;
		Entity entity = level.getEntity(session.visualUuid);
		return entity instanceof BeatificPalliumEntity pallium ? pallium : null;
	}

	static void recordImpact(ServerLevel level, Session session, BeatificPalliumImpactGeometry.Result geometry,
			float absorbedDamage, int seed) {
		if (level == null || session == null || geometry == null || absorbedDamage <= 0.0F
				|| session.phase != Phase.ACTIVE) {
			return;
		}

		long gameTime = level.getGameTime();
		if (session.hasPendingImpact) {
			aggregatePendingImpact(session, geometry, absorbedDamage, seed);
			if (gameTime >= session.nextImpactVfxGameTime)
				flushPendingImpact(level, session, false);
			return;
		}

		if (gameTime >= session.nextImpactVfxGameTime) {
			sendImpact(level, session, geometry.face(), geometry.u(), geometry.v(), absorbedDamage, seed);
			session.nextImpactVfxGameTime = gameTime + BeatificPalliumData.IMPACT_VFX_THROTTLE_TICKS;
			return;
		}

		aggregatePendingImpact(session, geometry, absorbedDamage, seed);
	}

	private static void aggregatePendingImpact(Session session, BeatificPalliumImpactGeometry.Result geometry,
			float absorbedDamage, int seed) {
		session.pendingImpactDamage += absorbedDamage;
		if (!session.hasPendingImpact || absorbedDamage >= session.pendingLargestImpactDamage) {
			session.pendingImpactFace = geometry.face();
			session.pendingImpactU = geometry.u();
			session.pendingImpactV = geometry.v();
			session.pendingImpactSeed = seed;
			session.pendingLargestImpactDamage = absorbedDamage;
		}
		session.hasPendingImpact = true;
	}

	private static void flushPendingImpact(ServerLevel level, Session session, boolean force) {
		if (!session.hasPendingImpact)
			return;
		long gameTime = level.getGameTime();
		if (!force && gameTime < session.nextImpactVfxGameTime)
			return;

		sendImpact(
				level,
				session,
				session.pendingImpactFace,
				session.pendingImpactU,
				session.pendingImpactV,
				session.pendingImpactDamage,
				session.pendingImpactSeed
		);
		clearPendingImpact(session);
		session.nextImpactVfxGameTime = gameTime + BeatificPalliumData.IMPACT_VFX_THROTTLE_TICKS;
	}

	private static void sendImpact(ServerLevel level, Session session, int face, float u, float v,
			float absorbedDamage, int seed) {
		BeatificPalliumEntity visual = visualEntity(level, session);
		if (visual == null || visual.isRemoved())
			return;
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(
				visual,
				new BeatificPalliumImpactMessage(
						visual.getId(),
						face,
						u,
						v,
						absorbedDamage,
						seed
				)
		);
	}

	private static void clearPendingImpact(Session session) {
		session.hasPendingImpact = false;
		session.pendingImpactDamage = 0.0F;
		session.pendingLargestImpactDamage = 0.0F;
		session.pendingImpactFace = 0;
		session.pendingImpactU = 0.0F;
		session.pendingImpactV = 0.0F;
		session.pendingImpactSeed = 0;
	}

	static void beginBurst(ServerLevel level, Session session, LivingEntity protectedTarget) {
		if (level == null || session == null || protectedTarget == null || session.phase != Phase.ACTIVE)
			return;

		BeatificPalliumEntity visual = visualEntity(level, session);
		if (visual == null) {
			invalidate(level, session, protectedTarget);
			return;
		}

		flushPendingImpact(level, session, true);
		session.phase = Phase.BURST;
		session.phaseStartGameTime = level.getGameTime();
		session.finalExplosionDamage = session.accumulatedDamage * BeatificPalliumData.EXPLOSION_MULTIPLIER;
		Vec3 origin = new Vec3(
				protectedTarget.getX(),
				protectedTarget.getY() + protectedTarget.getBbHeight() * 0.5D,
				protectedTarget.getZ()
		);
		session.wave = new BeatificPalliumExplosionWave(origin, session.finalExplosionDamage, session.phaseStartGameTime);
		visual.setVisualPhase(BeatificPalliumEntity.VisualPhase.BURST);
		Vec3 shatterOrigin = new Vec3(
				protectedTarget.getX(),
				protectedTarget.getY() + protectedTarget.getBbHeight() * BeatificPalliumData.VISUAL_CENTER_HEIGHT_FACTOR,
				protectedTarget.getZ()
		);
		Vec3 inheritedVelocity = protectedTarget.getDeltaMovement();
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(
				visual,
				new BeatificPalliumShatterMessage(
						visual.getId(),
						shatterOrigin.x,
						shatterOrigin.y,
						shatterOrigin.z,
						protectedTarget.yBodyRot,
						(float) inheritedVelocity.x,
						(float) inheritedVelocity.y,
						(float) inheritedVelocity.z,
						SHATTER_RGB,
						protectedTarget.getRandom().nextInt()
				)
		);

		level.playSound(
				null,
				origin.x,
				origin.y,
				origin.z,
				TimothatysTrinketsModSounds.BEATIFIC_PALLIUM_EXPLOSION.get(),
				SoundSource.PLAYERS,
				1.0F,
				1.0F
		);
		level.sendParticles(
				TimothatysTrinketsModParticleTypes.BEATIFIC_PALLIUM_EXPLOSION_RING.get(),
				origin.x,
				origin.y,
				origin.z,
				1,
				0.0D,
				0.0D,
				0.0D,
				0.0D
		);
	}

	static void endFromCapacity(ServerLevel level, Session session, LivingEntity protectedTarget) {
		beginBurst(level, session, protectedTarget);
		if (session != null && session.phase == Phase.BURST
				&& protectedTarget.hasEffect(TimothatysTrinketsModMobEffects.BEATIFIC_PALLIUM)) {
			protectedTarget.removeEffect(TimothatysTrinketsModMobEffects.BEATIFIC_PALLIUM);
		}
	}

	private static void beginFade(ServerLevel level, Session session, boolean discardAccumulatedDamage) {
		if (session.phase != Phase.ACTIVE)
			return;
		clearPendingImpact(session);
		if (discardAccumulatedDamage) {
			session.accumulatedDamage = 0.0F;
			session.finalExplosionDamage = 0.0F;
			session.wave = null;
		}
		session.phase = Phase.FADING;
		session.phaseStartGameTime = level.getGameTime();
		BeatificPalliumEntity visual = visualEntity(level, session);
		if (visual == null) {
			invalidate(level, session, null);
		} else {
			visual.setVisualPhase(BeatificPalliumEntity.VisualPhase.FADING);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onPalliumRemoved(MobEffectEvent.Remove event) {
		if (event.isCanceled()
				|| event.getEffect().value() != TimothatysTrinketsModMobEffects.BEATIFIC_PALLIUM.get()
				|| !(event.getEntity().level() instanceof ServerLevel level)) {
			return;
		}

		Session session = SESSIONS.get(event.getEntity().getUUID());
		if (session != null && session.phase == Phase.ACTIVE)
			beginFade(level, session, true);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onPalliumExpired(MobEffectEvent.Expired event) {
		MobEffectInstance effect = event.getEffectInstance();
		if (event.isCanceled() || effect == null
				|| effect.getEffect().value() != TimothatysTrinketsModMobEffects.BEATIFIC_PALLIUM.get()
				|| !(event.getEntity().level() instanceof ServerLevel level)) {
			return;
		}

		Session session = SESSIONS.get(event.getEntity().getUUID());
		if (session == null || session.phase != Phase.ACTIVE)
			return;
		if (session.accumulatedDamage > 0.0F)
			beginBurst(level, session, event.getEntity());
		else
			beginFade(level, session, false);
	}

	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent.Post event) {
		if (!(event.getLevel() instanceof ServerLevel level) || SESSIONS.isEmpty())
			return;

		try {
			for (Session session : SESSIONS.values()) {
				if (session.dimension.equals(level.dimension()))
					SESSION_TICK_BUFFER.add(session);
			}
			for (Session session : SESSION_TICK_BUFFER) {
				if (SESSIONS.get(session.targetUuid) == session)
					tickSession(level, session);
			}
		} finally {
			SESSION_TICK_BUFFER.clear();
		}
	}

	private static void tickSession(ServerLevel level, Session session) {
		Entity resolvedTarget = level.getEntity(session.targetUuid);
		if (!(resolvedTarget instanceof LivingEntity target) || !target.isAlive() || target.isDeadOrDying() || target.isRemoved()) {
			invalidate(level, session, resolvedTarget instanceof LivingEntity living ? living : null);
			return;
		}

		BeatificPalliumEntity visual = visualEntity(level, session);
		if (visual == null || visual.isRemoved()) {
			invalidate(level, session, target);
			return;
		}

		long gameTime = level.getGameTime();
		if (session.phase == Phase.ACTIVE) {
			flushPendingImpact(level, session, false);
			MobEffectInstance effect = target.getEffect(TimothatysTrinketsModMobEffects.BEATIFIC_PALLIUM);
			if (effect == null) {
				if (session.lastKnownEffectDuration <= 1 && session.accumulatedDamage > 0.0F)
					beginBurst(level, session, target);
				else
					beginFade(level, session, true);
				return;
			}
			session.lastKnownEffectDuration = effect.getDuration();
			if (gameTime - session.startGameTime >= BeatificPalliumData.APPEARANCE_TICKS
					&& visual.getVisualPhase() == BeatificPalliumEntity.VisualPhase.APPEARING) {
				visual.setVisualPhase(BeatificPalliumEntity.VisualPhase.LOOP);
			}
			return;
		}

		if (session.phase == Phase.FADING) {
			if (gameTime - session.phaseStartGameTime >= BeatificPalliumData.FADE_TICKS)
				finishCleanup(level, session);
			return;
		}

		ServerPlayer caster = level.getServer().getPlayerList().getPlayer(session.casterUuid);
		boolean waveFinished = session.wave == null
				|| session.wave.tick(level, visual, caster, session.targetUuid, session.casterUuid);
		if (waveFinished)
			finishCleanup(level, session);
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		if (!event.getEntity().level().isClientSide())
			cleanupMatching(event.getEntity().getUUID(), event.getEntity().level().getServer(), event.getEntity());
	}

	@SubscribeEvent
	public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
		if (!event.getLevel().isClientSide() && !(event.getEntity() instanceof BeatificPalliumEntity))
			cleanupMatching(
					event.getEntity().getUUID(),
					event.getLevel().getServer(),
					event.getEntity() instanceof LivingEntity living ? living : null
			);
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		cleanupMatching(event.getEntity().getUUID(), event.getEntity().getServer(), event.getEntity());
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		cleanupMatching(event.getEntity().getUUID(), event.getEntity().getServer(), event.getEntity());
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		MinecraftServer server = event.getServer();
		for (Session session : SESSIONS.values()) {
			ServerLevel level = server.getLevel(session.dimension);
			if (level != null)
				discardVisual(level, session);
		}
		SESSIONS.clear();
	}

	private static void cleanupMatching(UUID entityUuid, MinecraftServer server, LivingEntity departingTarget) {
		if (entityUuid == null || server == null)
			return;
		Iterator<Session> iterator = SESSIONS.values().iterator();
		while (iterator.hasNext()) {
			Session session = iterator.next();
			if (!entityUuid.equals(session.targetUuid) && !entityUuid.equals(session.casterUuid))
				continue;
			iterator.remove();
			ServerLevel level = server.getLevel(session.dimension);
			if (level != null) {
				LivingEntity target = entityUuid.equals(session.targetUuid) && departingTarget != null
						? departingTarget
						: resolveTarget(level, session);
				finishInvalidation(level, session, target);
			} else {
				session.phase = Phase.CLEANUP;
				clearPendingImpact(session);
			}
		}
	}

	private static LivingEntity resolveTarget(ServerLevel level, Session session) {
		Entity target = level.getEntity(session.targetUuid);
		return target instanceof LivingEntity living ? living : null;
	}

	private static void invalidate(ServerLevel level, Session session, LivingEntity target) {
		if (!SESSIONS.remove(session.targetUuid, session))
			return;
		finishInvalidation(level, session, target);
	}

	private static void finishInvalidation(ServerLevel level, Session session, LivingEntity target) {
		session.phase = Phase.CLEANUP;
		clearPendingImpact(session);
		session.accumulatedDamage = 0.0F;
		session.wave = null;
		if (target != null && target.hasEffect(TimothatysTrinketsModMobEffects.BEATIFIC_PALLIUM))
			target.removeEffect(TimothatysTrinketsModMobEffects.BEATIFIC_PALLIUM);
		discardVisual(level, session);
	}

	private static void finishCleanup(ServerLevel level, Session session) {
		if (!SESSIONS.remove(session.targetUuid, session))
			return;
		session.phase = Phase.CLEANUP;
		clearPendingImpact(session);
		discardVisual(level, session);
	}

	private static void discardVisual(ServerLevel level, Session session) {
		BeatificPalliumEntity visual = visualEntity(level, session);
		if (visual != null)
			visual.discard();
	}

	static final class Session {
		final UUID targetUuid;
		final UUID casterUuid;
		final ResourceKey<Level> dimension;
		final UUID visualUuid;
		final int visualEntityId;
		final long startGameTime;
		int lastKnownEffectDuration;
		float accumulatedDamage;
		float finalExplosionDamage;
		long phaseStartGameTime;
		long lastImpactSoundGameTime = Long.MIN_VALUE;
		long nextImpactVfxGameTime = Long.MIN_VALUE;
		boolean hasPendingImpact;
		float pendingImpactDamage;
		float pendingLargestImpactDamage;
		int pendingImpactFace;
		float pendingImpactU;
		float pendingImpactV;
		int pendingImpactSeed;
		Phase phase = Phase.ACTIVE;
		BeatificPalliumExplosionWave wave;

		private Session(UUID targetUuid, UUID casterUuid, ResourceKey<Level> dimension, UUID visualUuid,
				int visualEntityId, long startGameTime) {
			this.targetUuid = targetUuid;
			this.casterUuid = casterUuid;
			this.dimension = dimension;
			this.visualUuid = visualUuid;
			this.visualEntityId = visualEntityId;
			this.startGameTime = startGameTime;
			this.phaseStartGameTime = startGameTime;
		}

		float remainingCapacity() {
			return Math.max(0.0F, BeatificPalliumData.CAPACITY - this.accumulatedDamage);
		}

		void addAbsorbed(float amount) {
			this.accumulatedDamage = Math.min(BeatificPalliumData.CAPACITY, this.accumulatedDamage + Math.max(0.0F, amount));
		}
	}

	private enum Phase {
		ACTIVE,
		FADING,
		BURST,
		CLEANUP
	}
}
