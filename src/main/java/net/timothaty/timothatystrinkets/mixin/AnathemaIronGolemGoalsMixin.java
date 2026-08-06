package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaIronGolemPatrolGoal;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaIronGolemTargetGoal;

import net.minecraft.world.entity.animal.IronGolem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IronGolem.class)
public abstract class AnathemaIronGolemGoalsMixin {
	@Inject(method = "registerGoals", at = @At("RETURN"))
	private void timothatys_trinkets$addAnathemaGoals(CallbackInfo ci) {
		IronGolem golem = (IronGolem) (Object) this;
		golem.goalSelector.addGoal(3, new AnathemaIronGolemPatrolGoal(golem));
		golem.targetSelector.addGoal(2, new AnathemaIronGolemTargetGoal(golem));
	}
}
