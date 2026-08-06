package net.timothaty.timothatystrinkets.client;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.mechanics.pagans_charm.PaganCharmMeditationPlayerState;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class PaganCharmMeditationParticlesClient {
	private static final float MEDITATE_SPAWN_CHANCE = 0.07F;
	private static final float LOOP_SPAWN_CHANCE = 0.11F;

	private PaganCharmMeditationParticlesClient() {
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null || minecraft.isPaused())
			return;

		for (Player player : level.players()) {
			if (!(player instanceof PaganCharmMeditationPlayerState state))
				continue;

			int phase = state.timothatys_trinkets$getPaganCharmMeditationPhase(player.tickCount);
			if (phase == PaganCharmMeditationPlayerState.PHASE_NONE)
				continue;
			if (!player.isAlive() || player.isSpectator())
				continue;

			spawnBiomeEnergy(level, player, phase);
		}
	}

	private static void spawnBiomeEnergy(ClientLevel level, Player player, int phase) {
		RandomSource random = player.getRandom();
		float chance = phase == PaganCharmMeditationPlayerState.PHASE_LOOP ? LOOP_SPAWN_CHANCE : MEDITATE_SPAWN_CHANCE;
		if (random.nextFloat() > chance)
			return;

		int color = BiomeColors.getAverageGrassColor(level, player.blockPosition());
		double red = colorComponent(color, 16);
		double green = colorComponent(color, 8);
		double blue = colorComponent(color, 0);

		double angle = random.nextDouble() * Math.PI * 2.0D;
		double radius = player.getBbWidth() * (0.48D + random.nextDouble() * 0.28D);
		double x = player.getX() + Math.cos(angle) * radius;
		double y = player.getY() + 0.12D + random.nextDouble() * Math.max(0.35D, player.getBbHeight() * 0.72D);
		double z = player.getZ() + Math.sin(angle) * radius;

		level.addParticle(TimothatysTrinketsModParticleTypes.BIOME_ENERGY.get(), x, y, z, red, green, blue);
	}

	private static double colorComponent(int color, int shift) {
		float component = ((color >> shift) & 255) / 255.0F;
		return Mth.clamp(component * 1.03F + 0.04F, 0.0F, 1.0F);
	}
}
