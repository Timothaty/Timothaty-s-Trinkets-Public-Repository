package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.DebtlordEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DebtlordPillarRepositioning {
	private static final double[] BACK_OFFSETS = {3.0D, 2.5D, 3.5D, 3.0D, 3.0D, 2.6D, 2.6D, 3.2D, 3.2D, 2.8D};
	private static final double[] SIDE_OFFSETS = {0.0D, 0.0D, 0.0D, 1.1D, -1.1D, 1.45D, -1.45D, 0.65D, -0.65D, 0.0D};
	private static final double[] HEIGHT_OFFSETS = {0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.65D, 0.65D, -0.65D, -0.65D, 1.0D};

	private DebtlordPillarRepositioning() {
	}

	public static Vec3 findPosition(DebtlordEntity debtlord, LivingEntity target) {
		if (!(debtlord.level() instanceof ServerLevel level))
			return null;

		Vec3 look = target.getLookAngle();
		double lookLengthSqr = look.x * look.x + look.z * look.z;
		double forwardX;
		double forwardZ;
		if (lookLengthSqr > 1.0E-6D) {
			double inverseLength = Mth.invSqrt((float) lookLengthSqr);
			forwardX = look.x * inverseLength;
			forwardZ = look.z * inverseLength;
		} else {
			float yaw = target.getYRot() * Mth.DEG_TO_RAD;
			forwardX = -Mth.sin(yaw);
			forwardZ = Mth.cos(yaw);
		}
		double sideX = -forwardZ;
		double sideZ = forwardX;

		for (int index = 0; index < BACK_OFFSETS.length; index++) {
			double x = target.getX() - forwardX * BACK_OFFSETS[index] + sideX * SIDE_OFFSETS[index];
			double y = target.getBoundingBox().minY + HEIGHT_OFFSETS[index];
			double z = target.getZ() - forwardZ * BACK_OFFSETS[index] + sideZ * SIDE_OFFSETS[index];
			Vec3 candidate = new Vec3(x, y, z);
			if (isSafe(level, debtlord, candidate))
				return candidate;
		}
		return null;
	}

	private static boolean isSafe(ServerLevel level, DebtlordEntity debtlord, Vec3 candidate) {
		AABB currentBounds = debtlord.getBoundingBox();
		AABB candidateBounds = currentBounds.move(
			candidate.x - debtlord.getX(),
			candidate.y - debtlord.getY(),
			candidate.z - debtlord.getZ()
		).deflate(1.0E-7D);
		if (!areBoundingChunksLoaded(level, candidateBounds)
			|| !level.getWorldBorder().isWithinBounds(candidateBounds)
			|| !level.noCollision(debtlord, candidateBounds)
			|| level.containsAnyLiquid(candidateBounds))
			return false;
		return true;
	}

	private static boolean areBoundingChunksLoaded(ServerLevel level, AABB bounds) {
		int minX = Mth.floor(bounds.minX);
		int maxX = Mth.floor(bounds.maxX - 1.0E-7D);
		int minZ = Mth.floor(bounds.minZ);
		int maxZ = Mth.floor(bounds.maxZ - 1.0E-7D);
		int y = Mth.floor(bounds.minY);
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(minX, y, minZ);
		if (!level.hasChunkAt(cursor))
			return false;
		cursor.set(maxX, y, minZ);
		if (!level.hasChunkAt(cursor))
			return false;
		cursor.set(minX, y, maxZ);
		if (!level.hasChunkAt(cursor))
			return false;
		cursor.set(maxX, y, maxZ);
		return level.hasChunkAt(cursor);
	}
}
