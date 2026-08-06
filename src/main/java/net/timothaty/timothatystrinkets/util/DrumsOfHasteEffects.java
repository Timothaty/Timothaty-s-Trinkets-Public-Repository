package net.timothaty.timothatystrinkets.util;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class DrumsOfHasteEffects {
	private DrumsOfHasteEffects() {
	}

	public static void applyAttributeModifiers(Player player, int stacks) {
		if (player == null)
			return;

		double mult = DrumsOfHasteData.clampStacks(stacks) * 0.01D;
		applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), DrumsOfHasteData.MOVE_SPEED_MOD_ID, mult);
		applyModifier(player.getAttribute(Attributes.ATTACK_SPEED), DrumsOfHasteData.ATTACK_SPEED_MOD_ID, mult);
	}

	public static float getBreakSpeedMultiplier(int stacks) {
		return 1.0F + (DrumsOfHasteData.clampStacks(stacks) * 0.01F);
	}

	private static void applyModifier(AttributeInstance attribute, net.minecraft.resources.ResourceLocation id, double amount) {
		if (attribute == null)
			return;

		if (amount > 0.0D) {
			attribute.addOrReplacePermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		} else {
			attribute.removeModifier(id);
		}
	}
}
