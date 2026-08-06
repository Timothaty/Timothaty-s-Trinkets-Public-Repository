package net.timothaty.timothatystrinkets.mechanics.echo.formation;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModBlocks;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class EchoSphereFormationHelper {
	private static final int SEARCH_RADIUS = 6;
	private static final double SEARCH_RADIUS_SQUARED = SEARCH_RADIUS * SEARCH_RADIUS;
	private static final String WARDEN_ECHO_SPHERE_CREATED_TAG = "ttr_echo_sphere_created";

	private EchoSphereFormationHelper() {
	}

	public static boolean hasCreatedEchoSphere(Warden warden) {
		return warden.getPersistentData().getBoolean(WARDEN_ECHO_SPHERE_CREATED_TAG);
	}

	public static void markEchoSphereCreated(Warden warden) {
		warden.getPersistentData().putBoolean(WARDEN_ECHO_SPHERE_CREATED_TAG, true);
	}

	public static Optional<BlockPos> findNearestDormantSphere(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		BlockPos origin = player.blockPosition();
		Vec3 playerPosition = player.position();
		BlockPos nearestSpherePos = null;
		double nearestDistanceSquared = SEARCH_RADIUS_SQUARED;
		BlockPos.MutableBlockPos candidate = new BlockPos.MutableBlockPos();

		for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
			for (int y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; y++) {
				for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
					candidate.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
					if (!level.hasChunkAt(candidate)) {
						continue;
					}

					double distanceSquared = playerPosition.distanceToSqr(Vec3.atCenterOf(candidate));
					if (distanceSquared > SEARCH_RADIUS_SQUARED
							|| nearestSpherePos != null && distanceSquared >= nearestDistanceSquared) {
						continue;
					}
					if (level.getBlockState(candidate).is(TimothatysTrinketsModBlocks.DORMANT_SPHERE.get())) {
						nearestSpherePos = candidate.immutable();
						nearestDistanceSquared = distanceSquared;
					}
				}
			}
		}

		return Optional.ofNullable(nearestSpherePos);
	}

	public static boolean awakenDormantSphere(ServerLevel level, BlockPos spherePos) {
		if (!level.hasChunkAt(spherePos)
				|| !level.getBlockState(spherePos).is(TimothatysTrinketsModBlocks.DORMANT_SPHERE.get())) {
			return false;
		}

		if (!level.setBlockAndUpdate(spherePos, TimothatysTrinketsModBlocks.ECHO_SPHERE.get().defaultBlockState())) {
			return false;
		}

		if (!level.getBlockState(spherePos).is(TimothatysTrinketsModBlocks.ECHO_SPHERE.get())) {
			return false;
		}
		level.playSound(
				null,
				spherePos,
				TimothatysTrinketsModSounds.ECHO_SPHERE_TRANSMUTATION.get(),
				SoundSource.BLOCKS,
				1.0F,
				1.0F + level.getRandom().nextFloat() * 0.2F
		);
		return true;
	}
}
