package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.client.hubris.HubrisActivationClientState;
import net.timothaty.timothatystrinkets.client.wrath_of_the_wicked.WrathOfTheWickedClientState;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class WrathOfTheWickedMouseHandlerMixin {
	@Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$lockWrathCamera(double movementTime, CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player != null
				&& minecraft.screen == null
				&& minecraft.getOverlay() == null
				&& minecraft.options.getCameraType() == CameraType.FIRST_PERSON
				&& WrathOfTheWickedClientState.isMouseLocked(player)) {
			ci.cancel();
		}
	}

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$lockHubrisHotbarScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null && HubrisActivationClientState.isInputLocked(player))
			ci.cancel();
	}
}
