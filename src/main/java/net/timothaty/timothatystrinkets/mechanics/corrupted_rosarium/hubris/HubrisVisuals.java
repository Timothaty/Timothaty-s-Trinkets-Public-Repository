package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.network.HubrisActivationMessage;
import net.timothaty.timothatystrinkets.network.HubrisVisualStateMessage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import net.neoforged.neoforge.network.PacketDistributor;

public final class HubrisVisuals {
	private static final int BURST_PARTICLE_COUNT = 22;

	private HubrisVisuals() {
	}

	public static void syncActivation(ServerPlayer player, HubrisActivationState.Snapshot snapshot, boolean active) {
		if (player == null || snapshot == null)
			return;
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(
				player,
				new HubrisActivationMessage(
						player.getId(),
						snapshot.sessionToken(),
						snapshot.startGameTime(),
						snapshot.mainArm().ordinal(),
						snapshot.selectedHotbarSlot(),
						snapshot.variant().ordinal(),
						snapshot.weaponSnapshot(),
						active
				)
		);
	}

	public static void syncHubrisState(
			ServerPlayer player,
			long token,
			long startGameTime,
			long endGameTime,
			int remainingThorns,
			boolean active
	) {
		if (player == null)
			return;
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(
				player,
				new HubrisVisualStateMessage(
						player.getId(),
						token,
						startGameTime,
						endGameTime,
						remainingThorns,
						active
				)
		);
	}

	public static void playActivationSound(ServerPlayer player, HubrisAnimationVariant variant) {
		player.serverLevel().playSound(
				null,
				player.getX(),
				player.getY(),
				player.getZ(),
				variant == HubrisAnimationVariant.HEAVY
						? TimothatysTrinketsModSounds.HUBRIS_ACTIVATION_MACE.get()
						: TimothatysTrinketsModSounds.HUBRIS_ACTIVATION.get(),
				SoundSource.PLAYERS,
				1.0F,
				1.0F
		);
	}

	public static void emitApplicationBurst(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		for (int index = 0; index < BURST_PARTICLE_COUNT; index++) {
			double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
			double radius = 0.18D + player.getRandom().nextDouble() * 0.38D;
			double x = player.getX() + Math.cos(angle) * radius;
			double y = player.getY() + 0.38D + player.getRandom().nextDouble() * Math.max(0.65D, player.getBbHeight() * 0.62D);
			double z = player.getZ() + Math.sin(angle) * radius;
			level.sendParticles(
					TimothatysTrinketsModParticleTypes.DOT.get(),
					x,
					y,
					z,
					0,
					HubrisData.CRIMSON_RED,
					HubrisData.CRIMSON_GREEN,
					HubrisData.CRIMSON_BLUE,
					1.0D
			);
		}
	}
}
