package net.timothaty.timothatystrinkets.mixin;

import com.llamalad7.mixinextras.sugar.Local;

import net.timothaty.timothatystrinkets.mechanics.healing.HealingPresenceHealingModifier;
import net.timothaty.timothatystrinkets.mechanics.healing.RelicHealingType;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(FoodData.class)
public abstract class FoodDataNaturalHealingMixin {
	@ModifyArg(
			method = "tick",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"),
			index = 0,
			require = 2
	)
	private float timothatys_trinkets$modifyNaturalRegeneration(
			float originalAmount,
			@Local(argsOnly = true) Player player
	) {
		return HealingPresenceHealingModifier.modifyAmount(player, originalAmount, RelicHealingType.NATURAL);
	}
}
