package net.timothaty.timothatystrinkets.util;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class TimothatysTrinketsStunHelper {
	public static final int PLAYER_STUN_TICKS = 20;
	public static final int DEFAULT_SPRINT_BASH_STUN_TICKS = 20;
	public static final int MAX_MOB_STUN_TICKS = 80;
	public static final int MAX_MINIBOSS_STUN_TICKS = 50;
	public static final int DEFAULT_STAGGER_TICKS = 40;
	public static final int POST_CONTROL_IMMUNITY_TICKS = 5 * 20;

	private TimothatysTrinketsStunHelper() {
	}

	public static boolean tryApplyStun(LivingEntity target, LivingEntity source, int requestedTicks) {
		return tryApplyStun(target, source, requestedTicks, PLAYER_STUN_TICKS, true);
	}

	public static boolean tryApplyStunSilently(LivingEntity target, LivingEntity source, int requestedTicks) {
		return tryApplyStun(target, source, requestedTicks, PLAYER_STUN_TICKS, false);
	}

	public static boolean tryApplyStunSilently(LivingEntity target, LivingEntity source, int requestedTicks, int playerDurationTicks) {
		return tryApplyStun(target, source, requestedTicks, playerDurationTicks, false);
	}

	private static boolean tryApplyStun(LivingEntity target, LivingEntity source, int requestedTicks, int playerDurationTicks, boolean playDefaultSound) {
		if (!canReceiveControl(target))
			return false;
		if (target.hasEffect(TimothatysTrinketsModMobEffects.RESONANCE_CAGE))
			return false;
		if (isControlImmune(target))
			return false;
		if (target.hasEffect(TimothatysTrinketsModMobEffects.STUNNED) || target.hasEffect(TimothatysTrinketsModMobEffects.STAGGER))
			return false;

		if (target.getType().is(TimothatysTrinketsStunTags.STAGGER_CREATURES)) {
			return tryApplyStagger(target, requestedTicks, playDefaultSound);
		}

		if (target.getType().is(TimothatysTrinketsStunTags.STUN_IMMUNE))
			return false;

		int duration = clampStunDuration(target, requestedTicks, playerDurationTicks);
		if (duration <= 0)
			return false;

		TimothatysTrinketsAbilityInterrupts.interruptAll(target);
		target.addEffect(new MobEffectInstance(TimothatysTrinketsModMobEffects.STUNNED, duration, 0, false, false, true));
		if (playDefaultSound) {
			playStunAppliedSound(target);
		}
		return true;
	}

	public static boolean tryApplyStagger(LivingEntity target, int requestedTicks) {
		return tryApplyStagger(target, requestedTicks, true);
	}

	private static boolean tryApplyStagger(LivingEntity target, int requestedTicks, boolean playDefaultSound) {
		if (!canReceiveControl(target))
			return false;
		if (target.hasEffect(TimothatysTrinketsModMobEffects.RESONANCE_CAGE))
			return false;
		if (isControlImmune(target))
			return false;
		if (target.hasEffect(TimothatysTrinketsModMobEffects.STUNNED) || target.hasEffect(TimothatysTrinketsModMobEffects.STAGGER))
			return false;
		if (!target.getType().is(TimothatysTrinketsStunTags.STAGGER_CREATURES))
			return false;

		int duration = Math.max(1, Math.min(requestedTicks, DEFAULT_STAGGER_TICKS));
		TimothatysTrinketsAbilityInterrupts.interruptAll(target);
		target.addEffect(new MobEffectInstance(TimothatysTrinketsModMobEffects.STAGGER, duration, 0, false, false, true));
		if (playDefaultSound) {
			playBossStaggerSound(target);
		}
		return true;
	}

	public static boolean isStunned(LivingEntity entity) {
		return entity != null && !isMechanicallyImmunePlayer(entity) && entity.hasEffect(TimothatysTrinketsModMobEffects.STUNNED);
	}

	public static boolean isStaggered(LivingEntity entity) {
		return entity != null && !isMechanicallyImmunePlayer(entity) && entity.hasEffect(TimothatysTrinketsModMobEffects.STAGGER);
	}

	public static boolean isControlImmune(LivingEntity entity) {
		return entity != null && entity.hasEffect(TimothatysTrinketsModMobEffects.STUN_IMMUNITY);
	}

	public static boolean canAttemptControl(LivingEntity entity) {
		if (!canReceiveControl(entity))
			return false;
		if (entity.hasEffect(TimothatysTrinketsModMobEffects.RESONANCE_CAGE))
			return false;
		if (isControlImmune(entity))
			return false;
		if (entity.hasEffect(TimothatysTrinketsModMobEffects.STUNNED) || entity.hasEffect(TimothatysTrinketsModMobEffects.STAGGER))
			return false;
		return !entity.getType().is(TimothatysTrinketsStunTags.STUN_IMMUNE) || entity.getType().is(TimothatysTrinketsStunTags.STAGGER_CREATURES);
	}

	public static boolean isMechanicallyImmunePlayer(LivingEntity entity) {
		return entity instanceof Player player && (player.isCreative() || player.isSpectator());
	}

	public static void applyPostStunImmunity(LivingEntity entity) {
		applyPostControlImmunity(entity);
	}

	public static void applyPostStaggerImmunity(LivingEntity entity) {
		applyPostControlImmunity(entity);
	}

	public static void applyPostControlImmunity(LivingEntity entity) {
		applyStunImmunity(entity, POST_CONTROL_IMMUNITY_TICKS);
	}

	public static void applyStunImmunity(LivingEntity entity, int ticks) {
		if (entity == null || entity.level().isClientSide() || ticks <= 0 || isMechanicallyImmunePlayer(entity))
			return;
		entity.addEffect(new MobEffectInstance(TimothatysTrinketsModMobEffects.STUN_IMMUNITY, ticks, 0, false, false, true));
	}

	private static int clampStunDuration(LivingEntity target, int requestedTicks, int playerDurationTicks) {
		if (requestedTicks <= 0)
			return 0;
		if (target instanceof Player)
			return Math.max(1, playerDurationTicks);
		if (target.getType().is(TimothatysTrinketsStunTags.STUN_MINIBOSSES))
			return Math.min(requestedTicks, MAX_MINIBOSS_STUN_TICKS);
		return Math.min(requestedTicks, MAX_MOB_STUN_TICKS);
	}

	private static boolean canReceiveControl(LivingEntity entity) {
		if (entity == null || entity.level().isClientSide())
			return false;
		if (!entity.isAlive() || entity.isDeadOrDying())
			return false;
		return !isMechanicallyImmunePlayer(entity);
	}

	private static void playStunAppliedSound(LivingEntity target) {
		if (target.level().isClientSide())
			return;

		if (target instanceof Player) {
			target.level().playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.55F, 1.35F);
		}
	}

	private static void playBossStaggerSound(LivingEntity target) {
		if (!target.level().isClientSide())
			target.level().playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.8F, 0.65F);
	}
}
