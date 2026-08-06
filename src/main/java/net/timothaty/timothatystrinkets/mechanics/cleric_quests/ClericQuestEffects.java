package net.timothaty.timothatystrinkets.mechanics.cleric_quests;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import org.joml.Vector3f;

public final class ClericQuestEffects {
	private static final Vector3f[] HOLY_COLORS = {
		new Vector3f(253.0F / 255.0F, 1.0F, 202.0F / 255.0F),
		new Vector3f(250.0F / 255.0F, 1.0F, 145.0F / 255.0F),
		new Vector3f(1.0F, 216.0F / 255.0F, 99.0F / 255.0F)
	};

	private ClericQuestEffects() {
	}

	public static void confirmation(ServerPlayer player) {
		burst(player.serverLevel(), player, 12, 18, 16, 24);
	}

	public static void majorCompletion(ServerPlayer player) {
		burst(player.serverLevel(), player, 20, 28, 24, 36);
	}

	public static void playStageSound(ServerPlayer player, float pitch) {
		player.serverLevel().playSound(null, player.blockPosition(), TimothatysTrinketsModSounds.SACRAMENT_STAGE_COMPLETED.get(), SoundSource.PLAYERS, 1.0F, pitch);
	}

	public static void playDeedAccomplished(ServerPlayer player) {
		player.playNotifySound(TimothatysTrinketsModSounds.DEED_ACCOMPLISHED.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
	}

	public static void playSacramentMobSlayed(ServerPlayer player) {
		player.playNotifySound(TimothatysTrinketsModSounds.SACRAMENT_MOB_SLAYED.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
	}

	private static void burst(ServerLevel level, ServerPlayer player, int minDots, int maxDots, int minDust, int maxDust) {
		RandomSource random = level.getRandom();
		int dots = minDots + random.nextInt(maxDots - minDots + 1);
		int dust = minDust + random.nextInt(maxDust - minDust + 1);
		for (int index = 0; index < dots; index++) {
			Vector3f color = HOLY_COLORS[random.nextInt(HOLY_COLORS.length)];
			double x = player.getX() + (random.nextDouble() - 0.5D) * Math.max(0.55D, player.getBbWidth() * 1.4D);
			double y = player.getY() + 0.15D + random.nextDouble() * player.getBbHeight();
			double z = player.getZ() + (random.nextDouble() - 0.5D) * Math.max(0.55D, player.getBbWidth() * 1.4D);
			level.sendParticles(TimothatysTrinketsModParticleTypes.DOT.get(), x, y, z, 0, color.x(), color.y(), color.z(), 1.0D);
		}
		for (int index = 0; index < dust; index++) {
			Vector3f color = HOLY_COLORS[random.nextInt(HOLY_COLORS.length)];
			level.sendParticles(
				new DustParticleOptions(color, 0.8F + random.nextFloat() * 0.45F),
				player.getX(),
				player.getY() + player.getBbHeight() * 0.5D,
				player.getZ(),
				1,
				0.45D,
				player.getBbHeight() * 0.45D,
				0.45D,
				0.015D
			);
		}
	}
}
