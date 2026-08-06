package net.timothaty.timothatystrinkets.client.gorge;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.network.GorgeDigestiveSurgeVisualMessage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class GorgeDigestiveSurgeClient {
	private static final double DOT_RED = 75.0D / 255.0D;
	private static final double DOT_GREEN = 3.0D / 255.0D;
	private static final double DOT_BLUE = 13.0D / 255.0D;

	private GorgeDigestiveSurgeClient() {
	}

	public static void handle(GorgeDigestiveSurgeVisualMessage message) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null)
			return;

		Entity player = level.getEntity(message.playerEntityId());
		if (player == null)
			return;

		RandomSource random = RandomSource.create(message.seed());
		float partialTick = minecraft.getTimer()
				.getGameTimeDeltaPartialTick(false);
		Vec3 chest = new Vec3(
				Mth.lerp(partialTick, player.xo, player.getX()),
				Mth.lerp(partialTick, player.yo, player.getY())
						+ player.getBbHeight() * 0.62D,
				Mth.lerp(partialTick, player.zo, player.getZ())
		);
		int count = 2 + random.nextInt(2);
		for (int index = 0; index < count; index++) {
			double angle = random.nextDouble() * Math.PI * 2.0D;
			double radius = 0.18D + random.nextDouble() * 0.14D;
			Vec3 position = chest.add(
					Math.cos(angle) * radius,
					(random.nextDouble() - 0.5D) * 0.26D,
					Math.sin(angle) * radius
			);
			Particle particle = minecraft.particleEngine.createParticle(
					TimothatysTrinketsModParticleTypes.DOT.get(),
					position.x,
					position.y,
					position.z,
					DOT_RED,
					DOT_GREEN,
					DOT_BLUE
			);
			if (particle == null)
				continue;

			particle.setLifetime(10 + random.nextInt(5));
			Vec3 inward = chest.subtract(position);
			if (inward.lengthSqr() > 1.0E-6D)
				inward = inward.normalize();
			double speed = 0.020D + random.nextDouble() * 0.014D;
			particle.setParticleSpeed(
					inward.x * speed,
					inward.y * speed + 0.006D,
					inward.z * speed
			);
		}
	}
}
