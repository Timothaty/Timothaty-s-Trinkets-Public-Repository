package net.timothaty.timothatystrinkets.potion;

import net.timothaty.timothatystrinkets.mechanics.necromancer.NecromancerConfig;
import net.timothaty.timothatystrinkets.mechanics.necromancer.UnholyAuraEvents;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

public class UnholyAuraMobEffect extends MobEffect {
	public UnholyAuraMobEffect() {
		super(MobEffectCategory.HARMFUL, -13421773);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return duration % NecromancerConfig.UNHOLY_AURA_MODIFIER_UPDATE_INTERVAL_TICKS == 0;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		UnholyAuraEvents.refreshUnholyAuraModifiers(entity);
		return true;
	}
}
