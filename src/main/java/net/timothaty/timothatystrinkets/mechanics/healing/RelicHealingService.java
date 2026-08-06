package net.timothaty.timothatystrinkets.mechanics.healing;

import net.minecraft.world.entity.LivingEntity;

public final class RelicHealingService {
	private RelicHealingService() {
	}

	public static float heal(LivingEntity target, float requestedAmount, RelicHealingType type) {
		if (target == null || type == null || requestedAmount <= 0.0F || !target.isAlive())
			return 0.0F;

		float before = target.getHealth();
		float missing = Math.max(0.0F, target.getMaxHealth() - before);
		float modifiedAmount = HealingPresenceHealingModifier.modifyAmount(target, requestedAmount, type);
		target.heal(Math.min(modifiedAmount, missing));
		return Math.max(0.0F, target.getHealth() - before);
	}
}
