package net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class DuelistGuardParticles {
	private static final int CENTER_PARRY_SPARK_MIN_COUNT = 18;
	private static final int CENTER_PARRY_SPARK_RANDOM_COUNT = 10;
	private static final int SIDE_DEFLECT_SPARK_MIN_COUNT = 14;
	private static final int SIDE_DEFLECT_SPARK_RANDOM_COUNT = 8;

	private DuelistGuardParticles() {
	}

	public static void spawnSideDeflectSparks(Player defender, LivingEntity attacker, DuelistGuardDirection direction) {
		if (defender == null || attacker == null || !(defender.level() instanceof ServerLevel serverLevel))
			return;

		RandomSource random = defender.getRandom();
		SimpleParticleType particle = TimothatysTrinketsModParticleTypes.SPARK.get();
		Vec3 origin = getImpactPoint(defender, attacker);
		Vec3 burstDirection = getBurstDirection(defender, attacker, direction);
		spawnSparks(serverLevel, particle, random, origin, burstDirection, SIDE_DEFLECT_SPARK_MIN_COUNT, SIDE_DEFLECT_SPARK_RANDOM_COUNT);
	}

	public static void spawnCenterParrySparks(Player defender, LivingEntity attacker) {
		if (defender == null || attacker == null || !(defender.level() instanceof ServerLevel serverLevel))
			return;

		RandomSource random = defender.getRandom();
		SimpleParticleType particle = TimothatysTrinketsModParticleTypes.SPARK.get();
		Vec3 origin = getImpactPoint(defender, attacker);
		Vec3 burstDirection = getBurstDirection(defender, attacker, DuelistGuardDirection.CENTER);
		spawnSparks(serverLevel, particle, random, origin, burstDirection, CENTER_PARRY_SPARK_MIN_COUNT, CENTER_PARRY_SPARK_RANDOM_COUNT);
	}

	private static void spawnSparks(ServerLevel serverLevel, SimpleParticleType particle, RandomSource random, Vec3 origin, Vec3 burstDirection, int minCount, int randomCount) {
		int count = minCount + random.nextInt(randomCount + 1);
		for (int i = 0; i < count; i++) {
			Vec3 sparkDirection = randomSparkDirection(burstDirection, random);
			double speed = 0.20D + random.nextDouble() * 0.24D;
			double x = origin.x + (random.nextDouble() - 0.5D) * 0.10D;
			double y = origin.y + (random.nextDouble() - 0.5D) * 0.12D;
			double z = origin.z + (random.nextDouble() - 0.5D) * 0.10D;
			serverLevel.sendParticles(particle, x, y, z, 0, sparkDirection.x * speed, sparkDirection.y * speed, sparkDirection.z * speed, 1.0D);
		}
	}

	private static Vec3 getImpactPoint(Player defender, LivingEntity attacker) {
		Vec3 toAttacker = attacker.position().subtract(defender.position());
		Vec3 horizontal = new Vec3(toAttacker.x, 0.0D, toAttacker.z);
		Vec3 forward = horizontal.lengthSqr() > 1.0E-5D ? horizontal.normalize() : defender.getLookAngle();
		double reach = Math.max(0.42D, defender.getBbWidth() * 0.72D);
		return defender.position().add(forward.scale(reach)).add(0.0D, defender.getBbHeight() * 0.58D, 0.0D);
	}

	private static Vec3 getBurstDirection(Player defender, LivingEntity attacker, DuelistGuardDirection direction) {
		Vec3 toAttacker = attacker.position().subtract(defender.position());
		Vec3 horizontal = new Vec3(toAttacker.x, 0.0D, toAttacker.z);
		Vec3 outward = horizontal.lengthSqr() > 1.0E-5D ? horizontal.normalize() : defender.getLookAngle();
		if (direction == DuelistGuardDirection.CENTER) {
			Vec3 burst = outward.scale(0.92D).add(0.0D, 0.30D, 0.0D);
			return burst.lengthSqr() > 1.0E-5D ? burst.normalize() : new Vec3(0.0D, 1.0D, 0.0D);
		}
		Vec3 right = getRightVector(defender);
		double sideSign = direction == DuelistGuardDirection.LEFT ? -1.0D : 1.0D;
		Vec3 burst = outward.scale(0.86D).add(right.scale(sideSign * 0.34D)).add(0.0D, 0.24D, 0.0D);
		return burst.lengthSqr() > 1.0E-5D ? burst.normalize() : new Vec3(0.0D, 1.0D, 0.0D);
	}

	private static Vec3 getRightVector(Player defender) {
		Vec3 look = defender.getLookAngle();
		Vec3 right = new Vec3(-look.z, 0.0D, look.x);
		return right.lengthSqr() > 1.0E-5D ? right.normalize() : new Vec3(1.0D, 0.0D, 0.0D);
	}

	private static Vec3 randomSparkDirection(Vec3 baseDirection, RandomSource random) {
		Vec3 base = baseDirection.lengthSqr() > 1.0E-5D ? baseDirection.normalize() : new Vec3(0.0D, 1.0D, 0.0D);
		Vec3 side = new Vec3(-base.z, 0.0D, base.x);
		if (side.lengthSqr() < 1.0E-5D) {
			side = new Vec3(1.0D, 0.0D, 0.0D);
		}
		side = side.normalize();
		Vec3 direction = base.scale(0.75D + random.nextDouble() * 0.55D)
				.add(side.scale((random.nextDouble() - 0.5D) * 1.25D))
				.add(0.0D, 0.12D + random.nextDouble() * 0.54D, 0.0D);
		return direction.lengthSqr() > 1.0E-5D ? direction.normalize() : base;
	}
}
