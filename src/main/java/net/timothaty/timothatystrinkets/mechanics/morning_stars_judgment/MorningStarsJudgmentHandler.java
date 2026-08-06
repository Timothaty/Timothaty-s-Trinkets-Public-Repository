package net.timothaty.timothatystrinkets.mechanics.morning_stars_judgment;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.util.StrikerOfTheMorningStarCurios;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class MorningStarsJudgmentHandler {
	private static final double MIN_DIRECTION_LENGTH_SQR = 0.0001D;
	private static final double FALLBACK_ANGLE_STEP = 2.399963229728653D;

	private MorningStarsJudgmentHandler() {
	}

	public static void tryActivate(
			ServerPlayer attacker,
			LivingEntity target,
			boolean fullyCharged,
			boolean headStrike,
			float finalDirectHitDamage
	) {
		if (attacker == null
				|| target == null
				|| !fullyCharged
				|| !headStrike
				|| finalDirectHitDamage <= 0.0F)
			return;
		if (!(attacker.level() instanceof ServerLevel serverLevel)
				|| target.level() != serverLevel
				|| attacker.isRemoved()
				|| !attacker.isAlive())
			return;
		if (!attacker.getMainHandItem().is(
				TimothatysTrinketsModItems.MORGENSHTERN.get()
		))
			return;
		if (!StrikerOfTheMorningStarCurios.isStrikerEquipped(attacker))
			return;

		Item striker = TimothatysTrinketsModItems
				.STRIKER_OF_THE_MORNING_STAR.get();
		if (attacker.getCooldowns().isOnCooldown(striker))
			return;

		MorningStarsJudgmentWaveManager.startWave(
				serverLevel,
				attacker,
				target,
				finalDirectHitDamage
		);
		attacker.getCooldowns().addCooldown(
				striker,
				MorningStarsJudgmentData.COOLDOWN_TICKS
		);
		applyImmediateKnockback(serverLevel, attacker, target);
	}

	private static void applyImmediateKnockback(
			ServerLevel level,
			ServerPlayer attacker,
			LivingEntity directTarget
	) {
		double originX = directTarget.getX();
		double originY = directTarget.getEyeY();
		double originZ = directTarget.getZ();
		double radius = MorningStarsJudgmentData.KNOCKBACK_RADIUS;
		AABB searchBounds = new AABB(
				originX - radius,
				originY - MorningStarsJudgmentData.VERTICAL_SEARCH_RADIUS,
				originZ - radius,
				originX + radius,
				originY + MorningStarsJudgmentData.VERTICAL_SEARCH_RADIUS,
				originZ + radius
		);
		List<LivingEntity> nearby = level.getEntitiesOfClass(
				LivingEntity.class,
				searchBounds,
				candidate -> isValidSecondaryTarget(
						attacker,
						directTarget,
						candidate
				)
		);
		double radiusSqr = radius * radius;
		for (LivingEntity candidate : nearby) {
			double directionX = candidate.getX() - originX;
			double directionZ = candidate.getZ() - originZ;
			double lengthSqr = directionX * directionX
					+ directionZ * directionZ;
			if (lengthSqr > radiusSqr)
				continue;
			if (lengthSqr < MIN_DIRECTION_LENGTH_SQR) {
				double angle = candidate.getId() * FALLBACK_ANGLE_STEP;
				directionX = Math.cos(angle);
				directionZ = Math.sin(angle);
				lengthSqr = 1.0D;
			}

			double inverseLength = 1.0D / Math.sqrt(lengthSqr);
			candidate.push(
					directionX * inverseLength
							* MorningStarsJudgmentData
									.KNOCKBACK_HORIZONTAL_STRENGTH,
					MorningStarsJudgmentData.KNOCKBACK_VERTICAL_IMPULSE,
					directionZ * inverseLength
							* MorningStarsJudgmentData
									.KNOCKBACK_HORIZONTAL_STRENGTH
			);
			candidate.hurtMarked = true;
		}
	}

	static boolean isValidSecondaryTarget(
			ServerPlayer attacker,
			LivingEntity directTarget,
			LivingEntity candidate
	) {
		if (candidate == null
				|| candidate == attacker
				|| candidate == directTarget
				|| candidate.isRemoved()
				|| !candidate.isAlive())
			return false;
		return !(candidate instanceof Player player)
				|| (!player.isCreative() && !player.isSpectator());
	}
}
