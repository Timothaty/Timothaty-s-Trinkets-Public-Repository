package net.timothaty.timothatystrinkets.potion;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.mechanics.void_sphere.MarkedByVoidHandler;
import net.timothaty.timothatystrinkets.util.VoidMarkParticleData;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class MarkedbyVoidMobEffect extends MobEffect {
	private static final int VOID_MARK_PARTICLE_INTERVAL_TICKS = 4;

	public MarkedbyVoidMobEffect() {
		super(MobEffectCategory.HARMFUL, 0x330099);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		if (entity.level() instanceof ServerLevel serverLevel) {
			MobEffectInstance current = entity.getEffect(TimothatysTrinketsModMobEffects.MARKED_BY_VOID);
			if (current == null) {
				return super.applyEffectTick(entity, amplifier);
			}

			if (current.getDuration() <= 1) {
				MarkedByVoidHandler.releaseMark(entity, false);
				return super.applyEffectTick(entity, amplifier);
			}

			spawnVoidMarkParticle(serverLevel, entity);
		}

		return super.applyEffectTick(entity, amplifier);
	}

	private static void spawnVoidMarkParticle(ServerLevel serverLevel, LivingEntity entity) {
		if (entity.tickCount % VOID_MARK_PARTICLE_INTERVAL_TICKS != 0) {
			return;
		}

		serverLevel.sendParticles(
				TimothatysTrinketsModParticleTypes.VOID_MARK.get(),
				entity.getX(),
				entity.getY() + entity.getBbHeight(),
				entity.getZ(),
				0,
				VoidMarkParticleData.encodeEntityId(entity),
				VoidMarkParticleData.attachMagic(),
				0.0D,
				1.0D
		);
	}
}
