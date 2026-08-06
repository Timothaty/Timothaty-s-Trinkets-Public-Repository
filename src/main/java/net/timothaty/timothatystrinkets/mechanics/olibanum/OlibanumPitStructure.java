package net.timothaty.timothatystrinkets.mechanics.olibanum;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class OlibanumPitStructure {
	public static final TagKey<Block> INSULATION = TagKey.create(
			Registries.BLOCK,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "olibanum_pit_insulation")
	);

	private OlibanumPitStructure() {
	}

	public static boolean isValid(ServerLevel level, BlockPos potPos) {
		if (!areRequiredChunksLoaded(level, potPos)) {
			return false;
		}

		BlockState potState = level.getBlockState(potPos);
		if (!potState.is(Blocks.DECORATED_POT)
				|| !potState.getFluidState().isEmpty()
				|| !(level.getBlockEntity(potPos) instanceof DecoratedPotBlockEntity)) {
			return false;
		}

		BlockPos campfirePos = potPos.below();
		if (!CampfireBlock.isLitCampfire(level.getBlockState(campfirePos))) {
			return false;
		}

		if (!isInsulation(level, potPos.above(), Direction.DOWN)) {
			return false;
		}
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			if (!isInsulation(level, potPos.relative(direction), direction.getOpposite())
					|| !isInsulation(level, campfirePos.relative(direction), direction.getOpposite())) {
				return false;
			}
		}

		return isSafeSupport(level, campfirePos.below());
	}

	public static DecoratedPotBlockEntity getPot(ServerLevel level, BlockPos potPos) {
		if (!level.getBlockState(potPos).is(Blocks.DECORATED_POT)) {
			return null;
		}
		return level.getBlockEntity(potPos) instanceof DecoratedPotBlockEntity pot ? pot : null;
	}

	public static boolean areRequiredChunksLoaded(ServerLevel level, BlockPos potPos) {
		int minChunkX = SectionPos.blockToSectionCoord(potPos.getX() - 1);
		int maxChunkX = SectionPos.blockToSectionCoord(potPos.getX() + 1);
		int minChunkZ = SectionPos.blockToSectionCoord(potPos.getZ() - 1);
		int maxChunkZ = SectionPos.blockToSectionCoord(potPos.getZ() + 1);
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				if (!level.getChunkSource().hasChunk(chunkX, chunkZ)) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean isInsulation(ServerLevel level, BlockPos pos, Direction towardStructure) {
		BlockState state = level.getBlockState(pos);
		return state.is(INSULATION) && isDrySolidAndNonFlammable(level, pos, state, towardStructure);
	}

	private static boolean isSafeSupport(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return isDrySolidAndNonFlammable(level, pos, state, Direction.UP);
	}

	private static boolean isDrySolidAndNonFlammable(ServerLevel level, BlockPos pos, BlockState state,
			Direction towardStructure) {
		if (!state.isFaceSturdy(level, pos, towardStructure) || !state.getFluidState().isEmpty()) {
			return false;
		}
		for (Direction face : Direction.values()) {
			if (state.isFlammable(level, pos, face)) {
				return false;
			}
		}
		return true;
	}
}
