package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.entity.UndeadKnightEntity;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class StunnedMobAiMixin {
	@Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$skipStunnedMobAi(CallbackInfo ci) {
		Mob mob = (Mob) (Object) this;

		if (!TimothatysTrinketsStunHelper.isStunned(mob) && !(mob instanceof UndeadKnightEntity knight && knight.isReincarnating()))
			return;

		mob.getNavigation().stop();
		mob.setTarget(null);
		mob.setLastHurtByMob(null);
		mob.setAggressive(false);
		mob.setJumping(false);
		((JumpControlAccessor) mob.getJumpControl()).timothatys_trinkets$setJumpRequested(false);
		mob.xxa = 0.0F;
		mob.yya = 0.0F;
		mob.zza = 0.0F;
		mob.setSpeed(0.0F);

		ci.cancel();
	}
}
