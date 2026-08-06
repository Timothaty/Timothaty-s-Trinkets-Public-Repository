package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.mechanics.pagans_charm.PaganCharmMeditationInterrupts;

import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PaganCharmServerboundSwingMixin {
	@Shadow
	public ServerPlayer player;

	@Inject(method = "handleAnimate", at = @At("HEAD"))
	private void timothatys_trinkets$interruptPaganCharmMeditationOnSwing(ServerboundSwingPacket packet, CallbackInfo ci) {
		PaganCharmMeditationInterrupts.interruptFromSwing(this.player);
	}

	@Inject(method = "handleUseItem", at = @At("HEAD"))
	private void timothatys_trinkets$allowPaganCharmMeditationUseItem(ServerboundUseItemPacket packet, CallbackInfo ci) {
		PaganCharmMeditationInterrupts.allowBenignSwing(this.player);
	}

	@Inject(method = "handleUseItemOn", at = @At("HEAD"))
	private void timothatys_trinkets$allowPaganCharmMeditationUseItemOn(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
		PaganCharmMeditationInterrupts.allowBenignSwing(this.player);
	}
}
