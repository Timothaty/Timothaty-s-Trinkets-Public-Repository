package net.timothaty.timothatystrinkets.mechanics.blight;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

public final class BlightZoneHelper {
	private BlightZoneHelper() {
	}

	public static boolean isStandingOnBlight(LivingEntity living) {
		Level level = living.level();
		BlockPos groundPos = BlockPos.containing(living.getX(), living.getY() - 0.001D, living.getZ());
		return level.hasChunkAt(groundPos) && isBlight(level.getBlockState(groundPos));
	}

	public static boolean isInsideBlightAura(LivingEntity living) {
		Level level = living.level();
		AABB box = living.getBoundingBox();
		int minX = Mth.floor(box.minX + BlightConfig.AURA_FOOTPRINT_INSET);
		int maxX = Mth.floor(box.maxX - BlightConfig.AURA_FOOTPRINT_INSET);
		int minZ = Mth.floor(box.minZ + BlightConfig.AURA_FOOTPRINT_INSET);
		int maxZ = Mth.floor(box.maxZ - BlightConfig.AURA_FOOTPRINT_INSET);
		int feetY = Mth.floor(box.minY + BlightConfig.AURA_FOOTPRINT_INSET);
		BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				for (int baseY = feetY - 1; baseY >= feetY - BlightConfig.AURA_HEIGHT; baseY--) {
					checkPos.set(x, baseY, z);
					if (level.hasChunkAt(checkPos)
							&& isBlight(level.getBlockState(checkPos))
							&& hasClearColumnTo(level, checkPos, feetY)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public static boolean isBlightShelterAt(Level level, BlockPos standPos) {
		for (int baseY = standPos.getY() - 1; baseY >= standPos.getY() - BlightConfig.AURA_HEIGHT; baseY--) {
			BlockPos blightPos = new BlockPos(standPos.getX(), baseY, standPos.getZ());
			if (level.hasChunkAt(blightPos)
					&& isBlight(level.getBlockState(blightPos))
					&& hasClearColumnTo(level, blightPos, standPos.getY())) {
				return true;
			}
		}

		return false;
	}

	public static BlockPos findNearestBlightStandPos(ServerLevel level, LivingEntity living, int horizontalRange, int verticalRange) {
		BlockPos origin = living.blockPosition();
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;

		for (int dx = -horizontalRange; dx <= horizontalRange; dx++) {
			for (int dz = -horizontalRange; dz <= horizontalRange; dz++) {
				if ((dx * dx) + (dz * dz) > horizontalRange * horizontalRange) {
					continue;
				}
				for (int dy = -verticalRange; dy <= verticalRange; dy++) {
					BlockPos blightPos = origin.offset(dx, dy, dz);
					if (!level.hasChunkAt(blightPos) || !isBlight(level.getBlockState(blightPos))) {
						continue;
					}

					BlockPos standPos = blightPos.above();
					if (!hasClearColumnTo(level, blightPos, standPos.getY())) {
						continue;
					}

					int sx = standPos.getX() - origin.getX();
					int sy = standPos.getY() - origin.getY();
					int sz = standPos.getZ() - origin.getZ();
					double distance = (sx * sx) + (sy * sy) + (sz * sz);
					if (distance < bestDistance) {
						bestDistance = distance;
						best = standPos.immutable();
					}
				}
			}
		}

		return best;
	}

	public static BlockPos findNearestReachableBlightStandPos(ServerLevel level, PathfinderMob mob, int horizontalRange, int verticalRange) {
		return findNearestReachableBlightStandPos(level, mob, horizontalRange, verticalRange, 8);
	}

	public static BlockPos findNearestReachableBlightStandPos(ServerLevel level, PathfinderMob mob, int horizontalRange, int verticalRange, int maxPathChecks) {
		ReachableBlightSearch padded = findNearestReachableBlightStandPos(level, mob, horizontalRange, verticalRange, maxPathChecks, true);
		if (padded.standPos != null || !padded.sawValidStandPos) {
			return padded.standPos;
		}
		return findNearestReachableBlightStandPos(level, mob, horizontalRange, verticalRange, maxPathChecks, false).standPos;
	}

	private static ReachableBlightSearch findNearestReachableBlightStandPos(ServerLevel level, PathfinderMob mob, int horizontalRange, int verticalRange, int maxPathChecks, boolean requirePadding) {
		if (maxPathChecks <= 0) {
			return ReachableBlightSearch.NONE;
		}

		BlockPos origin = mob.blockPosition();
		int pathChecks = 0;
		boolean sawValidStandPos = false;

		for (int radius = 0; radius <= horizontalRange; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != radius || (dx * dx) + (dz * dz) > horizontalRange * horizontalRange) {
						continue;
					}
					for (int yStep = 0; yStep <= verticalRange * 2; yStep++) {
						int dy = yStep == 0 ? 0 : ((yStep + 1) / 2) * (yStep % 2 == 1 ? 1 : -1);
						BlockPos blightPos = origin.offset(dx, dy, dz);
						BlockPos standPos = getValidBlightStandPos(level, blightPos);
						if (standPos == null) {
							continue;
						}
						sawValidStandPos = true;
						if (requirePadding && !hasHorizontalBlightPadding(level, blightPos)) {
							continue;
						}

						Path path = mob.getNavigation().createPath(standPos, 0);
						pathChecks++;
						if (path != null && path.canReach()) {
							return new ReachableBlightSearch(standPos.immutable(), true);
						}
						if (pathChecks >= maxPathChecks) {
							return new ReachableBlightSearch(null, true);
						}
					}
				}
			}
		}

		return sawValidStandPos ? ReachableBlightSearch.FOUND_NO_REACHABLE_POS : ReachableBlightSearch.NONE;
	}

	private static final class ReachableBlightSearch {
		private static final ReachableBlightSearch NONE = new ReachableBlightSearch(null, false);
		private static final ReachableBlightSearch FOUND_NO_REACHABLE_POS = new ReachableBlightSearch(null, true);

		private final BlockPos standPos;
		private final boolean sawValidStandPos;

		private ReachableBlightSearch(BlockPos standPos, boolean sawValidStandPos) {
			this.standPos = standPos;
			this.sawValidStandPos = sawValidStandPos;
		}
	}

	private static BlockPos getValidBlightStandPos(Level level, BlockPos blightPos) {
		if (!level.hasChunkAt(blightPos) || !isBlight(level.getBlockState(blightPos))) {
			return null;
		}

		BlockPos standPos = blightPos.above();
		return hasClearColumnTo(level, blightPos, standPos.getY()) ? standPos : null;
	}

	private static boolean hasHorizontalBlightPadding(Level level, BlockPos blightPos) {
		int paddedSides = 0;
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos neighbor = blightPos.relative(direction);
			if (getValidBlightStandPos(level, neighbor) != null) {
				paddedSides++;
			}
		}
		return paddedSides == 4;
	}

	private static boolean hasClearColumnTo(Level level, BlockPos blightPos, int feetY) {
		BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
		for (int y = blightPos.getY() + 1; y <= feetY; y++) {
			checkPos.set(blightPos.getX(), y, blightPos.getZ());
			if (!level.hasChunkAt(checkPos)) {
				return false;
			}

			BlockState state = level.getBlockState(checkPos);
			if (!state.getCollisionShape(level, checkPos).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private static boolean isBlight(BlockState state) {
		return state.is(TimothatysTrinketsModBlocks.BLOCK_OF_BLIGHT.get());
	}
}
