package net.timothaty.timothatystrinkets.mechanics.cleric_quests.sacrament;

import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestProgress;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestRuntimeManager;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestSavedData;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SacramentTargetMarkerManager {
	private static final int SEARCH_INTERVAL_TICKS = 200;
	private static final int ANIMATION_DURATION_TICKS = 26;
	private static final double SEARCH_RADIUS = 64.0D;
	private static final int MAX_PER_TARGET_TYPE = 4;
	private static final int MAX_TOTAL_TARGETS = 12;
	private static final float ANGULAR_SPEED = 0.58F;
	private static final DustParticleOptions PALE_DUST = new DustParticleOptions(new Vector3f(253.0F / 255.0F, 1.0F, 202.0F / 255.0F), 0.85F);
	private static final DustParticleOptions BRIGHT_DUST = new DustParticleOptions(new Vector3f(250.0F / 255.0F, 1.0F, 145.0F / 255.0F), 0.85F);
	private static final Map<UUID, List<Marker>> MARKERS_BY_PLAYER = new HashMap<>();

	private SacramentTargetMarkerManager() {
	}

	public static void tick(MinecraftServer server, long now) {
		tickAnimations(server, now);
		if (Math.floorMod(now, SEARCH_INTERVAL_TICKS) == 0L && ClericQuestRuntimeManager.hasActiveHuntingPlayers())
			startPulse(server, now);
	}

	private static void startPulse(MinecraftServer server, long now) {
		ClericQuestSavedData data = ClericQuestSavedData.get(server.overworld());
		for (UUID playerId : ClericQuestRuntimeManager.activeHuntingPlayers()) {
			ServerPlayer player = server.getPlayerList().getPlayer(playerId);
			ClericQuestProgress progress = data.get(playerId);
			if (player == null || progress == null || progress.sacramentStage() != SacramentStage.HUNT_ACTIVE)
				continue;

			List<ResourceLocation> targets = progress.sacramentTargets();
			Map<ResourceLocation, Integer> pendingTargetIndices = new HashMap<>();
			for (int index = 0; index < targets.size() && index < 3; index++) {
				if ((progress.sacramentKilledMask() & (1 << index)) == 0)
					pendingTargetIndices.put(targets.get(index), index);
			}
			if (pendingTargetIndices.isEmpty()) {
				MARKERS_BY_PLAYER.remove(playerId);
				continue;
			}

			ServerLevel level = player.serverLevel();
			AABB searchBox = player.getBoundingBox().inflate(SEARCH_RADIUS);
			List<LivingEntity> nearbyTargets = level.getEntitiesOfClass(LivingEntity.class, searchBox, entity -> {
				if (!entity.isAlive() || entity == player || player.distanceToSqr(entity) > SEARCH_RADIUS * SEARCH_RADIUS)
					return false;
				ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
				return pendingTargetIndices.containsKey(typeId);
			});

			List<Marker> pulseMarkers = new ArrayList<>();
			for (Map.Entry<ResourceLocation, Integer> target : pendingTargetIndices.entrySet()) {
				nearbyTargets.stream()
					.filter(entity -> BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(target.getKey()))
					.sorted(Comparator.comparingDouble(player::distanceToSqr))
					.limit(MAX_PER_TARGET_TYPE)
					.forEach(entity -> pulseMarkers.add(new Marker(level.dimension(), entity.getUUID(), target.getValue(), now)));
				if (pulseMarkers.size() >= MAX_TOTAL_TARGETS)
					break;
			}
			if (pulseMarkers.isEmpty())
				MARKERS_BY_PLAYER.remove(playerId);
			else
				MARKERS_BY_PLAYER.put(playerId, new ArrayList<>(pulseMarkers.subList(0, Math.min(MAX_TOTAL_TARGETS, pulseMarkers.size()))));
		}
	}

	private static void tickAnimations(MinecraftServer server, long now) {
		if (MARKERS_BY_PLAYER.isEmpty())
			return;
		ClericQuestSavedData data = ClericQuestSavedData.get(server.overworld());
		Iterator<Map.Entry<UUID, List<Marker>>> players = MARKERS_BY_PLAYER.entrySet().iterator();
		while (players.hasNext()) {
			Map.Entry<UUID, List<Marker>> entry = players.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			ClericQuestProgress progress = data.get(entry.getKey());
			if (player == null || progress == null || progress.sacramentStage() != SacramentStage.HUNT_ACTIVE) {
				players.remove();
				continue;
			}

			Iterator<Marker> markers = entry.getValue().iterator();
			while (markers.hasNext()) {
				Marker marker = markers.next();
				long animationTick = now - marker.startedAt;
				if (animationTick < 0L || animationTick >= ANIMATION_DURATION_TICKS
						|| marker.targetIndex >= progress.sacramentTargets().size()
						|| (progress.sacramentKilledMask() & (1 << marker.targetIndex)) != 0) {
					markers.remove();
					continue;
				}
				ServerLevel level = server.getLevel(marker.dimension);
				Entity entity = level == null ? null : level.getEntity(marker.entityId);
				if (!(entity instanceof LivingEntity living) || !living.isAlive() || player.serverLevel() != level) {
					markers.remove();
					continue;
				}
				drawSpiralTick(player, living, (int) animationTick);
			}
			if (entry.getValue().isEmpty())
				players.remove();
		}
	}

	private static void drawSpiralTick(ServerPlayer player, LivingEntity entity, int animationTick) {
		float heightProgress = animationTick / (float) (ANIMATION_DURATION_TICKS - 1);
		double radius = Mth.clamp(entity.getBbWidth() * 0.65F, 0.28F, 0.85F);
		double height = Mth.clamp(entity.getBbHeight(), 0.8F, 3.0F);
		double angle = animationTick * ANGULAR_SPEED;
		sendDust(player, PALE_DUST, entity, radius, height, heightProgress, angle);
		sendDust(player, BRIGHT_DUST, entity, radius, height, heightProgress, angle + Math.PI);
	}

	private static void sendDust(ServerPlayer player, DustParticleOptions dust, LivingEntity entity, double radius, double height, float heightProgress, double angle) {
		double x = entity.getX() + Math.cos(angle) * radius;
		double y = entity.getY() + 0.1D + height * heightProgress;
		double z = entity.getZ() + Math.sin(angle) * radius;
		player.serverLevel().sendParticles(player, dust, true, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
	}

	public static void clear() {
		MARKERS_BY_PLAYER.clear();
	}

	private record Marker(ResourceKey<Level> dimension, UUID entityId, int targetIndex, long startedAt) {
	}
}
