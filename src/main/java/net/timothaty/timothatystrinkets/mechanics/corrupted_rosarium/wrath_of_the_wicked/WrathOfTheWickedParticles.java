package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.wrath_of_the_wicked;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.network.WrathOfTheWickedLaserMessage;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.network.PacketDistributor;

import org.joml.Vector3f;

public final class WrathOfTheWickedParticles {
	private static final DustParticleOptions AURA_DUST = new DustParticleOptions(
			new Vector3f(0x3C / 255.0F, 0x39 / 255.0F, 0x47 / 255.0F),
			0.78F
	);

	private static final double DOT_RED = 0xB5 / 255.0D;
	private static final double DOT_GREEN = 0x05 / 255.0D;
	private static final double DOT_BLUE = 0x18 / 255.0D;
	private static final int AURA_DUST_COUNT = 7;
	private static final int AURA_DOT_COUNT = 3;

	private WrathOfTheWickedParticles() {
	}

	public static void emitLaser(ServerPlayer player, Vec3 start, Vec3 target) {
		if (player == null
				|| start == null
				|| target == null
				|| !Double.isFinite(start.x)
				|| !Double.isFinite(start.y)
				|| !Double.isFinite(start.z)
				|| !Double.isFinite(target.x)
				|| !Double.isFinite(target.y)
				|| !Double.isFinite(target.z)) {
			return;
		}

		PacketDistributor.sendToPlayersTrackingEntityAndSelf(
				player,
				new WrathOfTheWickedLaserMessage(
						start.x,
						start.y,
						start.z,
						target.x,
						target.y,
						target.z
				)
		);
	}

	public static void emitAura(ServerPlayer player) {
		if (player == null || !player.isAlive())
			return;

		ServerLevel level = player.serverLevel();
		double centerY = player.getY() + player.getBbHeight() * 0.5D;
		double horizontalSpread = Math.max(0.22D, player.getBbWidth() * 0.72D);
		double verticalSpread = Math.max(0.32D, player.getBbHeight() * 0.45D);
		level.sendParticles(
				AURA_DUST,
				player.getX(),
				centerY,
				player.getZ(),
				AURA_DUST_COUNT,
				horizontalSpread,
				verticalSpread,
				horizontalSpread,
				0.008D
		);

		RandomSource random = player.getRandom();
		for (int index = 0; index < AURA_DOT_COUNT; index++) {
			double angle = random.nextDouble() * Math.PI * 2.0D;
			double distance = random.nextDouble() * horizontalSpread;
			double x = player.getX() + Math.cos(angle) * distance;
			double y = player.getY() + 0.12D
					+ random.nextDouble() * Math.max(0.2D, player.getBbHeight() * 0.82D);
			double z = player.getZ() + Math.sin(angle) * distance;
			level.sendParticles(
					TimothatysTrinketsModParticleTypes.DOT.get(),
					x,
					y,
					z,
					0,
					DOT_RED,
					DOT_GREEN,
					DOT_BLUE,
					1.0D
			);
		}
	}

	public static void emitFireWave(ServerPlayer player, Vec3 origin) {
		if (player == null || origin == null || !player.isAlive())
			return;

		player.serverLevel().sendParticles(
				TimothatysTrinketsModParticleTypes.EMBER_IMPULSE.get(),
				origin.x,
				origin.y + player.getBbHeight() * 0.5D,
				origin.z,
				0,
				WrathOfTheWickedData.RADIUS,
				0.0D,
				0.0D,
				1.0D
		);
	}

}
