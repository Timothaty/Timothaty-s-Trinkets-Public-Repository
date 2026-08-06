package net.timothaty.timothatystrinkets.potion;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.blight.BlightImmunityHelper;
import net.timothaty.timothatystrinkets.mechanics.blight.BlightSpreadHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

public class PutrefactionMobEffect extends MobEffect {
	private static final String NBT_COMPLETED = "tt_putrefaction_completed";

	public PutrefactionMobEffect() {
		super(MobEffectCategory.HARMFUL, -16751104);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public void onEffectStarted(LivingEntity entity, int amplifier) {
		entity.getPersistentData().remove(NBT_COMPLETED);
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return super.applyEffectTick(entity, amplifier);
		}

		MobEffectInstance current = entity.getEffect(TimothatysTrinketsModMobEffects.PUTREFACTION);
		if (current != null && current.getDuration() <= 1 && !entity.getPersistentData().getBoolean(NBT_COMPLETED)) {
			entity.getPersistentData().putBoolean(NBT_COMPLETED, true);
			if (BlightImmunityHelper.isBlightImmune(entity)) {
				return super.applyEffectTick(entity, amplifier);
			}
			BlightSpreadHelper.startPutrefactionSpread(level, entity);
			entity.kill();
		}

		return super.applyEffectTick(entity, amplifier);
	}
}
