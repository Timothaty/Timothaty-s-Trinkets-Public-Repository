package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.healing_presence;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class HealingPresenceDotParticles {
	private static final int SPAWN_ROLL_INTERVAL_TICKS = 8;
	private static final float SPAWN_CHANCE = 0.25F;

	private static final double COLOR_RED = 1.0D;
	private static final double COLOR_GREEN = 246.0D / 255.0D;
	private static final double COLOR_BLUE = 157.0D / 255.0D;

	private HealingPresenceDotParticles() {
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Pre event) {
		Player player = event.getEntity();
		if (!(player.level() instanceof ServerLevel level)
				|| !player.isAlive()
				|| player.isDeadOrDying()
				|| player.isRemoved()
				|| player.isSpectator()
				|| !player.hasEffect(TimothatysTrinketsModMobEffects.HEALING_PRESENCE)
				|| Math.floorMod(player.tickCount + player.getId(), SPAWN_ROLL_INTERVAL_TICKS) != 0)
			return;

		RandomSource random = player.getRandom();
		if (random.nextFloat() >= SPAWN_CHANCE)
			return;

		double horizontalSpread = Math.max(0.35D, player.getBbWidth() + 0.2D);
		double x = player.getX() + (random.nextDouble() - 0.5D) * horizontalSpread;
		double y = player.getY() + player.getBbHeight() * (0.12D + random.nextDouble() * 0.72D);
		double z = player.getZ() + (random.nextDouble() - 0.5D) * horizontalSpread;
		level.sendParticles(
				TimothatysTrinketsModParticleTypes.DOT.get(),
				x,
				y,
				z,
				0,
				COLOR_RED,
				COLOR_GREEN,
				COLOR_BLUE,
				1.0D
		);
	}
}
