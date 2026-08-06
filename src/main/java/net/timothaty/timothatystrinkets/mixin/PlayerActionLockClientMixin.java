package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.client.hubris.HubrisActivationClientState;
import net.timothaty.timothatystrinkets.client.debtlord.DebtlordHoldClientState;
import net.timothaty.timothatystrinkets.client.wrath_of_the_wicked.WrathOfTheWickedClientState;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class PlayerActionLockClientMixin {
	@Unique
	private boolean timothatys_trinkets$actionsWereLocked;
	@Unique
	private boolean timothatys_trinkets$fullInputWasLocked;
	@Unique
	private ClientLevel timothatys_trinkets$trackedLevel;
	@Unique
	private LocalPlayer timothatys_trinkets$trackedPlayer;

	@Inject(method = "handleKeybinds", at = @At("HEAD"))
	private void timothatys_trinkets$blockGameplayActions(CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (!this.timothatys_trinkets$updateActionLock(minecraft, player))
			return;

		boolean fullInputLock = HubrisActivationClientState.isInputLocked(player)
				|| DebtlordHoldClientState.isActive()
				|| timothatys_trinkets$isControlledByStun(player);
		this.timothatys_trinkets$fullInputWasLocked = fullInputLock;
		if (fullInputLock) {
			timothatys_trinkets$drainOneShotClicks(minecraft);
		}
		timothatys_trinkets$drainActionClicks(minecraft);
	}

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$blockUseItem(CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();
		if (this.timothatys_trinkets$updateActionLock(minecraft, minecraft.player))
			ci.cancel();
	}

	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$blockAttack(CallbackInfoReturnable<Boolean> cir) {
		Minecraft minecraft = Minecraft.getInstance();
		if (this.timothatys_trinkets$updateActionLock(minecraft, minecraft.player))
			cir.setReturnValue(false);
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$blockContinuedAttack(boolean leftClick, CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();
		if (this.timothatys_trinkets$updateActionLock(minecraft, minecraft.player))
			ci.cancel();
	}

	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$blockHubrisInventory(Screen screen, CallbackInfo ci) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null
				&& (HubrisActivationClientState.isInputLocked(player) || DebtlordHoldClientState.isActive())
				&& (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen)) {
			ci.cancel();
		}
	}

	@Unique
	private boolean timothatys_trinkets$updateActionLock(Minecraft minecraft, LocalPlayer player) {
		ClientLevel level = minecraft.level;
		if (level != this.timothatys_trinkets$trackedLevel
				|| player != this.timothatys_trinkets$trackedPlayer) {
			this.timothatys_trinkets$actionsWereLocked = false;
			this.timothatys_trinkets$fullInputWasLocked = false;
			this.timothatys_trinkets$trackedLevel = level;
			this.timothatys_trinkets$trackedPlayer = player;
		}

		boolean locked = timothatys_trinkets$isActionLocked(player);
		if (locked && !this.timothatys_trinkets$actionsWereLocked && minecraft.gameMode != null)
			minecraft.gameMode.stopDestroyBlock();
		if (!locked && this.timothatys_trinkets$actionsWereLocked) {
			timothatys_trinkets$drainActionClicks(minecraft);
			if (this.timothatys_trinkets$fullInputWasLocked)
				timothatys_trinkets$drainOneShotClicks(minecraft);
			this.timothatys_trinkets$fullInputWasLocked = false;
		}
		this.timothatys_trinkets$actionsWereLocked = locked;
		return locked;
	}

	@Unique
	private static boolean timothatys_trinkets$isActionLocked(LocalPlayer player) {
		return player != null && (HubrisActivationClientState.isInputLocked(player)
				|| WrathOfTheWickedClientState.isActive(player)
				|| DebtlordHoldClientState.isActive()
				|| timothatys_trinkets$isControlledByStun(player));
	}

	@Unique
	private static boolean timothatys_trinkets$isControlledByStun(LocalPlayer player) {
		return TimothatysTrinketsStunHelper.isStunned(player)
				&& !player.isCreative()
				&& !player.isSpectator();
	}

	@Unique
	private static void timothatys_trinkets$drainClicks(KeyMapping keyMapping) {
		while (keyMapping != null && keyMapping.consumeClick()) {
		}
	}

	@Unique
	private static void timothatys_trinkets$drainActionClicks(Minecraft minecraft) {
		timothatys_trinkets$drainClicks(minecraft.options.keyAttack);
		timothatys_trinkets$drainClicks(minecraft.options.keyUse);
	}

	@Unique
	private static void timothatys_trinkets$drainOneShotClicks(Minecraft minecraft) {
		for (KeyMapping hotbarKey : minecraft.options.keyHotbarSlots)
			timothatys_trinkets$drainClicks(hotbarKey);
		timothatys_trinkets$drainClicks(minecraft.options.keyInventory);
		timothatys_trinkets$drainClicks(minecraft.options.keySwapOffhand);
		timothatys_trinkets$drainClicks(minecraft.options.keyDrop);
		timothatys_trinkets$drainClicks(minecraft.options.keyPickItem);
	}
}
