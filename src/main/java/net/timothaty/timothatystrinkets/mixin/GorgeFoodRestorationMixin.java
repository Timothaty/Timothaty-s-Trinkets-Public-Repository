package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.gorge.GorgeState;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class GorgeFoodRestorationMixin {
	@Redirect(
			method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/food/FoodProperties;)V"
			)
	)
	private void timothatysTrinkets$blockOrdinaryFoodRestoration(
			FoodData foodData,
			FoodProperties foodProperties
	) {
		Player player = (Player) (Object) this;
		if (!GorgeState.isAbilityActive(player)) {
			foodData.eat(foodProperties);
		}
	}
}
