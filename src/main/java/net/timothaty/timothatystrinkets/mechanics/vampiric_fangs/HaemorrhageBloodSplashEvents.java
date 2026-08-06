package net.timothaty.timothatystrinkets.mechanics.vampiric_fangs;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.Optional;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class HaemorrhageBloodSplashEvents {
	private HaemorrhageBloodSplashEvents() {}

	private static final int PLAYER_HIT_BLOOD_COUNT = 26;
	private static final int OTHER_HIT_BLOOD_COUNT = 18;

	@SubscribeEvent
	public static void onLivingDamagePost(LivingDamageEvent.Post event) {
		LivingEntity victim = event.getEntity();
		if (victim == null || victim.level().isClientSide())
			return;
		if (victim instanceof ArmorStand)
			return;
		if (event.getNewDamage() <= 0.0F)
			return;
		if (!victim.hasEffect(TimothatysTrinketsModMobEffects.HAEMORRHAGE))
			return;
		if (!(victim.level() instanceof ServerLevel server))
			return;

		Entity attacker = event.getSource().getEntity();
		if (attacker instanceof Player player) {
			spawnBloodSplash(server, getPlayerCrosshairHitPoint(player, victim), PLAYER_HIT_BLOOD_COUNT, 0.34D);
		} else {
			spawnBloodSplash(server, getRandomBodyPoint(victim), OTHER_HIT_BLOOD_COUNT, 0.27D);
		}
	}

	public static void spawnInsatiableBiteSplash(ServerLevel server, LivingEntity target, Entity attacker) {
		Vec3 position = attacker instanceof Player player ? getPlayerCrosshairHitPoint(player, target) : getRandomBodyPoint(target);
		spawnBloodSplash(server, position, 48, 0.42D);
	}

	private static Vec3 getPlayerCrosshairHitPoint(Player player, LivingEntity victim) {
		Vec3 eye = player.getEyePosition(1.0F);
		double reach = Math.max(3.0D, player.distanceTo(victim) + victim.getBbWidth() + 1.5D);
		Vec3 end = eye.add(player.getViewVector(1.0F).scale(reach));
		Optional<Vec3> hit = victim.getBoundingBox().inflate(victim.getPickRadius() + 0.35D).clip(eye, end);
		return hit.orElseGet(() -> getRandomBodyPoint(victim));
	}

	private static Vec3 getRandomBodyPoint(LivingEntity victim) {
		RandomSource random = victim.getRandom();
		double horizontalSpread = Math.max(0.18D, victim.getBbWidth() * 0.38D);
		double x = victim.getX() + (random.nextDouble() - 0.5D) * 2.0D * horizontalSpread;
		double y = victim.getY() + victim.getBbHeight() * (0.25D + random.nextDouble() * 0.55D);
		double z = victim.getZ() + (random.nextDouble() - 0.5D) * 2.0D * horizontalSpread;
		return new Vec3(x, y, z);
	}

	private static void spawnBloodSplash(ServerLevel server, Vec3 position, int count, double speed) {
		SimpleParticleType particle = TimothatysTrinketsModParticleTypes.BLOOD_BIT.get();
		RandomSource random = server.random;
		for (int i = 0; i < count; i++) {
			Vec3 direction = randomDirection(random);
			double particleSpeed = speed * (0.55D + random.nextDouble() * 0.85D);
			double x = position.x + (random.nextDouble() - 0.5D) * 0.16D;
			double y = position.y + (random.nextDouble() - 0.5D) * 0.12D;
			double z = position.z + (random.nextDouble() - 0.5D) * 0.16D;
			server.sendParticles(particle,
					x, y, z,
					0,
					direction.x * particleSpeed,
					direction.y * particleSpeed,
					direction.z * particleSpeed,
					1.0D);
		}

		server.sendParticles(particle,
				position.x, position.y + 0.03D, position.z,
				Math.max(6, count / 4),
				0.28D, 0.18D, 0.28D,
				0.06D);
	}

	private static Vec3 randomDirection(RandomSource random) {
		double x = random.nextDouble() * 2.0D - 1.0D;
		double y = random.nextDouble() * 1.45D - 0.25D;
		double z = random.nextDouble() * 2.0D - 1.0D;
		Vec3 direction = new Vec3(x, y, z);
		if (direction.lengthSqr() < 0.001D)
			return new Vec3(0.0D, 1.0D, 0.0D);
		return direction.normalize();
	}
}
