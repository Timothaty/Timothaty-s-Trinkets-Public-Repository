package net.timothaty.timothatystrinkets.mechanics.echo.formation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

public final class EchoSphereFormationVisuals {
	private static final DustParticleOptions ECHO_DUST = new DustParticleOptions(
			new Vector3f(0x29 / 255.0F, 0xDF / 255.0F, 0xEB / 255.0F),
			0.9F
	);

	private EchoSphereFormationVisuals() {
	}

	public static void spawnSonicTransfer(ServerLevel level, ServerPlayer player, BlockPos spherePos) {
		Vec3 start = player.position().add(0.0D, player.getBbHeight() * 0.55D, 0.0D);
		Vec3 end = Vec3.atCenterOf(spherePos);
		int steps = Math.max(2, Mth.ceil(start.distanceTo(end) * 7.0D));

		for (int step = 0; step <= steps; step++) {
			double progress = step / (double) steps;
			Vec3 point = start.lerp(end, progress);
			level.sendParticles(ECHO_DUST, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}

		level.sendParticles(ECHO_DUST, end.x, end.y, end.z, 20, 0.22D, 0.22D, 0.22D, 0.025D);
		level.sendParticles(ParticleTypes.SCULK_SOUL, end.x, end.y, end.z, 8, 0.25D, 0.25D, 0.25D, 0.025D);
	}
}
