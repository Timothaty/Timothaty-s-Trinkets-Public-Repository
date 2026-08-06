package net.timothaty.timothatystrinkets.client.beatific_pallium;

import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.beatific_pallium.BeatificPalliumData;
import net.timothaty.timothatystrinkets.network.BeatificPalliumShatterMessage;
import net.timothaty.timothatystrinkets.particle.TintedShardParticleOptions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

public final class BeatificPalliumShatterVisuals {
	private static final int MIN_SHARDS = 24;
	private static final int SHARD_COUNT_VARIATION = 5;
	private static final int LIGHT_GOLD_RGB = 0xFFF3B0;

	private BeatificPalliumShatterVisuals() {
	}

	public static void spawn(BeatificPalliumShatterMessage message) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null || message == null)
			return;

		RandomSource random = RandomSource.create(message.seed());
		Vec3 origin = new Vec3(message.originX(), message.originY(), message.originZ());
		Vec3 inheritedVelocity = new Vec3(
				message.inheritedVelocityX(),
				message.inheritedVelocityY(),
				message.inheritedVelocityZ()
		).scale(0.35D);
		int shardCount = MIN_SHARDS + random.nextInt(SHARD_COUNT_VARIATION);
		for (int index = 0; index < shardCount; index++) {
			SurfacePoint surface = randomSurfacePoint(random);
			Vec3 worldOffset = rotateAroundY(surface.point(), message.bodyYaw());
			Vec3 worldNormal = rotateAroundY(surface.normal(), message.bodyYaw());

			double outwardSpeed = Mth.lerp(random.nextDouble(), 0.14D, 0.30D);
			Vec3 tangentJitter = tangentJitter(random, surface.normal());
			Vec3 localVelocity = surface.normal().scale(outwardSpeed).add(tangentJitter);
			Vec3 velocity = rotateAroundY(localVelocity, message.bodyYaw())
					.add(0.0D, Mth.lerp(random.nextDouble(), 0.04D, 0.15D), 0.0D)
					.add(inheritedVelocity);
			float scale = Mth.lerp(random.nextFloat(), 0.08F, 0.14F);
			int color = varyGold(message.rgb(), random);
			TintedShardParticleOptions options = new TintedShardParticleOptions(toVector(color), scale);
			Vec3 position = origin.add(worldOffset).add(worldNormal.scale(0.002D));
			level.addParticle(options, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
		}
	}

	private static SurfacePoint randomSurfacePoint(RandomSource random) {
		double halfSize = BeatificPalliumData.SHELL_HALF_SIZE;
		double first = Mth.lerp(random.nextDouble(), -halfSize, halfSize);
		double second = Mth.lerp(random.nextDouble(), -halfSize, halfSize);
		return switch (random.nextInt(6)) {
			case 0 -> new SurfacePoint(new Vec3(halfSize, first, second), new Vec3(1.0D, 0.0D, 0.0D));
			case 1 -> new SurfacePoint(new Vec3(-halfSize, first, second), new Vec3(-1.0D, 0.0D, 0.0D));
			case 2 -> new SurfacePoint(new Vec3(first, halfSize, second), new Vec3(0.0D, 1.0D, 0.0D));
			case 3 -> new SurfacePoint(new Vec3(first, -halfSize, second), new Vec3(0.0D, -1.0D, 0.0D));
			case 4 -> new SurfacePoint(new Vec3(first, second, halfSize), new Vec3(0.0D, 0.0D, 1.0D));
			default -> new SurfacePoint(new Vec3(first, second, -halfSize), new Vec3(0.0D, 0.0D, -1.0D));
		};
	}

	private static Vec3 tangentJitter(RandomSource random, Vec3 normal) {
		double x = Mth.lerp(random.nextDouble(), -0.08D, 0.08D);
		double y = Mth.lerp(random.nextDouble(), -0.08D, 0.08D);
		double z = Mth.lerp(random.nextDouble(), -0.08D, 0.08D);
		Vec3 jitter = new Vec3(x, y, z);
		return jitter.subtract(normal.scale(jitter.dot(normal)));
	}

	private static Vec3 rotateAroundY(Vec3 vector, float bodyYaw) {
		double radians = Math.toRadians(bodyYaw);
		double cos = Math.cos(radians);
		double sin = Math.sin(radians);
		return new Vec3(
				vector.x * cos - vector.z * sin,
				vector.y,
				vector.x * sin + vector.z * cos
		);
	}

	private static int varyGold(int baseRgb, RandomSource random) {
		float blend = random.nextFloat() * 0.45F;
		int red = Math.round(Mth.lerp(blend, baseRgb >> 16 & 0xFF, LIGHT_GOLD_RGB >> 16 & 0xFF));
		int green = Math.round(Mth.lerp(blend, baseRgb >> 8 & 0xFF, LIGHT_GOLD_RGB >> 8 & 0xFF));
		int blue = Math.round(Mth.lerp(blend, baseRgb & 0xFF, LIGHT_GOLD_RGB & 0xFF));
		return red << 16 | green << 8 | blue;
	}

	private static Vector3f toVector(int rgb) {
		return new Vector3f(
				(rgb >> 16 & 0xFF) / 255.0F,
				(rgb >> 8 & 0xFF) / 255.0F,
				(rgb & 0xFF) / 255.0F
		);
	}

	private record SurfacePoint(Vec3 point, Vec3 normal) {
	}
}
