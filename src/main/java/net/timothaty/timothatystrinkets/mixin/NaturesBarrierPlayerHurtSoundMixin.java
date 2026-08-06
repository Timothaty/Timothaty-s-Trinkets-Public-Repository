package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.mechanics.natures_barrier.NaturesBarrierSounds;
import net.timothaty.timothatystrinkets.mechanics.natures_barrier.NaturesBarrierState;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class NaturesBarrierPlayerHurtSoundMixin {
	@Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true, require = 0)
	private void timothatys_trinkets$useNaturesBarrierHurtSound(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
		Player player = (Player) (Object) this;
		if (NaturesBarrierState.shouldUseBarrierHurtSound(player)) {
			cir.setReturnValue(NaturesBarrierSounds.hurtSound());
		}
	}
}
