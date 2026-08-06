package net.timothaty.timothatystrinkets.client.wrath_of_the_wicked;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class WrathOfTheWickedClientParticles {
	private static final DustParticleOptions LASER_DUST = new DustParticleOptions(
			new Vector3f(0xB5 / 255.0F, 0x05 / 255.0F, 0x18 / 255.0F),
			0.72F
	);
	private static final double LASER_PARTICLE_SPACING = 0.16D;

	private WrathOfTheWickedClientParticles() {
	}

	public static void emitLaser(
			double startX,
			double startY,
			double startZ,
			double targetX,
			double targetY,
			double targetZ
	) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null)
			return;

		Vec3 start = new Vec3(startX, startY, startZ);
		Vec3 line = new Vec3(targetX, targetY, targetZ).subtract(start);
		double length = line.length();
		int steps = Math.max(1, (int) Math.ceil(length / LASER_PARTICLE_SPACING));

		for (int step = 0; step <= steps; step++) {
			Vec3 point = start.add(line.scale(step / (double) steps));
			level.addParticle(LASER_DUST, point.x, point.y, point.z, 0.0D, 0.0D, 0.0D);
		}
	}
}
