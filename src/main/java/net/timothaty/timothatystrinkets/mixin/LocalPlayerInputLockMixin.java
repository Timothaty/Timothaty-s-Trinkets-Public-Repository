package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.client.wrath_of_the_wicked.WrathOfTheWickedClientState;
import net.timothaty.timothatystrinkets.client.hubris.HubrisActivationClientState;
import net.timothaty.timothatystrinkets.client.debtlord.DebtlordHoldClientState;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerInputLockMixin {
	@Inject(method = "aiStep", at = @At("HEAD"))
	private void timothatys_trinkets$clearStaleLockedInput(CallbackInfo ci) {
		this.timothatys_trinkets$clearInputIfLocked();
	}

	@Inject(
			method = "aiStep",
			at = @At(
				value = "INVOKE",
				target = "Lnet/neoforged/neoforge/client/ClientHooks;onMovementInputUpdate(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/client/player/Input;)V",
				shift = At.Shift.AFTER
			)
	)
	private void timothatys_trinkets$clearFreshLockedInput(CallbackInfo ci) {
		this.timothatys_trinkets$clearInputIfLocked();
	}

	@Inject(
			method = "aiStep",
			at = @At(
				value = "INVOKE",
				target = "Lnet/minecraft/client/player/AbstractClientPlayer;aiStep()V"
			)
	)
	private void timothatys_trinkets$clearLockedInputBeforePhysics(CallbackInfo ci) {
		this.timothatys_trinkets$clearInputIfLocked();
	}

	@Inject(method = "isShiftKeyDown", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$preventLockedShift(CallbackInfoReturnable<Boolean> cir) {
		LocalPlayer player = (LocalPlayer) (Object) this;
		if (timothatys_trinkets$isInputLocked(player))
			cir.setReturnValue(false);
	}

	@Unique
	private void timothatys_trinkets$clearInputIfLocked() {
		LocalPlayer player = (LocalPlayer) (Object) this;
		if (!timothatys_trinkets$isInputLocked(player))
			return;

		Input input = player.input;
		if (input != null) {
			input.leftImpulse = 0.0F;
			input.forwardImpulse = 0.0F;
			input.up = false;
			input.down = false;
			input.left = false;
			input.right = false;
			input.jumping = false;
			input.shiftKeyDown = false;
		}

		player.setJumping(false);
		player.setSprinting(false);
	}

	@Unique
	private static boolean timothatys_trinkets$isInputLocked(LocalPlayer player) {
		return TimothatysTrinketsStunHelper.isStunned(player)
				|| HubrisActivationClientState.isInputLocked(player)
				|| DebtlordHoldClientState.isActive()
				|| WrathOfTheWickedClientState.isMovementLocked(player);
	}
}
