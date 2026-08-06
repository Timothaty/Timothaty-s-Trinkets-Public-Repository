package net.timothaty.timothatystrinkets.mechanics.undead_knight;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.entity.SoulOrbEntity;
import net.timothaty.timothatystrinkets.entity.UndeadKnightEntity;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class UndeadKnightSoulAbsorptionEvents {
	private static final double SOUL_SPAWN_RANGE = 6.0D;
	private static final double SOUL_SPAWN_RANGE_SQR = SOUL_SPAWN_RANGE * SOUL_SPAWN_RANGE;
	private static final int MIN_ORBS_TO_START_CAST = 3;

	private UndeadKnightSoulAbsorptionEvents() {
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		LivingEntity victim = event.getEntity();
		if (!(victim.level() instanceof ServerLevel serverLevel) || !canReleaseSoulOrbs(victim)) {
			return;
		}

		List<UndeadKnightEntity> nearbyKnights = getNearbySoulAbsorbingKnights(serverLevel, victim.position());
		if (nearbyKnights.isEmpty()) {
			return;
		}

		int logicalSoulCount = Math.max(1, Mth.ceil(victim.getMaxHealth() / UndeadKnightSoulAbsorptionData.HEALTH_PER_SOUL_ORB));
		spawnSoulOrbs(serverLevel, victim, logicalSoulCount);
		tryStartNearestKnightToSoulOrbs(serverLevel, nearbyKnights);
	}

	private static boolean canReleaseSoulOrbs(LivingEntity victim) {
		return victim != null
				&& !(victim instanceof UndeadKnightEntity)
				&& !victim.isInvertedHealAndHarm()
				&& !victim.getType().is(EntityTypeTags.UNDEAD)
				&& victim.getMaxHealth() > 0.0F;
	}

	private static List<UndeadKnightEntity> getNearbySoulAbsorbingKnights(ServerLevel level, Vec3 center) {
		AABB searchBox = new AABB(center, center).inflate(SOUL_SPAWN_RANGE);
		return level.getEntitiesOfClass(UndeadKnightEntity.class, searchBox,
				knight -> knight.isAlive()
						&& !knight.isRemoved()
						&& knight.canAbsorbSouls()
						&& knight.distanceToSqr(center) <= SOUL_SPAWN_RANGE_SQR);
	}

	private static void spawnSoulOrbs(ServerLevel level, LivingEntity victim, int logicalSoulCount) {
		RandomSource random = victim.getRandom();
		int physicalOrbCount = Math.min(Math.max(1, logicalSoulCount), UndeadKnightSoulAbsorptionData.MAX_PHYSICAL_ORBS_PER_DEATH);
		int baseValue = logicalSoulCount / physicalOrbCount;
		int remainder = logicalSoulCount % physicalOrbCount;
		for (int i = 0; i < physicalOrbCount; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0D;
			double radius = random.nextDouble() * 0.35D;
			double x = victim.getX() + Math.cos(angle) * radius;
			double y = victim.getY() + 0.15D + random.nextDouble() * 0.2D;
			double z = victim.getZ() + Math.sin(angle) * radius;
			SoulOrbEntity orb = new SoulOrbEntity(level, x, y, z);
			orb.setSoulValue(baseValue + (i < remainder ? 1 : 0));
			level.addFreshEntity(orb);
		}
	}

	private static void tryStartNearestKnightToSoulOrbs(ServerLevel level, List<UndeadKnightEntity> nearbyKnights) {
		KnightOrbCluster closestCluster = null;
		double closestAverageDistanceSqr = Double.MAX_VALUE;
		for (UndeadKnightEntity knight : nearbyKnights) {
			if (!knight.canStartSoulAbsorption()) {
				continue;
			}

			KnightOrbCluster cluster = new KnightOrbCluster(knight, getAvailableSoulOrbs(level, knight));
			if (logicalSoulCount(cluster.orbs(), MIN_ORBS_TO_START_CAST) < MIN_ORBS_TO_START_CAST) {
				continue;
			}

			double averageDistanceSqr = averageClosestThreeOrbDistanceSqr(cluster);
			if (averageDistanceSqr < closestAverageDistanceSqr) {
				closestAverageDistanceSqr = averageDistanceSqr;
				closestCluster = cluster;
			}
		}

		if (closestCluster != null) {
			closestCluster.knight().startSoulAbsorption();
		}
	}

	private static List<SoulOrbEntity> getAvailableSoulOrbs(ServerLevel level, UndeadKnightEntity knight) {
		AABB searchBox = knight.getBoundingBox().inflate(SOUL_SPAWN_RANGE);
		return level.getEntitiesOfClass(SoulOrbEntity.class, searchBox,
				orb -> orb.isAvailableForSoulAbsorption()
						&& orb.distanceToSqr(knight) <= SOUL_SPAWN_RANGE_SQR);
	}

	private static double averageClosestThreeOrbDistanceSqr(KnightOrbCluster cluster) {
		double closest = Double.MAX_VALUE;
		double secondClosest = Double.MAX_VALUE;
		double thirdClosest = Double.MAX_VALUE;
		for (SoulOrbEntity orb : cluster.orbs()) {
			double distanceSqr = orb.distanceToSqr(cluster.knight());
			int logicalUnits = Math.min(orb.getSoulValue(), MIN_ORBS_TO_START_CAST);
			for (int i = 0; i < logicalUnits; i++) {
				if (distanceSqr < closest) {
					thirdClosest = secondClosest;
					secondClosest = closest;
					closest = distanceSqr;
				} else if (distanceSqr < secondClosest) {
					thirdClosest = secondClosest;
					secondClosest = distanceSqr;
				} else if (distanceSqr < thirdClosest) {
					thirdClosest = distanceSqr;
				}
			}
		}
		return (closest + secondClosest + thirdClosest) / MIN_ORBS_TO_START_CAST;
	}

	private static int logicalSoulCount(List<SoulOrbEntity> orbs, int limit) {
		int count = 0;
		for (SoulOrbEntity orb : orbs) {
			count += Math.min(orb.getSoulValue(), limit - count);
			if (count >= limit) {
				return limit;
			}
		}
		return count;
	}

	private record KnightOrbCluster(UndeadKnightEntity knight, List<SoulOrbEntity> orbs) {
	}
}
