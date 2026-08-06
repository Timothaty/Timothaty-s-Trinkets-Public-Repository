package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.NecromancerEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class NecromancerSummonPlacement {
	private static final int SUMMON_RADIUS = 6;
	private static final double MIN_SUMMON_DISTANCE_FROM_NECROMANCER = 2.0D;
	private static final double MIN_SUMMON_DISTANCE_FROM_NECROMANCER_SQR = MIN_SUMMON_DISTANCE_FROM_NECROMANCER * MIN_SUMMON_DISTANCE_FROM_NECROMANCER;
	private static final double MAX_FOOT_SPACE_SURFACE_HEIGHT = 0.25D;

	private NecromancerSummonPlacement() {
	}

	public static boolean canSummonAtNecromancerPosition(NecromancerEntity necromancer, ServerLevel serverLevel) {
		if (!necromancer.onGround() || necromancer.isInWaterOrBubble()) {
			return false;
		}

		BlockPos origin = necromancer.blockPosition();
		int supportY = origin.getY() - 1;
		BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
		for (int xOffset = -SUMMON_RADIUS; xOffset <= SUMMON_RADIUS; xOffset++) {
			for (int zOffset = -SUMMON_RADIUS; zOffset <= SUMMON_RADIUS; zOffset++) {
				if (xOffset * xOffset + zOffset * zOffset > SUMMON_RADIUS * SUMMON_RADIUS) {
					continue;
				}

				int groundX = origin.getX() + xOffset;
				int groundZ = origin.getZ() + zOffset;
				for (int groundY = supportY; groundY >= supportY - 2; groundY--) {
					if (!Double.isNaN(findSpawnY(necromancer, serverLevel, mutablePos, groundX, groundY, groundZ))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static NecromancerSummonSpot findSummonSpot(NecromancerEntity necromancer, ServerLevel serverLevel, RandomSource random) {
		if (!necromancer.onGround() || necromancer.isInWaterOrBubble()) {
			return null;
		}

		BlockPos origin = necromancer.blockPosition();
		int supportY = origin.getY() - 1;
		BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
		int validCandidateCount = 0;
		int selectedX = 0;
		int selectedY = 0;
		int selectedZ = 0;
		double selectedSpawnY = 0.0D;

		for (int xOffset = -SUMMON_RADIUS; xOffset <= SUMMON_RADIUS; xOffset++) {
			for (int zOffset = -SUMMON_RADIUS; zOffset <= SUMMON_RADIUS; zOffset++) {
				if (xOffset * xOffset + zOffset * zOffset > SUMMON_RADIUS * SUMMON_RADIUS) {
					continue;
				}

				int groundX = origin.getX() + xOffset;
				int groundZ = origin.getZ() + zOffset;
				for (int groundY = supportY; groundY >= supportY - 2; groundY--) {
					double spawnY = findSpawnY(necromancer, serverLevel, mutablePos, groundX, groundY, groundZ);
					if (Double.isNaN(spawnY)) {
						continue;
					}

					validCandidateCount++;
					if (random.nextInt(validCandidateCount) == 0) {
						selectedX = groundX;
						selectedY = groundY;
						selectedZ = groundZ;
						selectedSpawnY = spawnY;
					}
					break;
				}
			}
		}

		return validCandidateCount == 0
				? null
				: new NecromancerSummonSpot(new BlockPos(selectedX, selectedY, selectedZ), selectedSpawnY);
	}

	public static boolean isSummonSpotStillValid(NecromancerEntity necromancer, ServerLevel serverLevel, NecromancerSummonSpot summonSpot) {
		if (summonSpot == null) {
			return false;
		}

		BlockPos groundPos = summonSpot.groundPos();
		double spawnY = findSpawnY(
				necromancer,
				serverLevel,
				new BlockPos.MutableBlockPos(),
				groundPos.getX(),
				groundPos.getY(),
				groundPos.getZ()
		);
		return !Double.isNaN(spawnY) && Math.abs(spawnY - summonSpot.spawnY()) < 0.001D;
	}

	private static double findSpawnY(NecromancerEntity necromancer, ServerLevel serverLevel,
			BlockPos.MutableBlockPos mutablePos, int groundX, int groundY, int groundZ) {
		if (isTooCloseToNecromancer(necromancer, groundX, groundZ)) {
			return Double.NaN;
		}

		mutablePos.set(groundX, groundY, groundZ);
		BlockState groundState = serverLevel.getBlockState(mutablePos);
		if (!groundState.isCollisionShapeFullBlock(serverLevel, mutablePos)) {
			return Double.NaN;
		}

		mutablePos.setY(groundY + 1);
		double footSurfaceHeight = getFootSpaceSurfaceHeight(serverLevel, mutablePos);
		mutablePos.setY(groundY + 2);
		boolean firstBlockClear = hasEmptyCollision(serverLevel, mutablePos);
		mutablePos.setY(groundY + 3);
		if (Double.isNaN(footSurfaceHeight) || !firstBlockClear || !hasEmptyCollision(serverLevel, mutablePos)) {
			return Double.NaN;
		}

		return groundY + 1.0D + footSurfaceHeight;
	}

	private static boolean isTooCloseToNecromancer(NecromancerEntity necromancer, int groundX, int groundZ) {
		double dx = groundX + 0.5D - necromancer.getX();
		double dz = groundZ + 0.5D - necromancer.getZ();
		return dx * dx + dz * dz < MIN_SUMMON_DISTANCE_FROM_NECROMANCER_SQR;
	}

	private static double getFootSpaceSurfaceHeight(ServerLevel serverLevel, BlockPos footPos) {
		VoxelShape collisionShape = serverLevel.getBlockState(footPos).getCollisionShape(serverLevel, footPos);
		if (collisionShape.isEmpty()) {
			return 0.0D;
		}

		double surfaceHeight = collisionShape.max(Direction.Axis.Y);
		return surfaceHeight <= MAX_FOOT_SPACE_SURFACE_HEIGHT ? surfaceHeight : Double.NaN;
	}

	private static boolean hasEmptyCollision(ServerLevel serverLevel, BlockPos pos) {
		return serverLevel.getBlockState(pos).getCollisionShape(serverLevel, pos).isEmpty();
	}
}
