package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.healing_presence;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

final class HealingPresenceVfxKeepalive {
	private static final int KEEPALIVE_INTERVAL_TICKS = 8;

	private HealingPresenceVfxKeepalive() {
	}

	static void tick(Player player) {
		if (!(player.level() instanceof ServerLevel serverLevel)
				|| Math.floorMod(player.tickCount + player.getId(), KEEPALIVE_INTERVAL_TICKS) != 0)
			return;

		serverLevel.sendParticles(
				TimothatysTrinketsModParticleTypes.HEALING_PRESENCE_AURA.get(),
				player.getX(),
				player.getY(),
				player.getZ(),
				0,
				player.getId(),
				0.0D,
				0.0D,
				1.0D
		);
	}
}
