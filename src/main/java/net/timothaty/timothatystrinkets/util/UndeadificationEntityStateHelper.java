package net.timothaty.timothatystrinkets.util;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class UndeadificationEntityStateHelper {
	private static final ResourceLocation UNDEADIFICATION_ID = ResourceLocation.fromNamespaceAndPath(
			TimothatysTrinketsMod.MODID,
			"undeadification"
	);
	private static final ResourceLocation UNDEADIFICATION_MOVEMENT_SPEED_ID = ResourceLocation.fromNamespaceAndPath(
			TimothatysTrinketsMod.MODID,
			"undeadification_movement_speed"
	);

	private UndeadificationEntityStateHelper() {
	}

	public static MobEffectInstance findUndeadification(LivingEntity entity) {
		MobEffectInstance directInstance = entity.getEffect(TimothatysTrinketsModMobEffects.UNDEADIFICATION);
		if (directInstance != null) {
			return directInstance;
		}

		for (MobEffectInstance instance : entity.getActiveEffects()) {
			ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect().value());
			if (UNDEADIFICATION_ID.equals(effectId)) {
				return instance;
			}
		}

		return null;
	}

	public static boolean hasUndeadificationVisualMarker(LivingEntity entity) {
		return findUndeadification(entity) != null || hasUndeadificationAttributeModifier(entity);
	}

	private static boolean hasUndeadificationAttributeModifier(LivingEntity entity) {
		AttributeInstance movementSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
		return movementSpeed != null && movementSpeed.getModifier(UNDEADIFICATION_MOVEMENT_SPEED_ID) != null;
	}
}
