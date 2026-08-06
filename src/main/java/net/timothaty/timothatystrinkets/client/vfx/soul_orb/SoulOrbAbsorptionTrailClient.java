package net.timothaty.timothatystrinkets.client.vfx.soul_orb;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.vfx.spark.SparkTrailHandler;
import net.timothaty.timothatystrinkets.entity.SoulOrbEntity;
import net.timothaty.timothatystrinkets.entity.SoulOrbTrailStateDispatcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class SoulOrbAbsorptionTrailClient {
	private static final int TRAIL_RED = 0;
	private static final int TRAIL_GREEN = 167;
	private static final int TRAIL_BLUE = 145;
	private static final int MAX_MISSING_ENTITY_CHECKS = 3;
	private static final Set<Integer> ACTIVE_TRAIL_ORB_IDS = new HashSet<>();
	private static final Map<Integer, TrackedTrail> TRAILS = new HashMap<>();
	private static final Map<SparkTrailHandler.SparkTrail, Integer> FADING_TRAILS = new HashMap<>();
	private static ClientLevel trackedLevel;

	static {
		SoulOrbTrailStateDispatcher.registerClientListener(SoulOrbAbsorptionTrailClient::onTrailStateChanged);
	}

	private SoulOrbAbsorptionTrailClient() {
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			discardTrails();
			trackedLevel = null;
			return;
		}
		if (level != trackedLevel) {
			discardTrails();
			trackedLevel = level;
		}

		Iterator<Integer> iterator = ACTIVE_TRAIL_ORB_IDS.iterator();
		while (iterator.hasNext()) {
			int entityId = iterator.next();
			Entity entity = level.getEntity(entityId);
			TrackedTrail trackedTrail = TRAILS.get(entityId);
			if (!(entity instanceof SoulOrbEntity orb) || orb.isRemoved()) {
				if (trackedTrail == null || ++trackedTrail.missingEntityChecks >= MAX_MISSING_ENTITY_CHECKS) {
					iterator.remove();
					stopTrail(entityId);
				}
				continue;
			}
			if (!orb.hasSoulAbsorptionTrail()) {
				iterator.remove();
				stopTrail(entityId);
				continue;
			}

			if (trackedTrail != null) {
				trackedTrail.missingEntityChecks = 0;
			}
			recordTrail(orb);
		}
		tickFadingTrails();
	}

	@SubscribeEvent
	public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide() && event.getEntity() instanceof SoulOrbEntity orb && orb.hasSoulAbsorptionTrail()) {
			registerTrail(orb);
		}
	}

	@SubscribeEvent
	public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
		if (event.getLevel() == trackedLevel && event.getEntity() instanceof SoulOrbEntity orb) {
			unregisterTrail(orb.getId());
		}
	}

	@SubscribeEvent
	public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		discardTrails();
		trackedLevel = null;
	}

	public static void onTrailStateChanged(SoulOrbEntity orb) {
		if (!orb.level().isClientSide() || orb.level() != Minecraft.getInstance().level) {
			return;
		}
		if (orb.hasSoulAbsorptionTrail() && !orb.isRemoved()) {
			registerTrail(orb);
		} else {
			unregisterTrail(orb.getId());
		}
	}

	private static void registerTrail(SoulOrbEntity orb) {
		if (!(orb.level() instanceof ClientLevel level)) {
			return;
		}
		ClientLevel currentLevel = Minecraft.getInstance().level;
		if (currentLevel != null && level != currentLevel) {
			return;
		}
		if (level != trackedLevel) {
			discardTrails();
			trackedLevel = level;
		}
		ACTIVE_TRAIL_ORB_IDS.add(orb.getId());
		ensureTrail(orb);
	}

	private static void unregisterTrail(int entityId) {
		ACTIVE_TRAIL_ORB_IDS.remove(entityId);
		stopTrail(entityId);
	}

	private static void recordTrail(SoulOrbEntity orb) {
		Vec3 previous = trailPosition(orb, orb.xo, orb.yo, orb.zo);
		Vec3 current = trailPosition(orb, orb.getX(), orb.getY(), orb.getZ());
		TrackedTrail trackedTrail = TRAILS.get(orb.getId());
		if (trackedTrail == null) {
			trackedTrail = new TrackedTrail(SparkTrailHandler.create(current, TRAIL_RED, TRAIL_GREEN, TRAIL_BLUE));
			TRAILS.put(orb.getId(), trackedTrail);
		}
		trackedTrail.missingEntityChecks = 0;
		trackedTrail.trail.record(previous, current);
	}

	private static void ensureTrail(SoulOrbEntity orb) {
		if (TRAILS.containsKey(orb.getId())) {
			return;
		}
		Vec3 current = trailPosition(orb, orb.getX(), orb.getY(), orb.getZ());
		TRAILS.put(orb.getId(), new TrackedTrail(SparkTrailHandler.create(current, TRAIL_RED, TRAIL_GREEN, TRAIL_BLUE)));
	}

	private static Vec3 trailPosition(SoulOrbEntity orb, double x, double y, double z) {
		return new Vec3(x, y + orb.getBbHeight() * 0.5D, z);
	}

	private static void stopTrail(int entityId) {
		TrackedTrail trackedTrail = TRAILS.remove(entityId);
		if (trackedTrail != null) {
			trackedTrail.trail.stopRecording();
			FADING_TRAILS.put(trackedTrail.trail, SparkTrailHandler.MAX_POINT_AGE + 1);
		}
	}

	private static void tickFadingTrails() {
		Iterator<Map.Entry<SparkTrailHandler.SparkTrail, Integer>> iterator = FADING_TRAILS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<SparkTrailHandler.SparkTrail, Integer> entry = iterator.next();
			int remainingTicks = entry.getValue() - 1;
			if (remainingTicks <= 0) {
				iterator.remove();
			} else {
				entry.setValue(remainingTicks);
			}
		}
	}

	private static void discardTrails() {
		for (TrackedTrail trackedTrail : TRAILS.values()) {
			SparkTrailHandler.discard(trackedTrail.trail);
		}
		for (SparkTrailHandler.SparkTrail fadingTrail : FADING_TRAILS.keySet()) {
			SparkTrailHandler.discard(fadingTrail);
		}
		ACTIVE_TRAIL_ORB_IDS.clear();
		TRAILS.clear();
		FADING_TRAILS.clear();
	}

	private static final class TrackedTrail {
		private final SparkTrailHandler.SparkTrail trail;
		private int missingEntityChecks;

		private TrackedTrail(SparkTrailHandler.SparkTrail trail) {
			this.trail = trail;
		}
	}
}
