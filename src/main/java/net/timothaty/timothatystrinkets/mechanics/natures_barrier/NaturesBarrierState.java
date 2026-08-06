package net.timothaty.timothatystrinkets.mechanics.natures_barrier;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class NaturesBarrierState {
	private NaturesBarrierState() {
	}

	public static boolean hasBarrier(LivingEntity entity) {
		return entity != null && entity.hasEffect(TimothatysTrinketsModMobEffects.NATURES_BARRIER);
	}

	public static float getMaxAbsorption(MobEffectInstance effect) {
		int amplifier = effect == null ? 0 : Math.max(0, effect.getAmplifier());
		return NaturesBarrierTuning.BASE_ABSORPTION + amplifier * NaturesBarrierTuning.ABSORPTION_PER_EXTRA_LEVEL;
	}

	public static float getOrCreateRemainingAbsorption(LivingEntity entity, MobEffectInstance effect) {
		CompoundTag data = entity.getPersistentData();
		if (!data.contains(NaturesBarrierTuning.NBT_REMAINING_ABSORPTION)) {
			setRemainingAbsorption(entity, getMaxAbsorption(effect));
		}

		return Math.max(0.0F, data.getFloat(NaturesBarrierTuning.NBT_REMAINING_ABSORPTION));
	}

	public static void setRemainingAbsorption(LivingEntity entity, float amount) {
		entity.getPersistentData().putFloat(NaturesBarrierTuning.NBT_REMAINING_ABSORPTION, Math.max(0.0F, amount));
	}

	public static void resetAbsorption(LivingEntity entity, MobEffectInstance effect) {
		setRemainingAbsorption(entity, getMaxAbsorption(effect));
	}

	public static boolean hasStoredAbsorption(LivingEntity entity) {
		return entity != null && entity.getPersistentData().contains(NaturesBarrierTuning.NBT_REMAINING_ABSORPTION);
	}

	public static void clear(LivingEntity entity) {
		if (entity == null)
			return;

		entity.getPersistentData().remove(NaturesBarrierTuning.NBT_REMAINING_ABSORPTION);
	}

	public static void markBarrierHurtSound(LivingEntity entity) {
		if (entity == null || entity.level().isClientSide())
			return;

		entity.getPersistentData().putLong(NaturesBarrierTuning.NBT_HURT_SOUND_UNTIL, entity.level().getGameTime() + 2L);
	}

	public static boolean shouldUseBarrierHurtSound(LivingEntity entity) {
		if (entity == null)
			return false;
		if (hasBarrier(entity))
			return true;

		CompoundTag data = entity.getPersistentData();
		return data.getLong(NaturesBarrierTuning.NBT_HURT_SOUND_UNTIL) >= entity.level().getGameTime();
	}
}
