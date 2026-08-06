package net.timothaty.timothatystrinkets.potion;

import net.timothaty.timothatystrinkets.mechanics.fire.FireSphereMoltenBaneEvents;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

public class MoltenBaneMobEffect extends MobEffect {
	private static final int MECHANICS_INTERVAL_TICKS = 5;

	public MoltenBaneMobEffect() {
		super(MobEffectCategory.HARMFUL, -1155584);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return duration % MECHANICS_INTERVAL_TICKS == 0;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		FireSphereMoltenBaneEvents.tickMoltenBane(entity);
		return super.applyEffectTick(entity, amplifier);
	}
}
