package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.mechanics.active_ability.PlayerActionLockHelper;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris.HubrisActivationState;
import net.timothaty.timothatystrinkets.mechanics.debtlord.DebtlordTelekineticHold;

import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class HubrisServerPacketLockMixin {
	@Shadow
	public ServerPlayer player;

	@Inject(method = "handleSetCarriedItem", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$blockCarriedItem(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) {
		timothatys_trinkets$cancelHubrisOnly(ci);
	}

	@Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$blockPlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
		if (HubrisActivationState.isCasting(this.player) || DebtlordTelekineticHold.isHeld(this.player)) {
			ci.cancel();
			return;
		}
		if (!PlayerActionLockHelper.isActionBlocked(this.player))
			return;

		// Wrath locks block attacks, but inventory, drop, offhand, and release actions must reach the server.
		switch (packet.getAction()) {
			case START_DESTROY_BLOCK, ABORT_DESTROY_BLOCK, STOP_DESTROY_BLOCK -> ci.cancel();
			default -> {
			}
		}
	}

	@Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$blockContainerClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
		timothatys_trinkets$cancelHubrisOnly(ci);
	}

	@Inject(method = "handleContainerButtonClick", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$blockContainerButton(ServerboundContainerButtonClickPacket packet, CallbackInfo ci) {
		timothatys_trinkets$cancelHubrisOnly(ci);
	}

	@Inject(method = "handlePickItem", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$blockPickItem(ServerboundPickItemPacket packet, CallbackInfo ci) {
		timothatys_trinkets$cancelHubrisOnly(ci);
	}

	@Inject(method = "handleUseItem", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$blockUseItem(ServerboundUseItemPacket packet, CallbackInfo ci) {
		timothatys_trinkets$cancelActionWhileLocked(ci);
	}

	@Inject(method = "handleUseItemOn", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$blockUseItemOn(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
		timothatys_trinkets$cancelActionWhileLocked(ci);
	}

	@Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$blockInteract(ServerboundInteractPacket packet, CallbackInfo ci) {
		timothatys_trinkets$cancelActionWhileLocked(ci);
	}

	@Inject(method = "handleAnimate", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$blockSwing(ServerboundSwingPacket packet, CallbackInfo ci) {
		timothatys_trinkets$cancelActionWhileLocked(ci);
	}

	@Inject(method = "handlePlayerCommand", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$blockPlayerCommand(ServerboundPlayerCommandPacket packet, CallbackInfo ci) {
		timothatys_trinkets$cancelHubrisOnly(ci);
	}

	@Unique
	private void timothatys_trinkets$cancelHubrisOnly(CallbackInfo ci) {
		if (HubrisActivationState.isCasting(this.player) || DebtlordTelekineticHold.isHeld(this.player))
			ci.cancel();
	}

	@Unique
	private void timothatys_trinkets$cancelActionWhileLocked(CallbackInfo ci) {
		if (PlayerActionLockHelper.isActionBlocked(this.player))
			ci.cancel();
	}
}
