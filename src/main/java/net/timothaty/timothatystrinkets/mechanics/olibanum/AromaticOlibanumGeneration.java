package net.timothaty.timothatystrinkets.mechanics.olibanum;

import net.timothaty.timothatystrinkets.block.AromaticOlibanumBlock;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Adds at most one resin source to each successfully generated vanilla oak or birch tree. */
public final class AromaticOlibanumGeneration {
	private static final int CHANCE_NUMERATOR = 777;
	private static final int CHANCE_DENOMINATOR = 10_000;
	private static final int MAX_HORIZONTAL_DISTANCE = 6;
	private static final int MIN_VERTICAL_OFFSET = -1;
	private static final int MAX_VERTICAL_OFFSET = 32;
	private static final int MAX_CONNECTED_LOGS = 256;

	private static final List<ResourceKey<ConfiguredFeature<?, ?>>> VANILLA_TREES = List.of(
			TreeFeatures.OAK,
			TreeFeatures.OAK_BEES_0002,
			TreeFeatures.OAK_BEES_002,
			TreeFeatures.OAK_BEES_005,
			TreeFeatures.FANCY_OAK,
			TreeFeatures.FANCY_OAK_BEES_0002,
			TreeFeatures.FANCY_OAK_BEES_002,
			TreeFeatures.FANCY_OAK_BEES_005,
			TreeFeatures.FANCY_OAK_BEES,
			TreeFeatures.SWAMP_OAK,
			TreeFeatures.BIRCH,
			TreeFeatures.BIRCH_BEES_0002,
			TreeFeatures.BIRCH_BEES_002,
			TreeFeatures.BIRCH_BEES_005,
			TreeFeatures.SUPER_BIRCH_BEES_0002,
			TreeFeatures.SUPER_BIRCH_BEES
	);

	private AromaticOlibanumGeneration() {
	}

	public static void afterTreePlaced(FeaturePlaceContext<TreeConfiguration> context, Set<BlockPos> generatedLogs) {
		if (!(context.level() instanceof WorldGenRegion) || !isSupportedVanillaTree(context)) {
			return;
		}

		List<LogCandidate> candidates = collectFreeAttachments(context, generatedLogs);
		if (candidates.isEmpty()) {
			return;
		}

		RandomSource random = context.random();
		if (random.nextInt(CHANCE_DENOMINATOR) >= CHANCE_NUMERATOR) {
			return;
		}

		LogCandidate selectedLog = candidates.get(random.nextInt(candidates.size()));
		Direction facing = selectedLog.freeSides().get(random.nextInt(selectedLog.freeSides().size()));
		BlockPos sourcePos = selectedLog.logPos().relative(facing);
		BlockState resin = TimothatysTrinketsModBlocks.AROMATIC_OLIBANUM.get().defaultBlockState()
				.setValue(AromaticOlibanumBlock.FACING, facing)
				.setValue(AromaticOlibanumBlock.AGE, AromaticOlibanumBlock.MAX_AGE);
		context.level().setBlock(sourcePos, resin, Block.UPDATE_CLIENTS);
	}

	private static boolean isSupportedVanillaTree(FeaturePlaceContext<TreeConfiguration> context) {
		Registry<ConfiguredFeature<?, ?>> registry = context.level().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
		for (ResourceKey<ConfiguredFeature<?, ?>> key : VANILLA_TREES) {
			ConfiguredFeature<?, ?> configured = registry.get(key);
			if (configured != null && configured.config() == context.config()) {
				return true;
			}
		}
		return false;
	}

	private static List<LogCandidate> collectFreeAttachments(FeaturePlaceContext<TreeConfiguration> context,
			Set<BlockPos> generatedLogs) {
		BlockPos origin = context.origin();
		Set<BlockPos> treeLogs = new HashSet<>();
		for (BlockPos generatedLog : generatedLogs) {
			if (!isWithinTreeBounds(origin, generatedLog)) {
				continue;
			}
			BlockState state = context.level().getBlockState(generatedLog);
			if (state.is(Blocks.OAK_LOG) || state.is(Blocks.BIRCH_LOG)) {
				treeLogs.add(generatedLog.immutable());
			}
		}
		if (treeLogs.isEmpty()) {
			return List.of();
		}

		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> visited = new HashSet<>();
		List<BlockPos> connectedLogs = new ArrayList<>();
		BlockPos firstLog = treeLogs.contains(origin)
				? origin.immutable()
				: treeLogs.stream().min((first, second) -> Double.compare(first.distSqr(origin), second.distSqr(origin))).orElseThrow();
		queue.add(firstLog);
		visited.add(firstLog);

		while (!queue.isEmpty() && connectedLogs.size() < MAX_CONNECTED_LOGS) {
			BlockPos logPos = queue.removeFirst();
			connectedLogs.add(logPos);
			for (Direction direction : Direction.values()) {
				BlockPos next = logPos.relative(direction);
				if (treeLogs.contains(next) && visited.add(next)) {
					queue.addLast(next);
				}
			}
		}

		List<LogCandidate> candidates = new ArrayList<>();
		for (BlockPos logPos : connectedLogs) {
			List<Direction> freeSides = new ArrayList<>(4);
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				if (context.level().getBlockState(logPos.relative(direction)).isAir()) {
					freeSides.add(direction);
				}
			}
			if (!freeSides.isEmpty()) {
				candidates.add(new LogCandidate(logPos, List.copyOf(freeSides)));
			}
		}
		return candidates;
	}

	private static boolean isWithinTreeBounds(BlockPos origin, BlockPos pos) {
		int dx = pos.getX() - origin.getX();
		int dy = pos.getY() - origin.getY();
		int dz = pos.getZ() - origin.getZ();
		return dx * dx + dz * dz <= MAX_HORIZONTAL_DISTANCE * MAX_HORIZONTAL_DISTANCE
				&& dy >= MIN_VERTICAL_OFFSET
				&& dy <= MAX_VERTICAL_OFFSET;
	}

	private record LogCandidate(BlockPos logPos, List<Direction> freeSides) {
	}
}
