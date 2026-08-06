package net.timothaty.timothatystrinkets.mechanics.damnation_altar;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.entity.ai.NecromancerSummonTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class DamnationAltarSummonTelegraphManager {
	private static final int SUMMON_RADIUS = 7;
	private static final int SEARCH_ATTEMPTS = 48;
	private static final int MIN_DELAY_TICKS = 60;
	private static final int MAX_DELAY_TICKS = 100;
	private static final int PARTICLE_INTERVAL_TICKS = 5;
	private static final int RELOCATION_RADIUS = 2;
	private static final int MAX_RELOCATION_CANDIDATES = 32;
	private static final DustParticleOptions TELEGRAPH_DUST = new DustParticleOptions(
			new Vector3f(0x68 / 255.0F, 0xE6 / 255.0F, 0x23 / 255.0F),
			0.85F
	);
	private static final Map<ResourceKey<Level>, List<PendingSummon>> PENDING = new HashMap<>();

	private DamnationAltarSummonTelegraphManager() {
	}

	public static int schedule(ServerLevel level, BlockPos altarPos, int count) {
		if (count <= 0)
			return 0;

		RandomSource random = level.getRandom();
		Set<BlockPos> ritualPositions = new HashSet<>();
		List<PendingSummon> dimensionPending = PENDING.computeIfAbsent(level.dimension(), ignored -> new ArrayList<>());
		int scheduled = 0;
		for (int index = 0; index < count; index++) {
			EntityType<?> entityType = NecromancerSummonTypes.pickSummonType(level, random);
			if (entityType == null)
				continue;

			BlockPos groundPos = findInitialGround(level, altarPos, entityType, random, ritualPositions);
			if (groundPos == null)
				continue;

			ritualPositions.add(groundPos);
			long spawnTime = level.getGameTime() + Mth.nextInt(random, MIN_DELAY_TICKS, MAX_DELAY_TICKS);
			dimensionPending.add(new PendingSummon(level.dimension(), entityType, groundPos, altarPos.immutable(), spawnTime));
			scheduled++;
		}

		if (dimensionPending.isEmpty())
			PENDING.remove(level.dimension());
		return scheduled;
	}

	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent.Post event) {
		if (!(event.getLevel() instanceof ServerLevel level))
			return;

		List<PendingSummon> pendingSummons = PENDING.get(level.dimension());
		if (pendingSummons == null || pendingSummons.isEmpty())
			return;

		long gameTime = level.getGameTime();
		Iterator<PendingSummon> iterator = pendingSummons.iterator();
		while (iterator.hasNext()) {
			PendingSummon pending = iterator.next();
			if (level.hasChunkAt(pending.altarPos())
					&& level.getBlockState(pending.altarPos()).getBlock() != TimothatysTrinketsModBlocks.DAMNATION_ALTAR.get()) {
				iterator.remove();
				continue;
			}
			if (gameTime >= pending.spawnTime()) {
				iterator.remove();
				finishSummon(level, pending);
			} else if (gameTime % PARTICLE_INTERVAL_TICKS == 0L
					&& level.hasChunkAt(pending.altarPos()) && level.hasChunkAt(pending.groundPos())) {
				spawnTelegraph(level, pending.groundPos());
			}
		}

		if (pendingSummons.isEmpty())
			PENDING.remove(level.dimension());
	}

	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		if (event.getLevel() instanceof ServerLevel level)
			PENDING.remove(level.dimension());
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		PENDING.clear();
	}

	private static void finishSummon(ServerLevel level, PendingSummon pending) {
		if (!pending.dimension().equals(level.dimension()) || !level.hasChunkAt(pending.altarPos()))
			return;
		if (level.getBlockState(pending.altarPos()).getBlock() != TimothatysTrinketsModBlocks.DAMNATION_ALTAR.get())
			return;

		SpawnPoint spawnPoint = resolveSpawnPoint(level, pending.entityType(), pending.groundPos());
		if (spawnPoint == null) {
			spawnPoint = findNearbyReplacement(level, pending.entityType(), pending.groundPos());
			if (spawnPoint == null)
				return;
		}

		Entity created = pending.entityType().create(level);
		if (!(created instanceof Mob mob)) {
			if (created != null)
				created.discard();
			return;
		}

		RandomSource random = level.getRandom();
		float yaw = random.nextFloat() * 360.0F;
		mob.moveTo(spawnPoint.position().x, spawnPoint.position().y, spawnPoint.position().z, yaw, 0.0F);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPoint.groundPos().above()), MobSpawnType.MOB_SUMMONED, (SpawnGroupData) null);
		if (!level.addFreshEntity(mob))
			return;

		level.playSound(
				null,
				spawnPoint.position().x,
				spawnPoint.position().y,
				spawnPoint.position().z,
				SoundEvents.EVOKER_CAST_SPELL,
				SoundSource.HOSTILE,
				1.0F,
				0.75F + random.nextFloat() * 0.15F
		);
	}

	private static BlockPos findInitialGround(ServerLevel level, BlockPos altarPos, EntityType<?> entityType,
			RandomSource random, Set<BlockPos> ritualPositions) {
		int radiusSqr = SUMMON_RADIUS * SUMMON_RADIUS;
		for (int attempt = 0; attempt < SEARCH_ATTEMPTS; attempt++) {
			int dx = Mth.nextInt(random, -SUMMON_RADIUS, SUMMON_RADIUS);
			int dz = Mth.nextInt(random, -SUMMON_RADIUS, SUMMON_RADIUS);
			if (dx * dx + dz * dz > radiusSqr)
				continue;

			for (int dy = 3; dy >= -4; dy--) {
				BlockPos groundPos = altarPos.offset(dx, dy, dz);
				if (ritualPositions.contains(groundPos))
					continue;
				if (resolveSpawnPoint(level, entityType, groundPos) != null)
					return groundPos.immutable();
			}
		}
		return null;
	}

	private static SpawnPoint findNearbyReplacement(ServerLevel level, EntityType<?> entityType, BlockPos originalGround) {
		List<BlockPos> candidates = new ArrayList<>();
		for (int dx = -RELOCATION_RADIUS; dx <= RELOCATION_RADIUS; dx++) {
			for (int dz = -RELOCATION_RADIUS; dz <= RELOCATION_RADIUS; dz++) {
				for (int dy = -1; dy <= 1; dy++) {
					if (dx == 0 && dy == 0 && dz == 0)
						continue;
					candidates.add(originalGround.offset(dx, dy, dz));
				}
			}
		}
		candidates.sort(Comparator.comparingDouble(candidate -> candidate.distSqr(originalGround)));

		int checked = 0;
		for (BlockPos candidate : candidates) {
			if (checked++ >= MAX_RELOCATION_CANDIDATES)
				break;
			SpawnPoint spawnPoint = resolveSpawnPoint(level, entityType, candidate);
			if (spawnPoint != null)
				return spawnPoint;
		}
		return null;
	}

	private static SpawnPoint resolveSpawnPoint(ServerLevel level, EntityType<?> entityType, BlockPos groundPos) {
		if (!level.hasChunkAt(groundPos) || !level.hasChunkAt(groundPos.above()))
			return null;

		BlockState groundState = level.getBlockState(groundPos);
		if (!groundState.isFaceSturdy(level, groundPos, Direction.UP))
			return null;
		VoxelShape supportShape = groundState.getCollisionShape(level, groundPos);
		if (supportShape.isEmpty())
			return null;

		double spawnY = groundPos.getY() + supportShape.max(Direction.Axis.Y);
		Vec3 position = new Vec3(groundPos.getX() + 0.5D, spawnY, groundPos.getZ() + 0.5D);
		EntityDimensions dimensions = entityType.getDimensions();
		AABB bounds = dimensions.makeBoundingBox(position);
		AABB validationBounds = new AABB(
				bounds.minX,
				bounds.minY,
				bounds.minZ,
				bounds.maxX,
				Math.max(bounds.maxY, spawnY + 2.0D),
				bounds.maxZ
		);
		if (!areBoundingChunksLoaded(level, validationBounds))
			return null;
		AABB collisionBounds = validationBounds.deflate(1.0E-7D);
		if (!level.noCollision(collisionBounds) || level.containsAnyLiquid(collisionBounds))
			return null;
		if (!level.getEntities((Entity) null, collisionBounds, entity -> !entity.isRemoved()).isEmpty())
			return null;
		return new SpawnPoint(groundPos.immutable(), position);
	}

	private static boolean areBoundingChunksLoaded(ServerLevel level, AABB bounds) {
		int minX = Mth.floor(bounds.minX);
		int maxX = Mth.floor(bounds.maxX - 1.0E-7D);
		int minZ = Mth.floor(bounds.minZ);
		int maxZ = Mth.floor(bounds.maxZ - 1.0E-7D);
		int y = Mth.floor(bounds.minY);
		return level.hasChunkAt(new BlockPos(minX, y, minZ))
				&& level.hasChunkAt(new BlockPos(minX, y, maxZ))
				&& level.hasChunkAt(new BlockPos(maxX, y, minZ))
				&& level.hasChunkAt(new BlockPos(maxX, y, maxZ));
	}

	private static void spawnTelegraph(ServerLevel level, BlockPos groundPos) {
		BlockState groundState = level.getBlockState(groundPos);
		VoxelShape supportShape = groundState.getCollisionShape(level, groundPos);
		if (supportShape.isEmpty())
			return;
		double floorY = groundPos.getY() + supportShape.max(Direction.Axis.Y);
		RandomSource random = level.getRandom();
		for (int layer = 0; layer < 4; layer++) {
			double y = floorY + 0.1D + layer * 0.55D;
			for (int particle = 0; particle < 2; particle++) {
				double angle = random.nextDouble() * Mth.TWO_PI;
				double radius = 0.18D + random.nextDouble() * 0.18D;
				level.sendParticles(
						TELEGRAPH_DUST,
						groundPos.getX() + 0.5D + Math.cos(angle) * radius,
						y,
						groundPos.getZ() + 0.5D + Math.sin(angle) * radius,
						1,
						0.015D, 0.025D, 0.015D,
						0.0D
				);
			}
		}
	}

	private record PendingSummon(ResourceKey<Level> dimension, EntityType<?> entityType, BlockPos groundPos,
			BlockPos altarPos, long spawnTime) {
	}

	private record SpawnPoint(BlockPos groundPos, Vec3 position) {
	}
}
