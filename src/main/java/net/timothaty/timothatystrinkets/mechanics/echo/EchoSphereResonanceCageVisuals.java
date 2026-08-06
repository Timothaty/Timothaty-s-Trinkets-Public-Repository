package net.timothaty.timothatystrinkets.mechanics.echo;


import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import org.joml.Vector3f;

public class EchoSphereResonanceCageVisuals {
	private static final double TWO_PI = Math.PI * 2.0D;
	private static final DustParticleOptions ECHO_DUST = new DustParticleOptions(new Vector3f(0.10F, 0.86F, 1.00F), 1.12F);

	private EchoSphereResonanceCageVisuals() {
	}

	public static void spawnEnter(ServerLevel level, LivingEntity target, int durationTicks, boolean deepDark) {
		BlockPos pos = target.blockPosition();
		level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.42F, deepDark ? 0.55F : 0.78F);
		level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.65F, deepDark ? 0.65F : 0.92F);

		spawnCaps(level, target, durationTicks + 18);
		spawnColumn(level, target, deepDark ? 34 : 22, 0.12D);
	}

	public static void spawnLoop(ServerLevel level, LivingEntity target, boolean deepDark) {
		spawnColumn(level, target, deepDark ? 11 : 5, 0.015D);
	}

	public static void spawnRelease(ServerLevel level, LivingEntity target, boolean deepDark) {
		BlockPos pos = target.blockPosition();
		level.playSound(null, pos, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, deepDark ? 0.72F : 0.58F, deepDark ? 0.62F : 1.08F);
		level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.65F, deepDark ? 0.72F : 1.2F);

		spawnColumn(level, target, deepDark ? 54 : 30, 0.09D);
	}

	public static void spawnDeathSouls(ServerLevel level, LivingEntity target, boolean deepDark) {
		level.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), deepDark ? 46 : 30, target.getBbWidth() * 0.72D, target.getBbHeight() * 0.5D, target.getBbWidth() * 0.72D, 0.085D);
		level.playSound(null, target.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, deepDark ? 0.8F : 0.55F, deepDark ? 0.65F : 1.0F);
	}

	private static void spawnCaps(ServerLevel level, LivingEntity target, int lifetimeTicks) {
		double radius = Math.max(0.82D, target.getBbWidth() * 0.82D + 0.42D);
		double x = target.getX();
		double z = target.getZ();
		double minY = target.getY() + 0.035D;
		double maxY = target.getY() + target.getBbHeight() - 0.035D;
		double yaw = (target.tickCount * 0.031D) % TWO_PI;

		level.sendParticles(TimothatysTrinketsModParticleTypes.RESONANCE_BOTTOM_TOP.get(), x, minY, z, 0, radius, lifetimeTicks, yaw, 1.0D);
		level.sendParticles(TimothatysTrinketsModParticleTypes.RESONANCE_BOTTOM_TOP.get(), x, maxY, z, 0, radius, lifetimeTicks, yaw + Math.PI, 1.0D);
	}

	private static void spawnColumn(ServerLevel level, LivingEntity target, int count, double speed) {
		double height = target.getBbHeight();
		double radius = Math.max(0.22D, target.getBbWidth() * 0.42D);
		double age = target.tickCount * 0.16D;

		for (int i = 0; i < count; i++) {
			double progress = ((target.tickCount * 0.035D) + (double) i / Math.max(1, count)) % 1.0D;
			double y = target.getY() + Mth.clamp((float) progress, 0.04F, 0.96F) * height;
			double angle = age + i * 2.399963229728653D;
			double x = target.getX() + Math.cos(angle) * radius;
			double z = target.getZ() + Math.sin(angle) * radius;

			level.sendParticles(ECHO_DUST, x, y, z, 1, 0.025D, 0.025D, 0.025D, speed);
		}
	}
}
