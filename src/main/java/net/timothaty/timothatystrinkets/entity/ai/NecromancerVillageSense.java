package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.mechanics.necromancer.NecromancerConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public final class NecromancerVillageSense {
	private static final int VILLAGER_SCAN_APPROACH_MARGIN = 32;
	private static final int DEPLETED_VILLAGE_PROBE_DISTANCE_BLOCKS = 16 * 32;
	private static final int[][] DEPLETED_VILLAGE_PROBE_DIRECTIONS = {
		{1, 0},
		{-1, 0},
		{0, 1},
		{0, -1}
	};

	private NecromancerVillageSense() {
	}

	public static final class Cache {
		private ServerLevel cachedLevel;
		private BlockPos cachedVillageCenter;
		private long nextStructureSearchGameTime;
		private long nextPoiRefreshGameTime;
		private long nextVillagerScanGameTime;
		private int emptyVillageScans;
		private List<BlockPos> villagePois = List.of();
		private final Deque<BlockPos> depletedVillageCenters = new ArrayDeque<>();

		public boolean hasNearbyVillage(ServerLevel serverLevel, BlockPos origin, RandomSource random) {
			refreshIfNeeded(serverLevel, origin, random);
			return cachedVillageCenter != null;
		}

		public Optional<BlockPos> getCurrentVillageCenter(ServerLevel serverLevel, BlockPos origin, RandomSource random) {
			refreshIfNeeded(serverLevel, origin, random);
			return Optional.ofNullable(cachedVillageCenter);
		}

		public boolean isInsideCurrentVillage(BlockPos pos) {
			return cachedVillageCenter != null && NecromancerVillageVillagerScanner.isInsideVillageArea(
				pos,
				cachedVillageCenter,
				NecromancerConfig.VILLAGE_HORIZONTAL_RADIUS,
				NecromancerConfig.VILLAGE_VERTICAL_RADIUS
			);
		}

		public List<BlockPos> findVillagePatrolTargets(ServerLevel serverLevel, BlockPos origin, int limit, RandomSource random) {
			if (limit <= 0) {
				return List.of();
			}

			refreshIfNeeded(serverLevel, origin, random);
			if (cachedVillageCenter == null || villagePois.isEmpty()) {
				return List.of();
			}

			return villagePois.stream()
				.sorted(Comparator.comparingDouble(pos -> distanceSqr(pos, origin)))
				.limit(limit)
				.toList();
		}

		private void refreshIfNeeded(ServerLevel serverLevel, BlockPos origin, RandomSource random) {
			if (cachedLevel != serverLevel) {
				resetForLevel(serverLevel);
			}

			long gameTime = serverLevel.getGameTime();
			if (cachedVillageCenter != null && !isWithinApproachHeight(origin, cachedVillageCenter)) {
				cachedVillageCenter = null;
				villagePois = List.of();
				emptyVillageScans = 0;
				nextStructureSearchGameTime = 0L;
			}

			if (cachedVillageCenter != null) {
				refreshVillagePoisIfNeeded(serverLevel, gameTime, random);
				scanVillagersIfNeeded(serverLevel, origin, gameTime, random);
			}

			if (cachedVillageCenter == null && gameTime >= nextStructureSearchGameTime) {
				searchForVillage(serverLevel, origin, gameTime, random);
			}
		}

		private void resetForLevel(ServerLevel serverLevel) {
			cachedLevel = serverLevel;
			cachedVillageCenter = null;
			nextStructureSearchGameTime = 0L;
			nextPoiRefreshGameTime = 0L;
			nextVillagerScanGameTime = 0L;
			emptyVillageScans = 0;
			villagePois = List.of();
			depletedVillageCenters.clear();
		}

		private void searchForVillage(ServerLevel serverLevel, BlockPos origin, long gameTime, RandomSource random) {
			cachedVillageCenter = findNearestUndepletedVillage(serverLevel, origin, random).orElse(null);
			nextStructureSearchGameTime = gameTime
				+ NecromancerConfig.VILLAGE_STRUCTURE_SEARCH_INTERVAL_TICKS
				+ random.nextInt(NecromancerConfig.VILLAGE_STRUCTURE_SEARCH_RANDOM_EXTRA_TICKS + 1);

			if (cachedVillageCenter == null) {
				villagePois = List.of();
				return;
			}

			emptyVillageScans = 0;
			nextPoiRefreshGameTime = 0L;
			nextVillagerScanGameTime = gameTime
				+ NecromancerConfig.VILLAGE_VILLAGER_SCAN_INTERVAL_TICKS
				+ random.nextInt(NecromancerConfig.VILLAGE_VILLAGER_SCAN_RANDOM_EXTRA_TICKS + 1);
			refreshVillagePoisIfNeeded(serverLevel, gameTime, random);
		}

		private Optional<BlockPos> findNearestUndepletedVillage(ServerLevel serverLevel, BlockPos origin, RandomSource random) {
			BlockPos nearest = locateVillage(serverLevel, origin);
			if (nearest == null) {
				return Optional.empty();
			}
			if (!isRememberedAsDepleted(nearest)) {
				return Optional.of(nearest);
			}

			BlockPos best = null;
			double bestDistanceSqr = Double.MAX_VALUE;
			int firstDirection = random.nextInt(DEPLETED_VILLAGE_PROBE_DIRECTIONS.length);
			for (int index = 0; index < DEPLETED_VILLAGE_PROBE_DIRECTIONS.length; index++) {
				int[] direction = DEPLETED_VILLAGE_PROBE_DIRECTIONS[(firstDirection + index) % DEPLETED_VILLAGE_PROBE_DIRECTIONS.length];
				BlockPos probeOrigin = origin.offset(
					direction[0] * DEPLETED_VILLAGE_PROBE_DISTANCE_BLOCKS,
					0,
					direction[1] * DEPLETED_VILLAGE_PROBE_DISTANCE_BLOCKS
				);
				BlockPos candidate = locateVillage(serverLevel, probeOrigin);
				if (candidate == null || isRememberedAsDepleted(candidate) || !isWithinStructureSearchRadius(candidate, origin)) {
					continue;
				}

				double candidateDistanceSqr = horizontalDistanceSqr(candidate, origin);
				if (candidateDistanceSqr < bestDistanceSqr) {
					best = candidate;
					bestDistanceSqr = candidateDistanceSqr;
				}
			}

			return Optional.ofNullable(best);
		}

		private BlockPos locateVillage(ServerLevel serverLevel, BlockPos origin) {
			BlockPos located = serverLevel.findNearestMapStructure(
				StructureTags.VILLAGE,
				origin,
				NecromancerConfig.VILLAGE_STRUCTURE_SEARCH_RADIUS_CHUNKS,
				false
			);
			if (located == null) {
				return null;
			}

			int surfaceY = serverLevel.getHeight(Heightmap.Types.WORLD_SURFACE, located.getX(), located.getZ());
			BlockPos villageCenter = new BlockPos(located.getX(), surfaceY, located.getZ());
			return isWithinApproachHeight(origin, villageCenter) ? villageCenter : null;
		}

		private void refreshVillagePoisIfNeeded(ServerLevel serverLevel, long gameTime, RandomSource random) {
			if (cachedVillageCenter == null || gameTime < nextPoiRefreshGameTime) {
				return;
			}

			BlockPos villageCenter = cachedVillageCenter;
			villagePois = serverLevel.getPoiManager().findAll(
					NecromancerVillageSense::isVillagePoi,
					pos -> NecromancerVillageVillagerScanner.isInsideVillageArea(
						pos,
						villageCenter,
						NecromancerConfig.VILLAGE_HORIZONTAL_RADIUS,
						NecromancerConfig.VILLAGE_VERTICAL_RADIUS
					),
					villageCenter,
					NecromancerConfig.VILLAGE_HORIZONTAL_RADIUS,
					PoiManager.Occupancy.ANY
				)
				.toList();

			nextPoiRefreshGameTime = gameTime
				+ NecromancerConfig.VILLAGE_POI_REFRESH_INTERVAL_TICKS
				+ random.nextInt(NecromancerConfig.VILLAGE_POI_REFRESH_RANDOM_EXTRA_TICKS + 1);
		}

		private void scanVillagersIfNeeded(ServerLevel serverLevel, BlockPos origin, long gameTime, RandomSource random) {
			if (cachedVillageCenter == null || gameTime < nextVillagerScanGameTime) {
				return;
			}

			if (!isCloseEnoughToScan(origin, cachedVillageCenter) || !serverLevel.isPositionEntityTicking(cachedVillageCenter)) {
				nextVillagerScanGameTime = gameTime + 20;
				return;
			}

			boolean hasUncorruptedVillagers = NecromancerVillageVillagerScanner.hasUncorruptedVillagers(
				serverLevel,
				cachedVillageCenter,
				NecromancerConfig.VILLAGE_HORIZONTAL_RADIUS,
				NecromancerConfig.VILLAGE_VERTICAL_RADIUS
			);
			if (hasUncorruptedVillagers) {
				emptyVillageScans = 0;
			} else {
				emptyVillageScans++;
				if (emptyVillageScans >= NecromancerConfig.VILLAGE_DEPLETED_CONFIRMATION_SCANS) {
					rememberCurrentVillageAsDepleted();
				}
			}

			nextVillagerScanGameTime = gameTime
				+ NecromancerConfig.VILLAGE_VILLAGER_SCAN_INTERVAL_TICKS
				+ random.nextInt(NecromancerConfig.VILLAGE_VILLAGER_SCAN_RANDOM_EXTRA_TICKS + 1);
		}

		private void rememberCurrentVillageAsDepleted() {
			if (cachedVillageCenter == null) {
				return;
			}

			depletedVillageCenters.addLast(cachedVillageCenter.immutable());
			while (depletedVillageCenters.size() > NecromancerConfig.MAX_DEPLETED_VILLAGES_REMEMBERED) {
				depletedVillageCenters.removeFirst();
			}

			cachedVillageCenter = null;
			villagePois = List.of();
			emptyVillageScans = 0;
			nextStructureSearchGameTime = 0L;
		}

		private boolean isRememberedAsDepleted(BlockPos center) {
			return depletedVillageCenters.stream().anyMatch(
				depletedCenter -> horizontalDistanceSqr(depletedCenter, center) <= NecromancerConfig.SAME_VILLAGE_CENTER_DISTANCE_SQR
			);
		}
	}

	private static boolean isVillagePoi(Holder<PoiType> poiType) {
		return poiType.is(PoiTypeTags.VILLAGE);
	}

	private static boolean isCloseEnoughToScan(BlockPos origin, BlockPos center) {
		double scanDistance = NecromancerConfig.VILLAGE_HORIZONTAL_RADIUS + VILLAGER_SCAN_APPROACH_MARGIN;
		return horizontalDistanceSqr(origin, center) <= scanDistance * scanDistance;
	}

	private static double distanceSqr(BlockPos first, BlockPos second) {
		double dx = first.getX() - second.getX();
		double dy = first.getY() - second.getY();
		double dz = first.getZ() - second.getZ();
		return dx * dx + dy * dy + dz * dz;
	}

	private static double horizontalDistanceSqr(BlockPos first, BlockPos second) {
		double dx = first.getX() - second.getX();
		double dz = first.getZ() - second.getZ();
		return dx * dx + dz * dz;
	}

	private static boolean isWithinApproachHeight(BlockPos origin, BlockPos villageCenter) {
		return Math.abs(villageCenter.getY() - origin.getY()) <= NecromancerConfig.VILLAGE_MAX_APPROACH_VERTICAL_DIFFERENCE;
	}

	private static boolean isWithinStructureSearchRadius(BlockPos pos, BlockPos origin) {
		double radiusBlocks = NecromancerConfig.VILLAGE_STRUCTURE_SEARCH_RADIUS_CHUNKS * 16.0D;
		return horizontalDistanceSqr(pos, origin) <= radiusBlocks * radiusBlocks;
	}
}
