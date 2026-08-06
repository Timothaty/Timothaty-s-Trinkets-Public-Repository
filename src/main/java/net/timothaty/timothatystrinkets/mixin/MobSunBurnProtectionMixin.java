package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.blight.BlightAuraCache;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobSunBurnProtectionMixin {
	@Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$unholyAuraProtectsUndeadFromSun(CallbackInfoReturnable<Boolean> cir) {
		Mob mob = (Mob) (Object) this;
		if (mob.getType().is(EntityTypeTags.UNDEAD) && mob.hasEffect(TimothatysTrinketsModMobEffects.UNHOLY_AURA))
			cir.setReturnValue(false);
	}

	@Inject(method = "isSunBurnTick", at = @At("RETURN"), cancellable = true, require = 0)
	private void timothatys_trinkets$blightProtectsSunSensitiveMobs(CallbackInfoReturnable<Boolean> cir) {
		Mob mob = (Mob) (Object) this;
		if (cir.getReturnValueZ() && BlightAuraCache.isInsideBlightAura(mob))
			cir.setReturnValue(false);
	}
}
