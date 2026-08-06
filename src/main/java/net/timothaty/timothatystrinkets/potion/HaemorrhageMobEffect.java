package net.timothaty.timothatystrinkets.potion;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class HaemorrhageMobEffect extends MobEffect {
	public static final ResourceLocation SLOW_MODIFIER_ID =
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "haemorrhage_slow");
	private static final double SLOW_MULTIPLIER = -0.20D;
	private static final int PARTICLE_INTERVAL_TICKS = 8;

	public HaemorrhageMobEffect() {
		super(MobEffectCategory.NEUTRAL, -10092544);
		this.addAttributeModifier(
				Attributes.MOVEMENT_SPEED,
				SLOW_MODIFIER_ID,
				SLOW_MULTIPLIER,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
		);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		if (entity.level() instanceof ServerLevel server && entity.tickCount % PARTICLE_INTERVAL_TICKS == 0) {
			server.sendParticles(
					TimothatysTrinketsModParticleTypes.BLOOD_BIT.get(),
					entity.getX(), entity.getY() + entity.getBbHeight() * 0.55D, entity.getZ(),
					2,
					entity.getBbWidth() * 0.35D, entity.getBbHeight() * 0.25D, entity.getBbWidth() * 0.35D,
					0.025D
			);
		}
		return true;
	}
}
