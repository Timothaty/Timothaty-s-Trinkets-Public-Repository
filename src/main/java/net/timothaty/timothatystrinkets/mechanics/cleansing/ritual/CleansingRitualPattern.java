package net.timothaty.timothatystrinkets.mechanics.cleansing.ritual;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CleansingRitualPattern {
	public static final int INCENSE_COUNT = 16;
	public static final List<BlockPos> CLOCKWISE_ROUTE = List.of(
			new BlockPos(-1, 0, -2), new BlockPos(0, 0, -2), new BlockPos(1, 0, -2),
			new BlockPos(1, 0, -1), new BlockPos(2, 0, -1), new BlockPos(2, 0, 0),
			new BlockPos(2, 0, 1), new BlockPos(1, 0, 1), new BlockPos(1, 0, 2),
			new BlockPos(0, 0, 2), new BlockPos(-1, 0, 2), new BlockPos(-1, 0, 1),
			new BlockPos(-2, 0, 1), new BlockPos(-2, 0, 0), new BlockPos(-2, 0, -1),
			new BlockPos(-1, 0, -1)
	);
	public static final List<BlockPos> CANDLE_OFFSETS = List.of(
			new BlockPos(0, 0, -1), new BlockPos(-1, 0, 0),
			new BlockPos(1, 0, 0), new BlockPos(0, 0, 1)
	);
	public static final Set<BlockPos> INCENSE_OFFSETS = Set.copyOf(CLOCKWISE_ROUTE);
	public static final Map<BlockPos, Integer> ROUTE_INDEX = createRouteIndex();
	public static final Map<BlockPos, Set<Direction>> EXPECTED_CONNECTIONS = createExpectedConnections();
	public static final List<BlockPos> REQUIRED_OFFSETS = createRequiredOffsets();

	private CleansingRitualPattern() {
	}

	public static int wrappedRouteIndex(int startIndex, int completedSteps) {
		return Math.floorMod(startIndex + completedSteps, INCENSE_COUNT);
	}

	private static Map<BlockPos, Integer> createRouteIndex() {
		Map<BlockPos, Integer> indices = new HashMap<>();
		for (int i = 0; i < CLOCKWISE_ROUTE.size(); i++) indices.put(CLOCKWISE_ROUTE.get(i), i);
		return Map.copyOf(indices);
	}

	private static Map<BlockPos, Set<Direction>> createExpectedConnections() {
		Map<BlockPos, Set<Direction>> connections = new HashMap<>();
		for (int i = 0; i < CLOCKWISE_ROUTE.size(); i++) {
			BlockPos current = CLOCKWISE_ROUTE.get(i);
			BlockPos previous = CLOCKWISE_ROUTE.get(Math.floorMod(i - 1, INCENSE_COUNT));
			BlockPos next = CLOCKWISE_ROUTE.get((i + 1) % INCENSE_COUNT);
			connections.put(current, Set.of(directionBetween(current, previous), directionBetween(current, next)));
		}
		return Map.copyOf(connections);
	}

	private static List<BlockPos> createRequiredOffsets() {
		List<BlockPos> offsets = new ArrayList<>(INCENSE_COUNT + CANDLE_OFFSETS.size() + 1);
		offsets.add(BlockPos.ZERO);
		offsets.addAll(CLOCKWISE_ROUTE);
		offsets.addAll(CANDLE_OFFSETS);
		return List.copyOf(offsets);
	}

	private static Direction directionBetween(BlockPos from, BlockPos to) {
		int dx = to.getX() - from.getX();
		int dz = to.getZ() - from.getZ();
		if (dx == 1 && dz == 0) return Direction.EAST;
		if (dx == -1 && dz == 0) return Direction.WEST;
		if (dx == 0 && dz == 1) return Direction.SOUTH;
		if (dx == 0 && dz == -1) return Direction.NORTH;
		throw new IllegalArgumentException("Cleansing route contains a non-cardinal step");
	}
}
