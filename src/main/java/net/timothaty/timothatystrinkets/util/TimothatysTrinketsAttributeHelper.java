package net.timothaty.timothatystrinkets.util;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public final class TimothatysTrinketsAttributeHelper {
	private TimothatysTrinketsAttributeHelper() {
	}

	public static void setModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation modifierId, double amount, AttributeModifier.Operation operation, boolean shouldHaveModifier) {
		if (entity == null || attribute == null || modifierId == null || operation == null)
			return;

		AttributeInstance instance = entity.getAttribute(attribute);
		if (instance == null)
			return;

		if (shouldHaveModifier) {
			AttributeModifier existing = instance.getModifier(modifierId);
			if (existing != null && existing.amount() == amount && existing.operation() == operation)
				return;

			instance.addOrReplacePermanentModifier(new AttributeModifier(modifierId, amount, operation));
		} else {
			if (instance.getModifier(modifierId) != null) {
				instance.removeModifier(modifierId);
			}
		}
	}

	public static void removeModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation modifierId) {
		setModifier(entity, attribute, modifierId, 0.0D, AttributeModifier.Operation.ADD_VALUE, false);
	}
}
