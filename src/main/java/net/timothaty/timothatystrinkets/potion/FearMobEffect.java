package net.timothaty.timothatystrinkets.potion;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class FearMobEffect extends MobEffect {
	private static final double BASE_MOVEMENT_SPEED_REDUCTION = 0.15D;
	private static final double MOVEMENT_SPEED_REDUCTION_PER_EXTRA_LEVEL = 0.05D;
	private static final double BASE_ATTACK_SPEED_REDUCTION = 0.20D;
	private static final double ATTACK_SPEED_REDUCTION_PER_EXTRA_LEVEL = 0.05D;

	public FearMobEffect() {
		super(MobEffectCategory.HARMFUL, -4976875);
		this.addAttributeModifier(
			Attributes.MOVEMENT_SPEED,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "fear_movement_speed"),
			AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
			amplifier -> -(BASE_MOVEMENT_SPEED_REDUCTION + MOVEMENT_SPEED_REDUCTION_PER_EXTRA_LEVEL * amplifier)
		);
		this.addAttributeModifier(
			Attributes.ATTACK_SPEED,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "fear_attack_speed"),
			AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
			amplifier -> -(BASE_ATTACK_SPEED_REDUCTION + ATTACK_SPEED_REDUCTION_PER_EXTRA_LEVEL * amplifier)
		);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		entity.setSprinting(false);
		return super.applyEffectTick(entity, amplifier);
	}
}
