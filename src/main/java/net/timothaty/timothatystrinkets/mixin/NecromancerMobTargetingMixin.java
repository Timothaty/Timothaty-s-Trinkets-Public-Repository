package net.timothaty.timothatystrinkets.mixin;

import javax.annotation.Nullable;

import net.timothaty.timothatystrinkets.mechanics.necromancer.NecromancerAllyHelper;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

@Mixin(TargetingConditions.class)
public abstract class NecromancerMobTargetingMixin {
	@Shadow
	@Final
	private boolean isCombat;

	@Inject(method = "test", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$undeadIgnoreUndeadifiedTargets(
		@Nullable LivingEntity attacker,
		LivingEntity target,
		CallbackInfoReturnable<Boolean> cir
	) {
		if (isCombat && NecromancerAllyHelper.shouldUndeadIgnoreTarget(attacker, target)) {
			cir.setReturnValue(false);
		}
	}
}
