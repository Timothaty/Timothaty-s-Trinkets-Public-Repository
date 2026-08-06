package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

public final class NecromancerVillageVillagerScanner {
	private NecromancerVillageVillagerScanner() {
	}

	public static int countUncorruptedVillagers(ServerLevel level, BlockPos villageCenter, int horizontalRadius, int verticalRadius) {
		int[] count = {0};
		level.getEntities().get(
			EntityTypeTest.forClass(Villager.class),
			createScanBounds(villageCenter, horizontalRadius, verticalRadius),
			villager -> {
				if (isUncorruptedVillagerInsideArea(villager, villageCenter, horizontalRadius, verticalRadius)) {
					count[0]++;
				}
				return AbortableIterationConsumer.Continuation.CONTINUE;
			}
		);
		return count[0];
	}

	public static boolean hasUncorruptedVillagers(ServerLevel level, BlockPos villageCenter, int horizontalRadius, int verticalRadius) {
		boolean[] found = {false};
		level.getEntities().get(
			EntityTypeTest.forClass(Villager.class),
			createScanBounds(villageCenter, horizontalRadius, verticalRadius),
			villager -> {
				if (!isUncorruptedVillagerInsideArea(villager, villageCenter, horizontalRadius, verticalRadius)) {
					return AbortableIterationConsumer.Continuation.CONTINUE;
				}

				found[0] = true;
				return AbortableIterationConsumer.Continuation.ABORT;
			}
		);
		return found[0];
	}

	public static boolean isInsideVillageArea(BlockPos pos, BlockPos center, int horizontalRadius, int verticalRadius) {
		if (Math.abs(pos.getY() - center.getY()) > verticalRadius) {
			return false;
		}

		double dx = pos.getX() - center.getX();
		double dz = pos.getZ() - center.getZ();
		double radiusSqr = (double) horizontalRadius * horizontalRadius;
		return dx * dx + dz * dz <= radiusSqr;
	}

	private static boolean isUncorruptedVillagerInsideArea(Villager villager, BlockPos center, int horizontalRadius, int verticalRadius) {
		return villager.isAlive()
			&& !villager.hasEffect(TimothatysTrinketsModMobEffects.UNDEADIFICATION)
			&& isInsideVillageArea(villager.blockPosition(), center, horizontalRadius, verticalRadius);
	}

	private static AABB createScanBounds(BlockPos center, int horizontalRadius, int verticalRadius) {
		return new AABB(center).inflate(horizontalRadius, verticalRadius, horizontalRadius);
	}
}
