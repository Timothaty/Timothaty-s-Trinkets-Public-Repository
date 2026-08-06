package net.timothaty.timothatystrinkets.mechanics.cleansing.ritual;

import net.timothaty.timothatystrinkets.block.IncenseTrailBlock;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModBlocks;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RedstoneSide;

import java.util.Set;

public final class CleansingRitualValidator {
	private CleansingRitualValidator() {
	}

	public static Match findFreshPattern(ServerLevel level, BlockPos clickedPos) {
		for (int routeIndex = 0; routeIndex < CleansingRitualPattern.CLOCKWISE_ROUTE.size(); routeIndex++) {
			BlockPos offset = CleansingRitualPattern.CLOCKWISE_ROUTE.get(routeIndex);
			BlockPos center = clickedPos.offset(-offset.getX(), 0, -offset.getZ());
			if (validateFreshPattern(level, center)) return new Match(center.immutable(), routeIndex);
		}
		return null;
	}

	public static boolean validateFreshPattern(ServerLevel level, BlockPos center) {
		if (!allRequiredChunksLoaded(level, center) || !hasPotWithFourIncense(level, center) || !hasLitCandles(level, center)) {
			return false;
		}
		BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
		for (BlockPos offset : CleansingRitualPattern.CLOCKWISE_ROUTE) {
			mutable.set(center.getX() + offset.getX(), center.getY(), center.getZ() + offset.getZ());
			BlockState state = level.getBlockState(mutable);
			if (!isIncense(state, false) || !hasExpectedFlatConnections(state, offset)) return false;
		}
		return true;
	}

	public static boolean validateBurningBase(ServerLevel level, BlockPos center) {
		return allRequiredChunksLoaded(level, center) && hasEmptyPot(level, center) && hasLitCandles(level, center);
	}

	public static boolean validateFreshSegment(ServerLevel level, BlockPos center, int routeIndex) {
		BlockPos offset = CleansingRitualPattern.CLOCKWISE_ROUTE.get(routeIndex);
		BlockPos pos = center.offset(offset.getX(), 0, offset.getZ());
		return isIncense(level.getBlockState(pos), false);
	}

	public static boolean validateCompletedPattern(ServerLevel level, BlockPos center) {
		if (!validateBurningBase(level, center)) return false;
		BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
		for (BlockPos offset : CleansingRitualPattern.CLOCKWISE_ROUTE) {
			mutable.set(center.getX() + offset.getX(), center.getY(), center.getZ() + offset.getZ());
			BlockState state = level.getBlockState(mutable);
			if (!isIncense(state, true) || !hasExpectedFlatConnections(state, offset)) return false;
		}
		return true;
	}

	public static boolean allRequiredChunksLoaded(ServerLevel level, BlockPos center) {
		int minChunkX = SectionPos.blockToSectionCoord(center.getX() - 2);
		int maxChunkX = SectionPos.blockToSectionCoord(center.getX() + 2);
		int minChunkZ = SectionPos.blockToSectionCoord(center.getZ() - 2);
		int maxChunkZ = SectionPos.blockToSectionCoord(center.getZ() + 2);
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				if (!level.getChunkSource().hasChunk(chunkX, chunkZ)) return false;
			}
		}
		return true;
	}

	public static DecoratedPotBlockEntity getPot(ServerLevel level, BlockPos center) {
		if (!level.getBlockState(center).is(Blocks.DECORATED_POT)) return null;
		return level.getBlockEntity(center) instanceof DecoratedPotBlockEntity pot ? pot : null;
	}

	private static boolean hasPotWithFourIncense(ServerLevel level, BlockPos center) {
		DecoratedPotBlockEntity pot = getPot(level, center);
		if (pot == null) return false;
		ItemStack stack = pot.getTheItem();
		return stack.is(TimothatysTrinketsModItems.INCENSE.get()) && stack.getCount() == 4;
	}

	private static boolean hasEmptyPot(ServerLevel level, BlockPos center) {
		DecoratedPotBlockEntity pot = getPot(level, center);
		return pot != null && pot.getTheItem().isEmpty();
	}

	private static boolean hasLitCandles(ServerLevel level, BlockPos center) {
		BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
		for (BlockPos offset : CleansingRitualPattern.CANDLE_OFFSETS) {
			mutable.set(center.getX() + offset.getX(), center.getY(), center.getZ() + offset.getZ());
			BlockState state = level.getBlockState(mutable);
			if (!(state.getBlock() instanceof CandleBlock) || !state.getValue(AbstractCandleBlock.LIT)) return false;
		}
		return true;
	}

	private static boolean isIncense(BlockState state, boolean ash) {
		return state.is(TimothatysTrinketsModBlocks.INCENSE.get()) && state.getValue(IncenseTrailBlock.ASH) == ash;
	}

	private static boolean hasExpectedFlatConnections(BlockState state, BlockPos offset) {
		Set<Direction> expected = CleansingRitualPattern.EXPECTED_CONNECTIONS.get(offset);
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			RedstoneSide actual = state.getValue(IncenseTrailBlock.PROPERTY_BY_DIRECTION.get(direction));
			if (expected.contains(direction)) {
				if (actual != RedstoneSide.SIDE) return false;
			} else if (actual != RedstoneSide.NONE) {
				return false;
			}
		}
		return true;
	}

	public record Match(BlockPos center, int startRouteIndex) {
	}
}
