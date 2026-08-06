package net.timothaty.timothatystrinkets.util;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public final class StrikerOfTheMorningStarEffects {
	private StrikerOfTheMorningStarEffects() {
	}

	public static void applyStunFatigue(Player player) {
		if (player == null || player.level().isClientSide())
			return;
		if (!player.getMainHandItem().is(TimothatysTrinketsStunTags.HEAVY_ARMS))
			return;

		player.addEffect(new MobEffectInstance(
				MobEffects.DIG_SLOWDOWN,
				StrikerOfTheMorningStarData.STUN_FATIGUE_TICKS,
				StrikerOfTheMorningStarData.STUN_FATIGUE_AMPLIFIER,
				false,
				true,
				true
		));
	}
}
