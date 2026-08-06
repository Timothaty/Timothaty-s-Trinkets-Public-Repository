package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.DebtlordEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

final class DebtlordChainTrajectory {
	private static final double SOURCE_FORWARD_OFFSET = 0.30D;
	private static final double SOURCE_SIDE_OFFSET_FACTOR = 0.42D;
	private static final double SOURCE_HEIGHT_FACTOR = 0.62D;
	private static final double TARGET_LOW_HEIGHT_FACTOR = 0.34D;
	private static final double TARGET_CENTER_HEIGHT_FACTOR = 0.52D;
	private static final double TARGET_HIGH_HEIGHT_FACTOR = 0.70D;

	private DebtlordChainTrajectory() {
	}

	static boolean hasClearPath(DebtlordEntity debtlord, LivingEntity target) {
		if (!(debtlord.level() instanceof ServerLevel level))
			return false;

		float bodyYaw = debtlord.yBodyRot * Mth.DEG_TO_RAD;
		double forwardX = -Mth.sin(bodyYaw);
		double forwardZ = Mth.cos(bodyYaw);
		double rightX = Mth.cos(bodyYaw);
		double rightZ = Mth.sin(bodyYaw);
		double centerX = debtlord.getX() + forwardX * SOURCE_FORWARD_OFFSET;
		double centerY = debtlord.getBoundingBox().minY + debtlord.getBbHeight() * SOURCE_HEIGHT_FACTOR;
		double centerZ = debtlord.getZ() + forwardZ * SOURCE_FORWARD_OFFSET;
		double sideOffset = debtlord.getBbWidth() * SOURCE_SIDE_OFFSET_FACTOR;

		// Fixed left/right hand approximations keep server collision independent of model bones.
		Vec3 leftSource = new Vec3(centerX - rightX * sideOffset, centerY, centerZ - rightZ * sideOffset);
		Vec3 rightSource = new Vec3(centerX + rightX * sideOffset, centerY, centerZ + rightZ * sideOffset);
		AABB targetBounds = target.getBoundingBox();
		double targetX = (targetBounds.minX + targetBounds.maxX) * 0.5D;
		double targetZ = (targetBounds.minZ + targetBounds.maxZ) * 0.5D;
		Vec3 lowTarget = new Vec3(targetX, Mth.lerp(TARGET_LOW_HEIGHT_FACTOR, targetBounds.minY, targetBounds.maxY), targetZ);
		Vec3 centerTarget = new Vec3(targetX, Mth.lerp(TARGET_CENTER_HEIGHT_FACTOR, targetBounds.minY, targetBounds.maxY), targetZ);
		Vec3 highTarget = new Vec3(targetX, Mth.lerp(TARGET_HIGH_HEIGHT_FACTOR, targetBounds.minY, targetBounds.maxY), targetZ);

		return canReachTarget(level, debtlord, leftSource, lowTarget, centerTarget, highTarget)
			&& canReachTarget(level, debtlord, rightSource, lowTarget, centerTarget, highTarget);
	}

	private static boolean canReachTarget(ServerLevel level, DebtlordEntity debtlord, Vec3 source,
			Vec3 lowTarget, Vec3 centerTarget, Vec3 highTarget) {
		return isUnblocked(level, debtlord, source, centerTarget)
			|| isUnblocked(level, debtlord, source, highTarget)
			|| isUnblocked(level, debtlord, source, lowTarget);
	}

	private static boolean isUnblocked(ServerLevel level, DebtlordEntity debtlord, Vec3 source, Vec3 target) {
		ClipContext context = new ClipContext(source, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, debtlord);
		return level.clip(context).getType() == HitResult.Type.MISS;
	}
}
