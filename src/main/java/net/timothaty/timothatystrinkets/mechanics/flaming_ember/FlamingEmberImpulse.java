package net.timothaty.timothatystrinkets.mechanics.flaming_ember;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.pact.PactOfAllianceHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class FlamingEmberImpulse {
	private static final int FLOOR_FLAME_RAYS = 16;
	private static final double FLOOR_FLAME_STEP = 0.45D;

	private FlamingEmberImpulse() {
	}

	public static void recordDamage(Player player, ItemStack ember, float damage) {
		if (player == null || ember == null || ember.isEmpty() || damage <= 0.0F)
			return;
		if (player.getCooldowns().isOnCooldown(ember.getItem()))
			return;

		Level level = player.level();
		if (level.isClientSide())
			return;

		CompoundTag data = player.getPersistentData();
		long now = level.getGameTime();
		long windowStart = data.getLong(FlamingEmberData.NBT_IMPULSE_DAMAGE_WINDOW_START_TICK);
		float windowDamage = data.getFloat(FlamingEmberData.NBT_IMPULSE_DAMAGE_WINDOW_AMOUNT);

		if (windowStart <= 0L || now - windowStart > FlamingEmberData.IMPULSE_DAMAGE_WINDOW_TICKS) {
			windowStart = now;
			windowDamage = 0.0F;
		}

		windowDamage += damage;
		if (windowDamage > FlamingEmberData.IMPULSE_DAMAGE_THRESHOLD) {
			clearDamageWindow(player);
			if (!trySpendChargeAndStartCooldown(player, ember))
				return;

			release(player);
			return;
		}

		data.putLong(FlamingEmberData.NBT_IMPULSE_DAMAGE_WINDOW_START_TICK, windowStart);
		data.putFloat(FlamingEmberData.NBT_IMPULSE_DAMAGE_WINDOW_AMOUNT, windowDamage);
	}

	private static boolean trySpendChargeAndStartCooldown(Player player, ItemStack ember) {
		if (player.getCooldowns().isOnCooldown(ember.getItem()))
			return false;
		if (!FlamingEmberData.consumeHeat(ember, FlamingEmberData.IMPULSE_HEAT_COST))
			return false;

		player.getCooldowns().addCooldown(ember.getItem(), FlamingEmberData.IMPULSE_COOLDOWN_TICKS);
		return true;
	}

	private static void release(Player player) {
		if (!(player.level() instanceof ServerLevel server))
			return;

		Vec3 modelCenter = new Vec3(player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ());
		Vec3 floorCenter = new Vec3(player.getX(), player.getBoundingBox().minY + 0.06D, player.getZ());

		playImpulseSound(server, player);
		spawnImpulseDecal(server, modelCenter);
		spawnFloorFlameLines(server, floorCenter);
		applyDelayedEffects(player, modelCenter);
	}

	private static void playImpulseSound(ServerLevel server, Player player) {
		server.playSound(
				null,
				player.getX(), player.getY(), player.getZ(),
				TimothatysTrinketsModSounds.EMBER_IMPULSE.get(),
				SoundSource.PLAYERS,
				1.0F,
				0.95F + player.getRandom().nextFloat() * 0.1F
		);
	}

	private static void spawnImpulseDecal(ServerLevel server, Vec3 center) {
		server.sendParticles(
				TimothatysTrinketsModParticleTypes.EMBER_IMPULSE.get(),
				center.x, center.y, center.z,
				0,
				FlamingEmberData.IMPULSE_RADIUS, 0.0D, 0.0D,
				1.0D
		);
		server.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 20, 0.45D, 0.45D, 0.45D, 0.035D);
	}

	private static void spawnFloorFlameLines(ServerLevel server, Vec3 center) {
		for (int delay = 0; delay <= FlamingEmberData.IMPULSE_SPREAD_TICKS; delay++) {
			final int scheduledDelay = delay;
			runDelayed(delay, () -> spawnFloorFlameStep(server, center, scheduledDelay));
		}
	}

	private static void spawnFloorFlameStep(ServerLevel server, Vec3 center, int stepTick) {
		double progress = Mth.clamp(stepTick / (double) FlamingEmberData.IMPULSE_SPREAD_TICKS, 0.0D, 1.0D);
		double outerDistance = FlamingEmberData.IMPULSE_RADIUS * progress;
		double innerDistance = Math.max(0.0D, outerDistance - 0.85D);

		for (int ray = 0; ray < FLOOR_FLAME_RAYS; ray++) {
			double angle = (Mth.TWO_PI * ray / FLOOR_FLAME_RAYS) + (stepTick * 0.035D);
			double dx = Math.cos(angle);
			double dz = Math.sin(angle);

			for (double distance = innerDistance; distance <= outerDistance; distance += FLOOR_FLAME_STEP) {
				double x = center.x + dx * distance;
				double z = center.z + dz * distance;
				double y = getFloorParticleY(server, x, z, center.y);
				server.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0.018D, 0.012D, 0.018D, 0.006D);
			}
		}
	}

	private static double getFloorParticleY(ServerLevel server, double x, double z, double fallbackY) {
		BlockPos base = BlockPos.containing(x, fallbackY, z);
		for (int offset = 2; offset >= -4; offset--) {
			BlockPos pos = base.offset(0, offset, 0);
			BlockState state = server.getBlockState(pos);
			if (state.getCollisionShape(server, pos).isEmpty())
				continue;

			return pos.getY() + state.getCollisionShape(server, pos).max(Direction.Axis.Y) + 0.055D;
		}

		return fallbackY;
	}

	private static void applyDelayedEffects(Player owner, Vec3 origin) {
		double radius = FlamingEmberData.IMPULSE_RADIUS;
		AABB bounds = new AABB(
				origin.x - radius,
				origin.y - FlamingEmberData.IMPULSE_VERTICAL_RANGE,
				origin.z - radius,
				origin.x + radius,
				origin.y + FlamingEmberData.IMPULSE_VERTICAL_RANGE,
				origin.z + radius
		);

		List<LivingEntity> targets = owner.level().getEntitiesOfClass(LivingEntity.class, bounds, target -> isValidTarget(owner, target));
		for (LivingEntity target : targets) {
			double distanceSqr = horizontalDistanceSqr(origin, target);
			if (distanceSqr > radius * radius)
				continue;

			int delay = Mth.clamp((int) Math.round(Math.sqrt(distanceSqr) / radius * FlamingEmberData.IMPULSE_SPREAD_TICKS), 1, FlamingEmberData.IMPULSE_SPREAD_TICKS);
			runDelayed(delay, () -> applyImpulseEffect(owner, target, origin));
		}
	}

	private static boolean isValidTarget(Player owner, LivingEntity target) {
		if (target == null || target == owner || !target.isAlive())
			return false;
		if (target instanceof Player player && (player.isCreative() || player.isSpectator()))
			return false;
		return !PactOfAllianceHelper.areAllied(owner, target);
	}

	private static void applyImpulseEffect(Player owner, LivingEntity target, Vec3 origin) {
		if (owner == null || target == null || !target.isAlive() || target.level() != owner.level())
			return;
		if (!isValidTarget(owner, target))
			return;

		Vec3 direction = new Vec3(target.getX() - origin.x, 0.0D, target.getZ() - origin.z);
		if (direction.lengthSqr() < 1.0E-4D) {
			direction = Vec3.directionFromRotation(0.0F, owner.getYRot());
		} else {
			direction = direction.normalize();
		}

		target.push(
				direction.x * FlamingEmberData.IMPULSE_KNOCKBACK_STRENGTH,
				FlamingEmberData.IMPULSE_KNOCKBACK_UPWARD,
				direction.z * FlamingEmberData.IMPULSE_KNOCKBACK_STRENGTH
		);
		target.hurtMarked = true;
		target.hasImpulse = true;
		target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), FlamingEmberData.IMPULSE_FIRE_TICKS));
		target.invulnerableTime = 0;
		target.hurt(target.damageSources().indirectMagic(owner, owner), FlamingEmberData.IMPULSE_MAGIC_DAMAGE);
	}

	private static double horizontalDistanceSqr(Vec3 origin, LivingEntity target) {
		double dx = target.getX() - origin.x;
		double dz = target.getZ() - origin.z;
		return dx * dx + dz * dz;
	}

	private static void clearDamageWindow(Player player) {
		CompoundTag data = player.getPersistentData();
		data.remove(FlamingEmberData.NBT_IMPULSE_DAMAGE_WINDOW_START_TICK);
		data.remove(FlamingEmberData.NBT_IMPULSE_DAMAGE_WINDOW_AMOUNT);
	}

	private static void runDelayed(int delayTicks, Runnable action) {
		if (delayTicks <= 0) {
			action.run();
		} else {
			TimothatysTrinketsMod.queueServerWork(delayTicks, action);
		}
	}
}
