package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.mechanics.blight.BlightSunShelterHelper;

import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.monster.Zombie;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Zombie.class)
public abstract class UndeadSeekBlightGoalMixin {
	@Inject(method = "addBehaviourGoals", at = @At("RETURN"), require = 0)
	private void timothatys_trinkets$addVanillaSunFleeGoal(CallbackInfo ci) {
		Zombie zombie = (Zombie) (Object) this;
		if (!BlightSunShelterHelper.shouldUseBlightSunShelter(zombie)) {
			return;
		}

		zombie.goalSelector.addGoal(2, new RestrictSunGoal(zombie));
		zombie.goalSelector.addGoal(3, new FleeSunGoal(zombie, 1.0D));
	}
}
