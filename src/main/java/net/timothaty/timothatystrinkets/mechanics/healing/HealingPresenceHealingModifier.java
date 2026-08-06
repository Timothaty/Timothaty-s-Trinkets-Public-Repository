package net.timothaty.timothatystrinkets.mechanics.healing;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class HealingPresenceHealingModifier {
	private static final float BASE_BONUS = 0.35F;
	private static final float AMPLIFIER_BONUS = 0.05F;

	private HealingPresenceHealingModifier() {
	}

	public static float modifyAmount(LivingEntity target, float originalAmount, RelicHealingType type) {
		if (target == null || originalAmount <= 0.0F || !isSupported(type))
			return originalAmount;

		MobEffectInstance presence = target.getEffect(TimothatysTrinketsModMobEffects.HEALING_PRESENCE);
		if (presence == null)
			return originalAmount;

		float bonus = BASE_BONUS + presence.getAmplifier() * AMPLIFIER_BONUS;
		return originalAmount * (1.0F + bonus);
	}

	private static boolean isSupported(RelicHealingType type) {
		return type == RelicHealingType.NATURAL || type == RelicHealingType.HOLY;
	}
}
