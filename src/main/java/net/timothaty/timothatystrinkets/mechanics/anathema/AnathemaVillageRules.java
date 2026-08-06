package net.timothaty.timothatystrinkets.mechanics.anathema;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class AnathemaVillageRules {
	public static final TagKey<Block> VILLAGE_WORKSTATIONS = TagKey.create(
		Registries.BLOCK,
		ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "village_workstations")
	);

	private static final int ARMORER_OWNERSHIP_RADIUS = 16;

	private AnathemaVillageRules() {
	}

	public static boolean isVillageTerritory(ServerLevel level, BlockPos pos) {
		return level != null && pos != null && level.isCloseToVillage(pos, 2);
	}

	public static boolean isVillageChest(ServerLevel level, BlockPos pos) {
		if (!isVillageTerritory(level, pos) || AnathemaVillageClaims.get(level).isPlayerPlaced(pos))
			return false;
		return level.getBlockState(pos).getBlock() instanceof ChestBlock;
	}

	public static boolean isVillageWorkstation(ServerLevel level, BlockPos pos, BlockState state) {
		if (!isVillageTerritory(level, pos) || AnathemaVillageClaims.get(level).isPlayerPlaced(pos))
			return false;

		if (state != null && state.is(VILLAGE_WORKSTATIONS))
			return true;

		return level.getPoiManager().getType(pos).map(type -> type.is(PoiTypeTags.ACQUIRABLE_JOB_SITE)).orElse(false);
	}

	public static boolean isVillageHouseBlock(ServerLevel level, BlockPos pos) {
		if (!isVillageTerritory(level, pos) || AnathemaVillageClaims.get(level).isPlayerPlaced(pos))
			return false;

		BlockState state = level.getBlockState(pos);
		return !state.isAir() && state.isFlammable(level, pos, Direction.UP);
	}

	public static boolean isArmorerOwnedArmorStand(ServerLevel level, BlockPos pos, boolean playerPlaced) {
		if (playerPlaced || !isVillageTerritory(level, pos))
			return false;

		return level.getPoiManager().findClosest(
			type -> type.is(PoiTypes.ARMORER),
			pos,
			ARMORER_OWNERSHIP_RADIUS,
			net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY
		).isPresent();
	}
}
