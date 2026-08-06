package net.timothaty.timothatystrinkets.mechanics.debtlord;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;

public final class DebtlordDamageScaling {
	public static final float DIRECT_ATTACK_IRON_GOLEM_MULTIPLIER = 2.0F;
	public static final float STOMP_IRON_GOLEM_MULTIPLIER = 4.0F;

	private DebtlordDamageScaling() {
	}

	public static float scaleDamage(LivingEntity target, float baseDamage, float ironGolemMultiplier) {
		return baseDamage * getAppliedMultiplier(target, ironGolemMultiplier);
	}

	public static float getAppliedMultiplier(LivingEntity target, float ironGolemMultiplier) {
		return target instanceof IronGolem ? ironGolemMultiplier : 1.0F;
	}

	public static float getEffectiveHealth(LivingEntity target) {
		return target.getHealth() + target.getAbsorptionAmount();
	}

	public static float getActualDamage(LivingEntity target, float effectiveHealthBefore) {
		return Math.max(0.0F, effectiveHealthBefore - getEffectiveHealth(target));
	}

	public static float normalizeDamageForLifeSteal(LivingEntity target, float actualDamage, float appliedMultiplier) {
		if (!(target instanceof IronGolem) || appliedMultiplier <= 1.0F)
			return actualDamage;
		return actualDamage / appliedMultiplier;
	}
}
