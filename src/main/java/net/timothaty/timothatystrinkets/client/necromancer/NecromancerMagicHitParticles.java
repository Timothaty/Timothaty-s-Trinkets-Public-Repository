package net.timothaty.timothatystrinkets.client.necromancer;

import net.timothaty.timothatystrinkets.network.NecromancerMagicHitMessage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import org.joml.Vector3f;

public final class NecromancerMagicHitParticles {
	private static final DustParticleOptions MAGIC_DAMAGE_DUST = new DustParticleOptions(
			new Vector3f(0.42F, 0.0F, 0.72F),
			1.25F
	);

	private NecromancerMagicHitParticles() {
	}

	public static void spawn(NecromancerMagicHitMessage message) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null || message == null)
			return;

		Entity resolved = level.getEntity(message.targetEntityId());
		double targetX = message.fallbackX();
		double targetY = message.fallbackY();
		double targetZ = message.fallbackZ();
		float targetWidth = message.targetWidth();
		float targetHeight = message.targetHeight();
		if (resolved instanceof LivingEntity living && !living.isRemoved()) {
			targetX = living.getX();
			targetY = living.getY();
			targetZ = living.getZ();
			targetWidth = living.getBbWidth();
			targetHeight = living.getBbHeight();
		}

		RandomSource random = RandomSource.create(message.seed());
		int particleCount = 24 + random.nextInt(9);
		double bodyRadius = Math.max(targetWidth * 0.38D, 0.22D);
		double bodyHeight = Math.max(targetHeight, 0.75D);
		for (int i = 0; i < particleCount; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0D;
			double spawnRadius = random.nextDouble() * bodyRadius;
			double horizontalSpeed = 0.045D + random.nextDouble() * 0.065D;
			double ySpeed = 0.012D + random.nextDouble() * 0.055D;
			level.addParticle(
					MAGIC_DAMAGE_DUST,
					targetX + Math.cos(angle) * spawnRadius,
					targetY + 0.12D + random.nextDouble() * bodyHeight * 0.82D,
					targetZ + Math.sin(angle) * spawnRadius,
					Math.cos(angle) * horizontalSpeed,
					ySpeed,
					Math.sin(angle) * horizontalSpeed
			);
		}
	}
}
