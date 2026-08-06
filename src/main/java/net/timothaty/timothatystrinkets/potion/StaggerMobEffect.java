package net.timothaty.timothatystrinkets.potion;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class StaggerMobEffect extends MobEffect {
	public static final ResourceLocation SPEED_REDUCTION_ID =
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "stagger_speed_reduction");

	public StaggerMobEffect() {
		super(MobEffectCategory.HARMFUL, -2025962);
		this.addAttributeModifier(
				Attributes.MOVEMENT_SPEED,
				SPEED_REDUCTION_ID,
				-0.50D,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
		);
	}
}
