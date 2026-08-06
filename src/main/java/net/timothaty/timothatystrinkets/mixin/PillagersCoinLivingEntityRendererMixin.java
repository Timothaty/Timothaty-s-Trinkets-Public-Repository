package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.mechanics.pillagers_coin.PillagersCoinVillagerRuntimeState;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class PillagersCoinLivingEntityRendererMixin {
	@Inject(method = "isShaking", at = @At("RETURN"), cancellable = true)
	private void timothatys_trinkets$showExtortionShake(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue()
				&& entity instanceof Villager
				&& entity instanceof PillagersCoinVillagerRuntimeState state
				&& (state.timothatys_trinkets$isExtortionVisualActive()
					|| state.timothatys_trinkets$isFearVisualActive()))
			cir.setReturnValue(true);
	}
}
