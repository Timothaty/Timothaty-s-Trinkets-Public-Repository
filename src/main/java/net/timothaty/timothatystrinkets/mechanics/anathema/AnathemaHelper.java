package net.timothaty.timothatystrinkets.mechanics.anathema;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public final class AnathemaHelper {
	private static final ThreadLocal<Set<UUID>> ALLOWED_REMOVALS = ThreadLocal.withInitial(HashSet::new);

	private AnathemaHelper() {
	}

	public static int getLevel(LivingEntity entity) {
		if (entity == null)
			return 0;

		MobEffectInstance instance = entity.getEffect(TimothatysTrinketsModMobEffects.ANATHEMA);
		return instance == null ? 0 : Math.min(AnathemaData.MAX_LEVEL, instance.getAmplifier() + 1);
	}

	public static boolean hasLevel(LivingEntity entity, int minimumLevel) {
		return getLevel(entity) >= minimumLevel;
	}

	public static void applyCrimeLevel(LivingEntity entity) {
		setLevel(entity, Math.min(AnathemaData.MAX_LEVEL, getLevel(entity) + 1));
	}

	public static void refreshCurrentLevel(LivingEntity entity) {
		int level = getLevel(entity);
		if (level > 0)
			setLevel(entity, level);
	}

	public static boolean reduceOneLevel(LivingEntity entity) {
		int level = getLevel(entity);
		if (level <= 0)
			return false;

		setLevel(entity, level - 1);
		return true;
	}

	public static void setLevel(LivingEntity entity, int level) {
		if (entity == null || entity.level().isClientSide())
			return;

		int clampedLevel = Math.max(0, Math.min(AnathemaData.MAX_LEVEL, level));
		withAllowedRemoval(entity, () -> entity.removeEffect(TimothatysTrinketsModMobEffects.ANATHEMA));

		if (clampedLevel > 0) {
			entity.addEffect(new MobEffectInstance(
				TimothatysTrinketsModMobEffects.ANATHEMA,
				AnathemaData.DURATION_TICKS,
				clampedLevel - 1,
				false,
				false,
				true
			));
		}
	}

	public static void replacePreservingDuration(LivingEntity entity, MobEffectInstance instance) {
		if (entity == null || instance == null || entity.level().isClientSide())
			return;

		withAllowedRemoval(entity, () -> entity.removeEffect(TimothatysTrinketsModMobEffects.ANATHEMA));
		entity.addEffect(new MobEffectInstance(
			TimothatysTrinketsModMobEffects.ANATHEMA,
			instance.getDuration(),
			Math.min(AnathemaData.MAX_LEVEL - 1, instance.getAmplifier()),
			instance.isAmbient(),
			false,
			instance.showIcon()
		));
	}

	public static boolean isRemovalAllowed(LivingEntity entity) {
		return entity != null && ALLOWED_REMOVALS.get().contains(entity.getUUID());
	}

	public static boolean withAllowedRemoval(LivingEntity entity, BooleanSupplier action) {
		if (entity == null || action == null)
			return false;

		Set<UUID> allowed = ALLOWED_REMOVALS.get();
		UUID uuid = entity.getUUID();
		boolean added = allowed.add(uuid);
		try {
			return action.getAsBoolean();
		} finally {
			if (added)
				allowed.remove(uuid);
			if (allowed.isEmpty())
				ALLOWED_REMOVALS.remove();
		}
	}
}
