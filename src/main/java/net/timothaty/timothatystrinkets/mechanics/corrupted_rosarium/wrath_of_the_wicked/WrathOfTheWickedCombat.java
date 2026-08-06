package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.wrath_of_the_wicked;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumTargeting;
import net.timothaty.timothatystrinkets.mechanics.venom.CorrosiveToxicityHelper;
import net.timothaty.timothatystrinkets.network.WrathOfTheWickedCameraShakeMessage;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class WrathOfTheWickedCombat {
	private static final double MIN_DIRECTION_LENGTH_SQR = 1.0E-6D;
	private static final double COMPARISON_EPSILON = 1.0E-6D;

	private WrathOfTheWickedCombat() {
	}

	public static void applyPulseRing(
			ServerPlayer player,
			WrathOfTheWickedSession session,
			double previousRadius,
			double currentRadius
	) {
		if (!canAct(player)
				|| session == null
				|| session.pulseOrigin() == null
				|| currentRadius <= previousRadius) {
			return;
		}

		Vec3 origin = session.pulseOrigin();
		double pulsePlaneY = origin.y;
		AABB bounds = new AABB(
				origin.x - currentRadius,
				pulsePlaneY - WrathOfTheWickedData.PULSE_VERTICAL_TOLERANCE,
				origin.z - currentRadius,
				origin.x + currentRadius,
				pulsePlaneY + WrathOfTheWickedData.PULSE_VERTICAL_TOLERANCE,
				origin.z + currentRadius
		);
		for (LivingEntity target : player.serverLevel().getEntitiesOfClass(
				LivingEntity.class,
				bounds,
				candidate -> canAffectPulse(player, candidate)
		)) {
			if (session.pulseHits().contains(target.getUUID()))
				continue;

			double targetFeetY = target.getBoundingBox().minY;
			if (Math.abs(targetFeetY - pulsePlaneY)
					> WrathOfTheWickedData.PULSE_VERTICAL_TOLERANCE) {
				continue;
			}

			double dx = target.getX() - origin.x;
			double dz = target.getZ() - origin.z;
			double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
			if (horizontalDistance <= previousRadius || horizontalDistance > currentRadius)
				continue;

			session.pulseHits().add(target.getUUID());
			target.addEffect(
					new MobEffectInstance(
							MobEffects.MOVEMENT_SLOWDOWN,
							WrathOfTheWickedData.PULSE_SLOWNESS_TICKS,
							1,
							false,
							false,
							true
					),
					player
			);
		}
	}

	public static void applyLaserStage(
			ServerPlayer player,
			WrathOfTheWickedSession session,
			int stage
	) {
		if (!canAct(player)
				|| session == null
				|| stage < 0
				|| stage >= WrathOfTheWickedData.LASER_STAGE_COUNT
				|| !player.level().dimension().equals(session.dimension())) {
			return;
		}

		LivingEntity selectedTarget = null;
		int selectedHitCount = Integer.MAX_VALUE;
		double selectedDeviation = Double.MAX_VALUE;
		double selectedDistanceSqr = Double.MAX_VALUE;
		float sweepYaw = session.initialYaw()
				+ 360.0F * WrathOfTheWickedData.laserSweepPhase(stage);
		for (LivingEntity target : targetsInRange(player)) {
			if (!canLaserHit(player, target)) {
				continue;
			}

			int hitCount = session.laserHitCount(target);
			double deviation = angularDeviationFromSweepDirection(
					player,
					target,
					sweepYaw
			);
			double distanceSqr = player.distanceToSqr(target);
			if (isBetterLaserCandidate(
					hitCount,
					deviation,
					distanceSqr,
					selectedHitCount,
					selectedDeviation,
					selectedDistanceSqr
			)) {
				selectedTarget = target;
				selectedHitCount = hitCount;
				selectedDeviation = deviation;
				selectedDistanceSqr = distanceSqr;
			}
		}

		if (selectedTarget != null)
			applyLaserHit(player, session, selectedTarget, stage);
	}

	public static void applyFireWaveRing(
			ServerPlayer player,
			WrathOfTheWickedSession.FireWave wave,
			double previousRadius,
			double currentRadius
	) {
		if (!canAct(player)
				|| wave == null
				|| currentRadius <= previousRadius) {
			return;
		}

		Vec3 origin = wave.origin();
		AABB bounds = new AABB(
				origin.x - currentRadius,
				origin.y - WrathOfTheWickedData.RADIUS,
				origin.z - currentRadius,
				origin.x + currentRadius,
				origin.y + WrathOfTheWickedData.RADIUS,
				origin.z + currentRadius
		);
		for (LivingEntity target : player.serverLevel().getEntitiesOfClass(
				LivingEntity.class,
				bounds,
				candidate -> canAffectFireWave(player, origin, candidate)
		)) {
			if (wave.hasHit(target))
				continue;

			double dx = target.getX() - origin.x;
			double dz = target.getZ() - origin.z;
			double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
			if (horizontalDistance > currentRadius
					|| (previousRadius > 0.0D && horizontalDistance <= previousRadius)) {
				continue;
			}

			wave.markHit(target);
			target.setRemainingFireTicks(Math.max(
					target.getRemainingFireTicks(),
					WrathOfTheWickedData.FIRE_TICKS
			));
			Vec3 velocityBeforeDamage = target.getDeltaMovement();
			target.invulnerableTime = 0;
			target.hurt(
					target.damageSources().indirectMagic(player, player),
					WrathOfTheWickedData.FIRE_WAVE_DAMAGE
			);
			target.setDeltaMovement(velocityBeforeDamage);
			target.hurtMarked = true;
		}
	}

	private static void applyLaserHit(
			ServerPlayer player,
			WrathOfTheWickedSession session,
			LivingEntity target,
			int stage
	) {
		if (!canLaserHit(player, target)) {
			return;
		}

		boolean firstHit = session.laserHitCount(target) == 0;
		float damage = firstHit
				? firstLaserDamage(target)
				: WrathOfTheWickedData.REPEAT_LASER_DAMAGE;
		target.invulnerableTime = 0;
		boolean damaged = target.hurt(
				target.damageSources().indirectMagic(player, player),
				damage
		);
		if (!damaged)
			return;

		session.recordLaserHit(target);
		emitSuccessfulLaserFeedback(player, session, target, stage);
		if (!firstHit)
			return;

		TimothatysTrinketsStunHelper.tryApplyStunSilently(
				target,
				player,
				WrathOfTheWickedData.CONTROL_TICKS
		);

		if (session.venomSphereSynergy()
				&& target.getArmorValue() > 0
				&& player.getRandom().nextBoolean()) {
			CorrosiveToxicityHelper.applyLevelOneForAtLeast(
					target,
					player,
					WrathOfTheWickedData.CORROSIVE_TOXICITY_TICKS
			);
		}
	}

	private static float firstLaserDamage(LivingEntity target) {
		float missingHealth = Math.max(0.0F, target.getMaxHealth() - target.getHealth());
		float missingHealthMultiplier = target.getType().is(WrathOfTheWickedData.BOSSES)
				? 0.05F
				: 0.20F;
		return 2.0F + missingHealth * missingHealthMultiplier;
	}

	private static void emitSuccessfulLaserFeedback(
			ServerPlayer player,
			WrathOfTheWickedSession session,
			LivingEntity target,
			int stage
	) {
		Vec3 sweepDirection = Vec3.directionFromRotation(
				0.0F,
				session.initialYaw()
						+ 360.0F * WrathOfTheWickedData.laserSweepPhase(stage)
		);
		Vec3 laserOrigin = new Vec3(
				player.getX(),
				player.getY() + player.getBbHeight() * 0.74D,
				player.getZ()
		).add(sweepDirection.scale(0.12D));
		Vec3 laserTarget = target.getBoundingBox().getCenter();
		WrathOfTheWickedParticles.emitLaser(player, laserOrigin, laserTarget);
		player.serverLevel().playSound(
				null,
				laserOrigin.x,
				laserOrigin.y,
				laserOrigin.z,
				TimothatysTrinketsModSounds.ALTAR_SHOT.get(),
				SoundSource.PLAYERS,
				0.8F,
				0.8F + player.getRandom().nextFloat() * 0.6F
		);
		PacketDistributor.sendToPlayer(
				player,
				WrathOfTheWickedCameraShakeMessage.INSTANCE
		);
	}

	private static boolean canLaserHit(ServerPlayer player, LivingEntity target) {
		return canAffect(player, target)
				&& player.hasLineOfSight(target);
	}

	private static double angularDeviationFromSweepDirection(
			ServerPlayer player,
			LivingEntity target,
			float sweepYaw
	) {
		double dx = target.getX() - player.getX();
		double dz = target.getZ() - player.getZ();
		double targetYaw = dx * dx + dz * dz < MIN_DIRECTION_LENGTH_SQR
				? sweepYaw
				: Math.toDegrees(Math.atan2(-dx, dz));
		return Math.abs(Mth.wrapDegrees((float) (targetYaw - sweepYaw)));
	}

	private static boolean isBetterLaserCandidate(
			int hitCount,
			double deviation,
			double distanceSqr,
			int selectedHitCount,
			double selectedDeviation,
			double selectedDistanceSqr
	) {
		if (hitCount != selectedHitCount)
			return hitCount < selectedHitCount;
		if (deviation < selectedDeviation - COMPARISON_EPSILON)
			return true;
		return Math.abs(deviation - selectedDeviation) <= COMPARISON_EPSILON
				&& distanceSqr < selectedDistanceSqr - COMPARISON_EPSILON;
	}

	private static List<LivingEntity> targetsInRange(ServerPlayer player) {
		AABB bounds = player.getBoundingBox().inflate(WrathOfTheWickedData.RADIUS);
		return player.serverLevel().getEntitiesOfClass(
				LivingEntity.class,
				bounds,
				target -> canAffect(player, target)
		);
	}

	private static boolean canAffect(ServerPlayer player, LivingEntity target) {
		return target != null
				&& target.level() == player.level()
				&& player.distanceToSqr(target) <= WrathOfTheWickedData.RADIUS_SQR
				&& !CorruptedRosariumTargeting.isProtectedCombatTarget(player, target);
	}

	private static boolean canAffectPulse(ServerPlayer player, LivingEntity target) {
		return target != null
				&& target.level() == player.level()
				&& !CorruptedRosariumTargeting.isProtectedCombatTarget(player, target);
	}

	private static boolean canAffectFireWave(
			ServerPlayer player,
			Vec3 origin,
			LivingEntity target
	) {
		return target != null
				&& target.level() == player.level()
				&& origin.distanceToSqr(target.position()) <= WrathOfTheWickedData.RADIUS_SQR
				&& !CorruptedRosariumTargeting.isProtectedCombatTarget(player, target);
	}

	private static boolean canAct(ServerPlayer player) {
		return player != null
				&& player.isAlive()
				&& !player.isDeadOrDying()
				&& !player.isRemoved()
				&& !player.isSpectator();
	}
}
