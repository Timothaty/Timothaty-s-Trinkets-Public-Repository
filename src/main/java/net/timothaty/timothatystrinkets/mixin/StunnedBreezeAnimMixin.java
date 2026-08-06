package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.client.stunned.StunnedHeadAnimation;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.monster.breeze.Breeze;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.model.BreezeModel")
public abstract class StunnedBreezeAnimMixin {
	@Shadow
	@Final
	private ModelPart head;

	@Inject(
			method = "setupAnim(Lnet/minecraft/world/entity/monster/breeze/Breeze;FFFFF)V",
			at = @At("TAIL"),
			require = 0
	)
	private void timothatys_trinkets$applyStunnedHeadAnimation(
			Breeze entity,
			float limbSwing,
			float limbSwingAmount,
			float ageInTicks,
			float netHeadYaw,
			float headPitch,
			CallbackInfo ci
	) {
		if (!StunnedHeadAnimation.shouldAnimate(entity))
			return;

		StunnedHeadAnimation.apply(this.head, ageInTicks);
	}
}
