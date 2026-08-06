package net.timothaty.timothatystrinkets.client.angels_shroud;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.angels_shroud.AngelsShroudData;
import net.timothaty.timothatystrinkets.network.AngelsShroudEndBurstMessage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class AngelsShroudEndBurstClient {
	private AngelsShroudEndBurstClient() {
	}

	public static void spawn(AngelsShroudEndBurstMessage message) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return;

		RandomSource random = RandomSource.create(message.seed());
		float bodyHeight = Math.max(0.5F, message.bodyHeight());
		for (int index = 0; index < AngelsShroudData.END_BURST_PARTICLE_COUNT; index++) {
			float angle = random.nextFloat() * Mth.TWO_PI;
			double radius = 0.12D + random.nextDouble() * 0.26D;
			double cos = Mth.cos(angle);
			double sin = Mth.sin(angle);
			double x = message.x() + cos * radius;
			double y = message.y() + 0.12D + random.nextDouble() * bodyHeight * 0.82D;
			double z = message.z() + sin * radius;
			boolean pale = random.nextBoolean();
			double red = pale ? AngelsShroudData.PALE_GOLD_RED : AngelsShroudData.GOLD_RED;
			double green = pale ? AngelsShroudData.PALE_GOLD_GREEN : AngelsShroudData.GOLD_GREEN;
			double blue = pale ? AngelsShroudData.PALE_GOLD_BLUE : AngelsShroudData.GOLD_BLUE;

			Particle particle = minecraft.particleEngine.createParticle(
					TimothatysTrinketsModParticleTypes.DOT.get(),
					x,
					y,
					z,
					red,
					green,
					blue
			);
			if (particle == null)
				continue;

			double speed = 0.055D + random.nextDouble() * 0.075D;
			particle.setParticleSpeed(
					cos * speed + message.inheritedVelocityX() * 0.18D,
					0.045D + random.nextDouble() * 0.070D + message.inheritedVelocityY() * 0.18D,
					sin * speed + message.inheritedVelocityZ() * 0.18D
			);
		}
	}
}
