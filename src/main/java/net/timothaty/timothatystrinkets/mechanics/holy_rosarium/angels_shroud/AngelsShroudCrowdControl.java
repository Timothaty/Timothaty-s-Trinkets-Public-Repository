package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.angels_shroud;

import net.timothaty.timothatystrinkets.mechanics.pact.PactOfAllianceHelper;
import net.timothaty.timothatystrinkets.particle.BabahParticleOptions;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import org.joml.Vector3f;

import java.util.UUID;

public final class AngelsShroudCrowdControl {
	private static final double RADIUS_SQR = AngelsShroudData.CROWD_CONTROL_RADIUS
			* AngelsShroudData.CROWD_CONTROL_RADIUS;

	private AngelsShroudCrowdControl() {
	}

	public static void apply(ServerPlayer protectedPlayer) {
		if (!(protectedPlayer.level() instanceof ServerLevel level))
			return;

		for (LivingEntity target : level.getEntitiesOfClass(
				LivingEntity.class,
				protectedPlayer.getBoundingBox().inflate(AngelsShroudData.CROWD_CONTROL_RADIUS)
		)) {
			if (!isValidTarget(level, protectedPlayer, target)
					|| protectedPlayer.distanceToSqr(target) > RADIUS_SQR) {
				continue;
			}

			boolean affected = target instanceof Player player
					? blindPlayer(protectedPlayer, player)
					: controlMob(protectedPlayer, target);
			if (affected)
				spawnImpact(level, target);
		}
	}

	private static boolean blindPlayer(ServerPlayer source, Player target) {
		target.addEffect(new MobEffectInstance(
				MobEffects.BLINDNESS,
				AngelsShroudData.PLAYER_BLINDNESS_TICKS,
				0,
				false,
				false,
				true
		), source);
		return true;
	}

	private static boolean controlMob(ServerPlayer source, LivingEntity target) {
		if (!(target instanceof Mob mob))
			return false;
		if (!(target instanceof Enemy) && mob.getTarget() != source)
			return false;
		return TimothatysTrinketsStunHelper.tryApplyStunSilently(
				target,
				source,
				AngelsShroudData.MOB_STUN_TICKS
		);
	}

	private static boolean isValidTarget(ServerLevel level, ServerPlayer source, LivingEntity target) {
		if (target == source || !target.isAlive() || target.isDeadOrDying() || target.isRemoved()
				|| target instanceof ArmorStand || target.isSpectator()) {
			return false;
		}
		if (target instanceof Player player && player.isCreative())
			return false;
		if (source.isAlliedTo(target) || target.isAlliedTo(source)
				|| PactOfAllianceHelper.areAllied(source, target)) {
			return false;
		}
		return !isOwnedByAlly(level, source, target);
	}

	private static boolean isOwnedByAlly(ServerLevel level, ServerPlayer source, LivingEntity target) {
		if (!(target instanceof OwnableEntity ownable))
			return false;
		UUID ownerUuid = ownable.getOwnerUUID();
		if (ownerUuid == null)
			return false;
		if (ownerUuid.equals(source.getUUID()))
			return true;
		if (PactOfAllianceHelper.hasMember(source, ownerUuid))
			return true;
		Entity owner = level.getEntity(ownerUuid);
		return owner instanceof LivingEntity livingOwner
				&& (source.isAlliedTo(livingOwner)
				|| livingOwner.isAlliedTo(source)
				|| PactOfAllianceHelper.areAllied(source, livingOwner));
	}

	private static void spawnImpact(ServerLevel level, LivingEntity target) {
		BabahParticleOptions particle = new BabahParticleOptions(
				new Vector3f(
						AngelsShroudData.GOLD_RED,
						AngelsShroudData.GOLD_GREEN,
						AngelsShroudData.GOLD_BLUE
				),
				AngelsShroudData.BABAH_SCALE
		);
		level.sendParticles(
				particle,
				target.getX(),
				target.getY() + target.getBbHeight() * 0.55D,
				target.getZ(),
				1,
				0.0D,
				0.0D,
				0.0D,
				0.0D
		);
	}
}
