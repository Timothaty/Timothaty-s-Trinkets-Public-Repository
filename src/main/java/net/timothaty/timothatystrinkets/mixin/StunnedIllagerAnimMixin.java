package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.client.stunned.StunnedHeadAnimation;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.monster.AbstractIllager;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.model.IllagerModel")
public abstract class StunnedIllagerAnimMixin {
	@Unique
	private boolean timothatys_trinkets$wasStunned;

	@Shadow
	@Final
	private ModelPart head;

	@Inject(
			method = "setupAnim(Lnet/minecraft/world/entity/monster/AbstractIllager;FFFFF)V",
			at = @At("TAIL"),
			require = 0
	)
	private void timothatys_trinkets$applyStunnedHeadAnimation(
			AbstractIllager entity,
			float limbSwing,
			float limbSwingAmount,
			float ageInTicks,
			float netHeadYaw,
			float headPitch,
			CallbackInfo ci
	) {
		if (!StunnedHeadAnimation.shouldAnimate(entity)) {
			if (this.timothatys_trinkets$wasStunned)
				StunnedHeadAnimation.resetRoll(this.head);

			this.timothatys_trinkets$wasStunned = false;
			return;
		}

		this.timothatys_trinkets$wasStunned = true;
		StunnedHeadAnimation.apply(this.head, ageInTicks);
	}
}
