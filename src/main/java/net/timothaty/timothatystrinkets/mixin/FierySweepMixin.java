package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.mechanics.fire.CustomSweepVisualState;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class FierySweepMixin {
	@Inject(method = "sweepAttack", at = @At("HEAD"), cancellable = true, require = 0)
	private void timothatys_trinkets$replaceSweepWithFierySweep(CallbackInfo ci) {
		Player player = (Player) (Object) this;
		CustomSweepVisualState.Visual visual = CustomSweepVisualState.consume(player);
		if (visual == CustomSweepVisualState.Visual.VANILLA)
			return;

		if (player.level() instanceof ServerLevel serverLevel) {
			double xOffset = -Math.sin(player.getYRot() * (Math.PI / 180.0D));
			double zOffset = Math.cos(player.getYRot() * (Math.PI / 180.0D));
			serverLevel.sendParticles(
					visual == CustomSweepVisualState.Visual.PRIDEFUL
							? TimothatysTrinketsModParticleTypes.PRIDEFUL_SWEEP.get()
							: TimothatysTrinketsModParticleTypes.FIERY_SWEEP_PARTICLE.get(),
					player.getX() + xOffset,
					player.getY(0.5D),
					player.getZ() + zOffset,
					0,
					xOffset,
					0.0D,
					zOffset,
					0.0D
			);
		}

		ci.cancel();
	}
}
