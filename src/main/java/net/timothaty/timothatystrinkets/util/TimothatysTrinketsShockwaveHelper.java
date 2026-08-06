package net.timothaty.timothatystrinkets.util;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class TimothatysTrinketsShockwaveHelper {
	private static final double SHOCKWAVE_BELOW_FEET_TOLERANCE = 0.35D;

	private TimothatysTrinketsShockwaveHelper() {
	}

	public static void createMaceShockwave(Player attacker, LivingEntity directTarget, double radius, float baseDamage) {
		if (attacker == null || attacker.level().isClientSide() || radius <= 0.0D || baseDamage <= 0.0F)
			return;

		ShockwaveOrigin origin = getShockwaveOrigin(attacker, directTarget);
		spawnShockwaveVfx(attacker, origin, radius);

		AABB box = new AABB(
				origin.x - radius,
				origin.y - SHOCKWAVE_BELOW_FEET_TOLERANCE,
				origin.z - radius,
				origin.x + radius,
				origin.y + StrikerOfTheMorningStarData.MACE_SHOCKWAVE_VERTICAL_TOLERANCE,
				origin.z + radius
		);
		List<LivingEntity> targets = attacker.level().getEntitiesOfClass(LivingEntity.class, box, entity -> isValidShockwaveTarget(attacker, directTarget, entity));

		DamageSource source = attacker.damageSources().playerAttack(attacker);
		attacker.getPersistentData().putBoolean(StrikerOfTheMorningStarData.NBT_SHOCKWAVE_DAMAGE_GUARD, true);
		try {
			for (LivingEntity target : targets) {
				double horizontalDistanceSqr = horizontalDistanceSqr(origin.x, origin.z, target);
				double radiusSqr = radius * radius;
				if (horizontalDistanceSqr > radiusSqr)
					continue;
				if (canDodgeShockwaveByJumping(origin.y, target))
					continue;

				double horizontalDistance = Math.sqrt(horizontalDistanceSqr);
				double falloff = Math.max(0.25D, 1.0D - horizontalDistance / radius);
				float damage = (float) (baseDamage * falloff);
				if (damage <= 0.0F)
					continue;

				target.invulnerableTime = 0;
				target.hurt(source, damage);
			}
		} finally {
			attacker.getPersistentData().remove(StrikerOfTheMorningStarData.NBT_SHOCKWAVE_DAMAGE_GUARD);
		}

		attacker.level().playSound(null, attacker.blockPosition(), SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 0.65F, 0.82F);
	}

	private static void spawnShockwaveVfx(Player attacker, ShockwaveOrigin origin, double radius) {
		if (!(attacker.level() instanceof ServerLevel server))
			return;

		server.sendParticles(
				TimothatysTrinketsModParticleTypes.SHOCKWAVE.get(),
				origin.x, origin.y, origin.z,
				0,
				radius, 0.0D, 0.0D,
				1.0D
		);
	}

	private static ShockwaveOrigin getShockwaveOrigin(Player attacker, LivingEntity directTarget) {
		LivingEntity originEntity = directTarget != null ? directTarget : attacker;
		return new ShockwaveOrigin(originEntity.getX(), getFeetFloorY(originEntity), originEntity.getZ());
	}

	private static double getFeetFloorY(LivingEntity entity) {
		BlockPos onPos = entity.getOnPos();
		double blockTopY = onPos.getY() + 1.0D;
		double feetY = entity.getBoundingBox().minY;

		if (Math.abs(blockTopY - feetY) <= 1.25D) {
			return blockTopY;
		}

		return feetY;
	}

	private static boolean isValidShockwaveTarget(Player attacker, LivingEntity directTarget, LivingEntity candidate) {
		if (candidate == null || !candidate.isAlive() || candidate == attacker || candidate == directTarget)
			return false;
		if (candidate instanceof Player player && (player.isCreative() || player.isSpectator()))
			return false;
		if (candidate.hasEffect(TimothatysTrinketsModMobEffects.RESONANCE_CAGE))
			return false;
		return !candidate.hasEffect(TimothatysTrinketsModMobEffects.STUN_IMMUNITY);
	}

	private static boolean canDodgeShockwaveByJumping(double impactY, LivingEntity target) {
		if (target.onGround())
			return false;

		double feetAboveImpactFloor = target.getBoundingBox().minY - impactY;
		return feetAboveImpactFloor > 0.55D || target.getDeltaMovement().y > 0.08D;
	}

	private static double horizontalDistanceSqr(double fromX, double fromZ, LivingEntity to) {
		double dx = fromX - to.getX();
		double dz = fromZ - to.getZ();
		return dx * dx + dz * dz;
	}

	private record ShockwaveOrigin(double x, double y, double z) {
	}
}
