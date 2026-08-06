package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.confession;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestCeremonyService;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

final class ConfessionHealingEffects {
	private static final double HOLY_R = 1.0D;
	private static final double HOLY_G = 212.0D / 255.0D;
	private static final double HOLY_B = 45.0D / 255.0D;

	private ConfessionHealingEffects() {
	}

	static void startBlessing(ServerLevel level, Villager cleric, Player target) {
		ClericQuestCeremonyService.playStandalone(level, cleric, target);
	}

	static void finishHealing(ServerLevel level, Player target) {
		for (int index = 0; index < 24; index++) {
			double x = target.getX() + (target.getRandom().nextDouble() - 0.5D) * Math.max(0.35D, target.getBbWidth());
			double y = target.getY() + target.getBbHeight() * (0.18D + target.getRandom().nextDouble() * 0.68D);
			double z = target.getZ() + (target.getRandom().nextDouble() - 0.5D) * Math.max(0.35D, target.getBbWidth());
			level.sendParticles(TimothatysTrinketsModParticleTypes.DOT.get(), x, y, z, 0, HOLY_R, HOLY_G, HOLY_B, 1.0D);
		}
	}
}
