package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RestrictSunGoal.class)
public abstract class UnholyAuraRestrictSunGoalMixin {
	@Shadow
	@Final
	private PathfinderMob mob;

	@Inject(method = "canUse", at = @At("HEAD"), cancellable = true, require = 0)
	private void timothatys_trinkets$skipSunRestrictionUnderUnholyAura(CallbackInfoReturnable<Boolean> cir) {
		if (mob.getType().is(EntityTypeTags.UNDEAD) && mob.hasEffect(TimothatysTrinketsModMobEffects.UNHOLY_AURA)) {
			cir.setReturnValue(false);
		}
	}
}
