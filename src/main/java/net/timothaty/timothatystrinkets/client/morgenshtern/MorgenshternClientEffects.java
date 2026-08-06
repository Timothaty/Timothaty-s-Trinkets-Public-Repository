package net.timothaty.timothatystrinkets.client.morgenshtern;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class MorgenshternClientEffects {
	private MorgenshternClientEffects() {
	}

	public static void onStrike(int attackerEntityId, int targetEntityId) {
		MorgenshternStrikeClientState.start(attackerEntityId);
		spawnMorgenshternSweep(
				attackerEntityId,
				targetEntityId
		);

		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player != null && player.getId() == attackerEntityId) {
			MorgenshternCameraShakeHandler.start(
					attackerEntityId,
					targetEntityId
			);
		}
	}

	private static void spawnMorgenshternSweep(
			int attackerEntityId,
			int targetEntityId
	) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null)
			return;

		Entity attacker = level.getEntity(attackerEntityId);
		Entity targetEntity = level.getEntity(targetEntityId);
		if (!(targetEntity instanceof LivingEntity target))
			return;

		double x = target.getX();
		double z = target.getZ();
		if (attacker != null) {
			double directionX = attacker.getX() - x;
			double directionZ = attacker.getZ() - z;
			double horizontalLength = Math.sqrt(
					directionX * directionX
							+ directionZ * directionZ
			);
			if (horizontalLength > 0.0001D) {
				double offset = Math.max(
						0.16D,
						target.getBbWidth() * 0.52D
				);
				x += directionX / horizontalLength * offset;
				z += directionZ / horizontalLength * offset;
			}
		}

		double size = Mth.clamp(
				0.74D + target.getBbWidth() * 0.34D,
				0.78D,
				1.30D
		);
		level.addParticle(
				TimothatysTrinketsModParticleTypes.MORGENSHTERN_SWEEP.get(),
				x,
				target.getEyeY(),
				z,
				size,
				0.0D,
				0.0D
		);
	}
}
