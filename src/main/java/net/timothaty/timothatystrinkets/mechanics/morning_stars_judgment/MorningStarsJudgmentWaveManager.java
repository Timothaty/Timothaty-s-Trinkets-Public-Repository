package net.timothaty.timothatystrinkets.mechanics.morning_stars_judgment;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.util.StrikerOfTheMorningStarData;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsDamageSources;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class MorningStarsJudgmentWaveManager {
	private static final List<ActiveWave> ACTIVE_WAVES =
			new ArrayList<>();

	private MorningStarsJudgmentWaveManager() {
	}

	public static void startWave(
			ServerLevel level,
			ServerPlayer attacker,
			LivingEntity directTarget,
			float finalDirectHitDamage
	) {
		double originX = directTarget.getX();
		double originY = directTarget.getEyeY()
				+ MorningStarsJudgmentData.VISUAL_Y_OFFSET;
		double originZ = directTarget.getZ();
		float waveDamage = finalDirectHitDamage
				* MorningStarsJudgmentData.DAMAGE_MULTIPLIER;

		level.sendParticles(
				TimothatysTrinketsModParticleTypes.SHOCKWAVE.get(),
				originX,
				originY,
				originZ,
				0,
				MorningStarsJudgmentData.MAX_RADIUS,
				0.0D,
				0.0D,
				1.0D
		);
		ACTIVE_WAVES.add(new ActiveWave(
				level.dimension(),
				attacker.getUUID(),
				directTarget.getUUID(),
				originX,
				originY,
				originZ,
				waveDamage
		));
	}

	@SubscribeEvent
	public static void onServerTickPost(ServerTickEvent.Post event) {
		if (ACTIVE_WAVES.isEmpty())
			return;

		MinecraftServer server = event.getServer();
		Iterator<ActiveWave> iterator = ACTIVE_WAVES.iterator();
		while (iterator.hasNext()) {
			if (!tickWave(server, iterator.next()))
				iterator.remove();
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		ACTIVE_WAVES.clear();
	}

	private static boolean tickWave(
			MinecraftServer server,
			ActiveWave wave
	) {
		ServerLevel level = server.getLevel(wave.dimension);
		ServerPlayer attacker = server.getPlayerList()
				.getPlayer(wave.attackerId);
		if (level == null
				|| attacker == null
				|| attacker.isRemoved()
				|| !attacker.isAlive()
				|| attacker.serverLevel() != level)
			return false;
		if (wave.age >= MorningStarsJudgmentData.LIFETIME_TICKS)
			return false;

		float previousRadius = wave.age == 0
				? 0.0F
				: MorningStarsJudgmentData.radiusAtAge(wave.age);
		int nextAge = wave.age + 1;
		float currentRadius = MorningStarsJudgmentData
				.radiusAtAge(nextAge);
		damageReachedTargets(
				level,
				attacker,
				wave,
				previousRadius,
				currentRadius
		);
		wave.age = nextAge;
		return wave.age < MorningStarsJudgmentData.LIFETIME_TICKS;
	}

	private static void damageReachedTargets(
			ServerLevel level,
			ServerPlayer attacker,
			ActiveWave wave,
			float previousRadius,
			float currentRadius
	) {
		AABB searchBounds = new AABB(
				wave.originX - currentRadius,
				wave.originY
						- MorningStarsJudgmentData.VERTICAL_SEARCH_RADIUS,
				wave.originZ - currentRadius,
				wave.originX + currentRadius,
				wave.originY
						+ MorningStarsJudgmentData.VERTICAL_SEARCH_RADIUS,
				wave.originZ + currentRadius
		);
		List<LivingEntity> candidates = level.getEntitiesOfClass(
				LivingEntity.class,
				searchBounds,
				candidate -> MorningStarsJudgmentHandler
						.isValidSecondaryTarget(attacker, null, candidate)
		);
		double previousRadiusSqr = previousRadius * previousRadius;
		double currentRadiusSqr = currentRadius * currentRadius;
		DamageSource source = TimothatysTrinketsDamageSources
				.morningStarsJudgment(level, attacker);

		attacker.getPersistentData().putBoolean(
				StrikerOfTheMorningStarData.NBT_SHOCKWAVE_DAMAGE_GUARD,
				true
		);
		try {
			for (LivingEntity candidate : candidates) {
				UUID candidateId = candidate.getUUID();
				if (candidateId.equals(wave.directTargetId)
						|| wave.hitEntityIds.contains(candidateId))
					continue;

				double distanceSqr = horizontalDistanceSqr(
						wave.originX,
						wave.originZ,
						candidate
				);
				if (distanceSqr <= previousRadiusSqr
						|| distanceSqr > currentRadiusSqr)
					continue;

				wave.hitEntityIds.add(candidateId);
				candidate.invulnerableTime = 0;
				candidate.hurt(source, wave.damage);
			}
		} finally {
			attacker.getPersistentData().remove(
					StrikerOfTheMorningStarData.NBT_SHOCKWAVE_DAMAGE_GUARD
			);
		}
	}

	private static double horizontalDistanceSqr(
			double originX,
			double originZ,
			LivingEntity target
	) {
		double deltaX = target.getX() - originX;
		double deltaZ = target.getZ() - originZ;
		return deltaX * deltaX + deltaZ * deltaZ;
	}

	private static final class ActiveWave {
		private final ResourceKey<Level> dimension;
		private final UUID attackerId;
		private final UUID directTargetId;
		private final double originX;
		private final double originY;
		private final double originZ;
		private final float damage;
		private final Set<UUID> hitEntityIds = new HashSet<>();
		private int age;

		private ActiveWave(
				ResourceKey<Level> dimension,
				UUID attackerId,
				UUID directTargetId,
				double originX,
				double originY,
				double originZ,
				float damage
		) {
			this.dimension = dimension;
			this.attackerId = attackerId;
			this.directTargetId = directTargetId;
			this.originX = originX;
			this.originY = originY;
			this.originZ = originZ;
			this.damage = damage;
		}
	}
}
