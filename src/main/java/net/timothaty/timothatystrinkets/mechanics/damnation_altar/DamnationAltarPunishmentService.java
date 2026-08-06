package net.timothaty.timothatystrinkets.mechanics.damnation_altar;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class DamnationAltarPunishmentService {
	private static final int LOW_RELATION_COOLDOWN_TICKS = 20;
	private static final int LOW_RELATION_STRIKE_DELAY_TICKS = 12;
	private static final int SACRIFICE_STRIKE_DELAY_TICKS = 8;
	private static final int LASER_POINTS_PER_BLOCK = 5;
	private static final int SACRIFICE_TARGET_RADIUS = 7;
	private static final DustParticleOptions LASER_DUST = new DustParticleOptions(
			new Vector3f(0xC8 / 255.0F, 0xFF / 255.0F, 0x6F / 255.0F),
			1.15F
	);
	private static final Map<UUID, Long> LOW_RELATION_COOLDOWNS = new HashMap<>();

	private DamnationAltarPunishmentService() {
	}

	public static void punishLowRelation(ServerLevel level, ServerPlayer player, BlockPos altarPos) {
		long gameTime = level.getGameTime();
		long readyAt = LOW_RELATION_COOLDOWNS.getOrDefault(player.getUUID(), Long.MIN_VALUE);
		if (gameTime < readyAt) return;
		LOW_RELATION_COOLDOWNS.put(player.getUUID(), gameTime + LOW_RELATION_COOLDOWN_TICKS);

		int strikes = level.getRandom().nextBoolean() ? 1 : 3;
		for (int i = 0; i < strikes; i++) {
			int delay = 1 + i * LOW_RELATION_STRIKE_DELAY_TICKS;
			TimothatysTrinketsMod.queueServerWork(delay, () -> strikePlayer(level, player, altarPos));
		}
	}

	public static void scheduleSacrificePunishment(ServerLevel level, ServerPlayer sourcePlayer, BlockPos altarPos, LivingEntity sacrificed, int strikes) {
		for (int i = 0; i < strikes; i++) {
			int delay = 1 + i * SACRIFICE_STRIKE_DELAY_TICKS;
			TimothatysTrinketsMod.queueServerWork(delay, () -> strikeSacrificeTarget(level, sourcePlayer, altarPos, sacrificed));
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		LOW_RELATION_COOLDOWNS.clear();
	}

	private static void strikePlayer(ServerLevel level, ServerPlayer player, BlockPos altarPos) {
		if (!player.isAlive() || player.level() != level) return;
		spawnLaser(level, altarPos, player);
		playShot(level, altarPos);
		player.hurt(player.damageSources().magic(), 1.0F);
	}

	private static void strikeSacrificeTarget(ServerLevel level, ServerPlayer sourcePlayer, BlockPos altarPos, LivingEntity sacrificed) {
		LivingEntity target = pickSacrificeTarget(level, altarPos, sacrificed);
		if (target == null) return;
		spawnLaser(level, altarPos, target);
		playShot(level, altarPos);
		target.hurt(target.damageSources().indirectMagic(sourcePlayer, sourcePlayer), 4.0F + level.getRandom().nextInt(3));
	}

	private static LivingEntity pickSacrificeTarget(ServerLevel level, BlockPos altarPos, LivingEntity sacrificed) {
		Vec3 center = Vec3.atCenterOf(altarPos);
		AABB area = new AABB(altarPos).inflate(SACRIFICE_TARGET_RADIUS);
		double radiusSqr = SACRIFICE_TARGET_RADIUS * SACRIFICE_TARGET_RADIUS;
		List<LivingEntity> candidates = level.getEntitiesOfClass(
				LivingEntity.class,
				area,
				entity -> entity.isAlive()
						&& entity != sacrificed
						&& !entity.isSpectator()
						&& entity.distanceToSqr(center) <= radiusSqr
		);
		return candidates.isEmpty() ? null : candidates.get(level.getRandom().nextInt(candidates.size()));
	}

	private static void spawnLaser(ServerLevel level, BlockPos altarPos, LivingEntity target) {
		Vec3 start = Vec3.atCenterOf(altarPos);
		Vec3 end = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
		int points = Math.max(2, Mth.ceil(start.distanceTo(end) * LASER_POINTS_PER_BLOCK));
		for (int i = 0; i <= points; i++) {
			float t = (float) i / points;
			level.sendParticles(
					LASER_DUST,
					Mth.lerp(t, start.x, end.x),
					Mth.lerp(t, start.y, end.y),
					Mth.lerp(t, start.z, end.z),
					1, 0.0D, 0.0D, 0.0D, 0.0D
			);
		}
	}

	private static void playShot(ServerLevel level, BlockPos altarPos) {
		float pitch = 0.9F + level.getRandom().nextFloat() * 0.6F;
		level.playSound(null, altarPos, TimothatysTrinketsModSounds.ALTAR_SHOT.get(), SoundSource.BLOCKS, 1.0F, pitch);
	}
}
