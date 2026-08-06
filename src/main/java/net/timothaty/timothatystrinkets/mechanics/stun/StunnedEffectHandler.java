package net.timothaty.timothatystrinkets.mechanics.stun;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.network.StunnedCameraShakeMessage;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsControlData;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsControlParticles;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsControlSounds;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class StunnedEffectHandler {
	private StunnedEffectHandler() {
	}


	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Pre event) {
		Entity rawEntity = event.getEntity();
		if (!(rawEntity instanceof LivingEntity living))
			return;
		if (living.level().isClientSide())
			return;

		CompoundTag data = living.getPersistentData();
		boolean hasStunnedEffect = living.hasEffect(TimothatysTrinketsModMobEffects.STUNNED);
		boolean hasStaggerEffect = living.hasEffect(TimothatysTrinketsModMobEffects.STAGGER);
		boolean hasControlImmunity = living.hasEffect(TimothatysTrinketsModMobEffects.STUN_IMMUNITY);
		boolean hasRuntimeState = data.getBoolean(TimothatysTrinketsControlData.NBT_STUN_ACTIVE)
				|| data.getBoolean(TimothatysTrinketsControlData.NBT_STAGGER_ACTIVE);
		if (!hasStunnedEffect && !hasStaggerEffect && !hasControlImmunity && !hasRuntimeState) {
			return;
		}

		TimothatysTrinketsControlParticles.hideVisibleControlEffectParticles(living);
		if (!living.isAlive()) {
			handleStunEndIfNeeded(living, false);
			handleStaggerEndIfNeeded(living, false);
			return;
		}

		if (TimothatysTrinketsStunHelper.isMechanicallyImmunePlayer(living)) {
			clearControlEffectsIfImmune(living);
			handleStunEndIfNeeded(living, false);
			handleStaggerEndIfNeeded(living, false);
			return;
		}

		if (TimothatysTrinketsStunHelper.isControlImmune(living)) {
			clearControlEffectsIfImmune(living);
			handleStunEndIfNeeded(living, false);
			handleStaggerEndIfNeeded(living, false);
			return;
		}

		boolean stunned = TimothatysTrinketsStunHelper.isStunned(living);
		boolean staggered = TimothatysTrinketsStunHelper.isStaggered(living);

		if (stunned) {
			handleStunnedTick(living, staggered);
		} else {
			handleStunEndIfNeeded(living, true);
		}

		if (staggered) {
			handleStaggerTick(living, stunned);
		} else {
			handleStaggerEndIfNeeded(living, true);
		}
	}

	private static void handleStunnedTick(LivingEntity living, boolean hasStaggerToo) {
		CompoundTag data = living.getPersistentData();

		if (!data.getBoolean(TimothatysTrinketsControlData.NBT_STUN_ACTIVE)) {
			data.putBoolean(TimothatysTrinketsControlData.NBT_STUN_ACTIVE, true);
			data.putLong(TimothatysTrinketsControlData.NBT_STUNNED_NEXT_LOOP_TICK, 0L);
			living.stopUsingItem();
			shakeStunnedPlayerCamera(living);
		}

		data.putLong(TimothatysTrinketsControlData.NBT_STUN_END_TICK, living.level().getGameTime());
		TimothatysTrinketsControlSounds.playStunnedLoopIfNeeded(living, data);

		living.setSprinting(false);
		living.hurtMarked = true;
		StunnedMovementController.freeze(living);

		if (living instanceof Mob mob) {
			StunnedMovementController.clearMobCombatState(mob);
		}

		TimothatysTrinketsControlParticles.spawnSpiralKeepalive(living, true, hasStaggerToo);
	}

	private static void shakeStunnedPlayerCamera(LivingEntity living) {
		if (living instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer, StunnedCameraShakeMessage.INSTANCE);
		}
	}

	private static void handleStaggerTick(LivingEntity living, boolean hasStunToo) {
		CompoundTag data = living.getPersistentData();
		data.putBoolean(TimothatysTrinketsControlData.NBT_STAGGER_ACTIVE, true);
		data.putLong(TimothatysTrinketsControlData.NBT_STAGGER_END_TICK, living.level().getGameTime());
		TimothatysTrinketsControlParticles.spawnSpiralKeepalive(living, false, hasStunToo);
	}

	private static void handleStunEndIfNeeded(LivingEntity living, boolean applyImmunity) {
		CompoundTag data = living.getPersistentData();
		if (!data.getBoolean(TimothatysTrinketsControlData.NBT_STUN_ACTIVE))
			return;

		if (living instanceof Mob mob) {
			StunnedMovementController.clearMobCombatState(mob);
		}

		data.remove(TimothatysTrinketsControlData.NBT_STUN_ACTIVE);
		data.remove(TimothatysTrinketsControlData.NBT_STUN_LOCKED_SLOT);
		data.remove(TimothatysTrinketsControlData.NBT_STUN_END_TICK);
		data.remove(TimothatysTrinketsControlData.NBT_STUNNED_NEXT_LOOP_TICK);

		if (applyImmunity) {
			TimothatysTrinketsStunHelper.applyPostStunImmunity(living);
		}
	}

	private static void handleStaggerEndIfNeeded(LivingEntity living, boolean applyImmunity) {
		CompoundTag data = living.getPersistentData();
		if (!data.getBoolean(TimothatysTrinketsControlData.NBT_STAGGER_ACTIVE))
			return;
		data.remove(TimothatysTrinketsControlData.NBT_STAGGER_ACTIVE);
		data.remove(TimothatysTrinketsControlData.NBT_STAGGER_END_TICK);
		if (applyImmunity) {
			TimothatysTrinketsStunHelper.applyPostStaggerImmunity(living);
		}
	}

	private static void clearControlEffectsIfImmune(LivingEntity living) {
		if (living.hasEffect(TimothatysTrinketsModMobEffects.STUNNED)) {
			living.removeEffect(TimothatysTrinketsModMobEffects.STUNNED);
		}
		if (living.hasEffect(TimothatysTrinketsModMobEffects.STAGGER)) {
			living.removeEffect(TimothatysTrinketsModMobEffects.STAGGER);
		}
	}
}
