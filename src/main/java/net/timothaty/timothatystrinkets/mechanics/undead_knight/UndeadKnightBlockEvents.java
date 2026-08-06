package net.timothaty.timothatystrinkets.mechanics.undead_knight;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.entity.UndeadKnightEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris.HubrisStrikeResolver;
import net.timothaty.timothatystrinkets.network.UndeadKnightParryCameraShakeMessage;
import net.timothaty.timothatystrinkets.util.StrikerOfTheMorningStarCurios;
import net.timothaty.timothatystrinkets.util.StrikerOfTheMorningStarData;
import net.timothaty.timothatystrinkets.util.StrikerOfTheMorningStarEffects;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunTags;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class UndeadKnightBlockEvents {
	private static final float BLOCK_CHANCE = 0.18F;
	private static final double BLOCK_CONE_DEGREES = 180.0D;
	private static final double BLOCK_CONE_DOT = Math.cos(Math.toRadians(BLOCK_CONE_DEGREES * 0.5D));
	private static final int PARRY_SPARK_MIN_COUNT = 12;
	private static final int PARRY_SPARK_RANDOM_COUNT = 6;

	private UndeadKnightBlockEvents() {
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
		if (!(event.getEntity() instanceof UndeadKnightEntity knight) || knight.level().isClientSide())
			return;
		if (TimothatysTrinketsStunHelper.isStunned(knight) || TimothatysTrinketsStunHelper.isStaggered(knight))
			return;

		DamageSource source = event.getSource();
		if (!isBlockableDamage(source) || !isInsideBlockCone(knight, source))
			return;

		if (knight.isBlocking()) {
			event.setCanceled(true);
			handleSuccessfulBlock(knight, source);
			return;
		}
		if (knight.getRandom().nextFloat() >= BLOCK_CHANCE)
			return;

		if (knight.startBlock()) {
			event.setCanceled(true);
			handleSuccessfulBlock(knight, source);
		}
	}

	private static void handleSuccessfulBlock(UndeadKnightEntity knight, DamageSource source) {
		HubrisStrikeResolver.markDefended(
				knight,
				source,
				HubrisStrikeResolver.DefenseKind.UNDEAD_KNIGHT_BLOCK
		);
		spawnParrySparks(knight, source);
		sendParryCameraShake(source);
		handleStrikerBlockedMelee(knight, source);
	}

	private static boolean isBlockableDamage(DamageSource source) {
		return source != null && (isDirectMeleeDamage(source) || isArrowDamage(source));
	}

	private static boolean isDirectMeleeDamage(DamageSource source) {
		if (!source.is(DamageTypes.PLAYER_ATTACK) && !source.is(DamageTypes.MOB_ATTACK) && !source.is(DamageTypes.MOB_ATTACK_NO_AGGRO))
			return false;

		Entity attacker = source.getEntity();
		return attacker instanceof LivingEntity && source.getDirectEntity() == attacker;
	}

	private static boolean isArrowDamage(DamageSource source) {
		return source.is(DamageTypes.ARROW) && source.getDirectEntity() instanceof AbstractArrow;
	}

	private static void sendParryCameraShake(DamageSource source) {
		if (source.getEntity() instanceof ServerPlayer player) {
			PacketDistributor.sendToPlayer(player, UndeadKnightParryCameraShakeMessage.INSTANCE);
		}
	}

	private static void spawnParrySparks(UndeadKnightEntity knight, DamageSource source) {
		if (!(knight.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		RandomSource random = knight.getRandom();
		SimpleParticleType particle = TimothatysTrinketsModParticleTypes.SPARK.get();
		Vec3 impactPoint = getImpactPoint(knight, source);
		Vec3 burstDirection = getBurstDirection(knight, source, impactPoint);
		Vec3 origin = impactPoint.add(burstDirection.scale(0.045D));
		int count = PARRY_SPARK_MIN_COUNT + random.nextInt(PARRY_SPARK_RANDOM_COUNT + 1);
		for (int i = 0; i < count; i++) {
			Vec3 direction = randomSparkDirection(burstDirection, random);
			double speed = 0.18D + random.nextDouble() * 0.22D;
			double x = origin.x + (random.nextDouble() - 0.5D) * 0.075D;
			double y = origin.y + (random.nextDouble() - 0.5D) * 0.075D;
			double z = origin.z + (random.nextDouble() - 0.5D) * 0.075D;
			serverLevel.sendParticles(particle, x, y, z, 0, direction.x * speed, direction.y * speed, direction.z * speed, 1.0D);
		}
	}

	private static Vec3 getImpactPoint(UndeadKnightEntity knight, DamageSource source) {
		if (source.getEntity() instanceof Player player) {
			return getCrosshairHitPoint(player, knight);
		}
		if (source.getEntity() instanceof LivingEntity attacker) {
			return getCrosshairHitPoint(attacker, knight);
		}
		if (source.getDirectEntity() instanceof AbstractArrow arrow) {
			Vec3 motion = arrow.getDeltaMovement();
			if (motion.lengthSqr() > 1.0E-5D) {
				Vec3 end = arrow.position();
				Vec3 start = end.subtract(motion.normalize().scale(2.0D));
				return knight.getBoundingBox().inflate(knight.getPickRadius() + 0.35D).clip(start, end).orElse(end);
			}
			return arrow.position();
		}
		Vec3 sourcePosition = source.getSourcePosition();
		return sourcePosition != null ? sourcePosition : getRandomBodyPoint(knight);
	}

	private static Vec3 getCrosshairHitPoint(LivingEntity attacker, UndeadKnightEntity knight) {
		Vec3 eye = attacker.getEyePosition(1.0F);
		double reach = Math.max(3.0D, attacker.distanceTo(knight) + knight.getBbWidth() + 1.5D);
		Vec3 end = eye.add(attacker.getViewVector(1.0F).scale(reach));
		return knight.getBoundingBox().inflate(knight.getPickRadius() + 0.35D).clip(eye, end).orElseGet(() -> getRandomBodyPoint(knight));
	}

	private static Vec3 getRandomBodyPoint(UndeadKnightEntity knight) {
		RandomSource random = knight.getRandom();
		double horizontalSpread = Math.max(0.18D, knight.getBbWidth() * 0.38D);
		double x = knight.getX() + (random.nextDouble() - 0.5D) * 2.0D * horizontalSpread;
		double y = knight.getY() + knight.getBbHeight() * (0.32D + random.nextDouble() * 0.42D);
		double z = knight.getZ() + (random.nextDouble() - 0.5D) * 2.0D * horizontalSpread;
		return new Vec3(x, y, z);
	}

	private static Vec3 getBurstDirection(UndeadKnightEntity knight, DamageSource source, Vec3 impactPoint) {
		if (source.getDirectEntity() instanceof AbstractArrow arrow) {
			Vec3 arrowMotion = arrow.getDeltaMovement();
			if (arrowMotion.lengthSqr() > 1.0E-5D) {
				return arrowMotion.scale(-1.0D).normalize();
			}
		}

		Entity attacker = source.getEntity();
		Vec3 sourcePosition = attacker instanceof LivingEntity livingAttacker ? livingAttacker.getEyePosition(1.0F) : source.getSourcePosition();
		if (sourcePosition != null) {
			Vec3 toSource = sourcePosition.subtract(impactPoint);
			if (toSource.lengthSqr() > 1.0E-5D) {
				return toSource.normalize();
			}
		}

		Vec3 horizontalSource = getHorizontalSourceDirection(knight, source);
		if (horizontalSource != null && horizontalSource.lengthSqr() > 1.0E-5D) {
			return horizontalSource.normalize();
		}
		return knight.getLookAngle().scale(-1.0D).normalize();
	}

	private static Vec3 randomSparkDirection(Vec3 baseDirection, RandomSource random) {
		Vec3 base = baseDirection.lengthSqr() > 1.0E-5D ? baseDirection.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
		Vec3 side = new Vec3(-base.z, 0.0D, base.x);
		if (side.lengthSqr() < 1.0E-5D) {
			side = new Vec3(1.0D, 0.0D, 0.0D);
		}
		side = side.normalize();
		Vec3 direction = base.scale(0.78D + random.nextDouble() * 0.55D)
				.add(side.scale((random.nextDouble() - 0.5D) * 1.25D))
				.add(0.0D, 0.18D + random.nextDouble() * 0.75D, 0.0D);
		if (direction.lengthSqr() < 1.0E-5D) {
			return new Vec3(0.0D, 1.0D, 0.0D);
		}
		return direction.normalize();
	}

	private static void handleStrikerBlockedMelee(UndeadKnightEntity knight, DamageSource source) {
		if (!isDirectMeleeDamage(source))
			return;

		if (!(source.getEntity() instanceof Player attacker))
			return;
		if (!StrikerOfTheMorningStarCurios.isStrikerEquipped(attacker))
			return;

		ItemStack weapon = attacker.getMainHandItem();
		if (!weapon.is(TimothatysTrinketsStunTags.HEAVY_ARMS))
			return;

		int stunTicks = weapon.is(ItemTags.AXES)
				? StrikerOfTheMorningStarData.UNDEAD_KNIGHT_AXE_BLOCK_STUN_TICKS
				: StrikerOfTheMorningStarData.UNDEAD_KNIGHT_BLOCK_STUN_TICKS;
		boolean applied = TimothatysTrinketsStunHelper.tryApplyStunSilently(knight, attacker, stunTicks, stunTicks);
		if (applied) {
			StrikerOfTheMorningStarEffects.applyStunFatigue(attacker);
		}
	}

	private static boolean isInsideBlockCone(UndeadKnightEntity knight, DamageSource source) {
		Vec3 toSource = getHorizontalSourceDirection(knight, source);
		if (toSource == null)
			return false;

		Vec3 bodyForward = Vec3.directionFromRotation(0.0F, knight.yBodyRot);
		Vec3 flatBodyForward = new Vec3(bodyForward.x, 0.0D, bodyForward.z);
		if (flatBodyForward.lengthSqr() < 1.0E-6D)
			return false;

		return flatBodyForward.normalize().dot(toSource.normalize()) >= BLOCK_CONE_DOT - 1.0E-6D;
	}

	private static Vec3 getHorizontalSourceDirection(UndeadKnightEntity knight, DamageSource source) {
		Entity directEntity = source.getDirectEntity();
		Entity attacker = source.getEntity();
		Entity origin = directEntity != null ? directEntity : attacker;
		if (origin != null) {
			Vec3 toOrigin = origin.position().subtract(knight.position());
			Vec3 horizontal = new Vec3(toOrigin.x, 0.0D, toOrigin.z);
			if (horizontal.lengthSqr() >= 1.0E-4D) {
				return horizontal;
			}
		}

		if (directEntity instanceof AbstractArrow arrow) {
			Vec3 arrowMotion = arrow.getDeltaMovement();
			Vec3 incomingFrom = new Vec3(-arrowMotion.x, 0.0D, -arrowMotion.z);
			if (incomingFrom.lengthSqr() >= 1.0E-4D) {
				return incomingFrom;
			}
		}
		return null;
	}
}
