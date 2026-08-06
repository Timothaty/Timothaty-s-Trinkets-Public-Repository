package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris.HubrisStrikeResolver;
import net.timothaty.timothatystrinkets.mechanics.fire.CustomSweepVisualState;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class HubrisPlayerAttackMixin {
	@Inject(method = "attack", at = @At("HEAD"))
	private void timothatys_trinkets$beginAttackScope(Entity target, CallbackInfo ci) {
		Player player = (Player) (Object) this;
		if (!(player instanceof ServerPlayer serverPlayer))
			return;
		CustomSweepVisualState.beginAttack(player);
		HubrisStrikeResolver.beginAttack(serverPlayer, target);
	}

	@Inject(method = "attack", at = @At("RETURN"))
	private void timothatys_trinkets$finishAttackScope(Entity target, CallbackInfo ci) {
		Player player = (Player) (Object) this;
		if (!(player instanceof ServerPlayer serverPlayer))
			return;
		try {
			HubrisStrikeResolver.finishAttack(serverPlayer);
		} finally {
			CustomSweepVisualState.endAttack(player);
		}
	}
}
