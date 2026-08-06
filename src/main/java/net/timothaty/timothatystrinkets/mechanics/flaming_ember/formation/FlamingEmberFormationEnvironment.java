package net.timothaty.timothatystrinkets.mechanics.flaming_ember.formation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public final class FlamingEmberFormationEnvironment {
	private FlamingEmberFormationEnvironment() {
	}

	public static boolean isPlayerEligible(ServerPlayer player) {
		return player != null
				&& player.isAlive()
				&& !player.isDeadOrDying()
				&& !player.isRemoved()
				&& !player.isSpectator()
				&& player.level().dimension().equals(Level.NETHER)
				&& !player.isInLava();
	}

	public static boolean hasSuitableLavaLake(ServerPlayer player) {
		if (!isPlayerEligible(player))
			return false;

		ServerLevel level = player.serverLevel();
		BlockPos nearestSource = findNearestLavaSource(level, player.position());
		return nearestSource != null && hasRequiredConnectedSources(level, nearestSource);
	}

	private static BlockPos findNearestLavaSource(ServerLevel level, Vec3 origin) {
		BlockPos center = BlockPos.containing(origin);
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		BlockPos nearest = null;
		double nearestDistanceSqr = FlamingEmberFormationData.LAVA_SEARCH_RADIUS
				* FlamingEmberFormationData.LAVA_SEARCH_RADIUS;

		for (int x = -FlamingEmberFormationData.LAVA_SEARCH_RADIUS; x <= FlamingEmberFormationData.LAVA_SEARCH_RADIUS; x++) {
			for (int y = -FlamingEmberFormationData.LAVA_SEARCH_RADIUS; y <= FlamingEmberFormationData.LAVA_SEARCH_RADIUS; y++) {
				for (int z = -FlamingEmberFormationData.LAVA_SEARCH_RADIUS; z <= FlamingEmberFormationData.LAVA_SEARCH_RADIUS; z++) {
					cursor.setWithOffset(center, x, y, z);
					if (!level.isLoaded(cursor) || !isLavaSource(level, cursor))
						continue;

					double distanceSqr = origin.distanceToSqr(Vec3.atCenterOf(cursor));
					if (distanceSqr <= nearestDistanceSqr) {
						nearestDistanceSqr = distanceSqr;
						nearest = cursor.immutable();
					}
				}
			}
		}
		return nearest;
	}

	private static boolean hasRequiredConnectedSources(ServerLevel level, BlockPos start) {
		Queue<BlockPos> pending = new ArrayDeque<>();
		Set<BlockPos> found = new HashSet<>();
		pending.add(start);
		found.add(start);

		while (!pending.isEmpty()) {
			BlockPos current = pending.remove();
			for (Direction direction : Direction.values()) {
				BlockPos neighbor = current.relative(direction);
				if (found.contains(neighbor) || !level.isLoaded(neighbor) || !isLavaSource(level, neighbor))
					continue;

				found.add(neighbor);
				if (found.size() >= FlamingEmberFormationData.REQUIRED_LAVA_SOURCES)
					return true;
				pending.add(neighbor);
			}
		}
		return false;
	}

	private static boolean isLavaSource(ServerLevel level, BlockPos pos) {
		FluidState fluidState = level.getFluidState(pos);
		return fluidState.is(FluidTags.LAVA) && fluidState.isSource();
	}
}
