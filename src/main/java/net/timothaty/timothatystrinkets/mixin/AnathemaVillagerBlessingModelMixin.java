package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.client.AnathemaVillagerBlessingModelAnimation;
import net.timothaty.timothatystrinkets.client.AnathemaVillagerBlessingParticles;
import net.timothaty.timothatystrinkets.client.stunned.StunnedClientAnimationState;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaVillagerBlessingState;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.model.VillagerModel")
public abstract class AnathemaVillagerBlessingModelMixin {
	@Shadow
	@Final
	private ModelPart root;
	@Unique
	private boolean timothatys_trinkets$wasBlessing;

	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V", at = @At("HEAD"), require = 0)
	private void timothatys_trinkets$resetBlessingsPose(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
		if (this.timothatys_trinkets$wasBlessing)
			this.root.getAllParts().forEach(ModelPart::resetPose);
	}

	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V", at = @At("TAIL"), require = 0)
	private void timothatys_trinkets$applyBlessingsPose(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
		if (!(entity instanceof AnathemaVillagerBlessingState state)
			|| !state.timothatys_trinkets$isBlessingsAnimationActive()
			|| StunnedClientAnimationState.isStunned(entity)) {
			this.timothatys_trinkets$wasBlessing = false;
			return;
		}

		AnathemaVillagerBlessingModelAnimation.apply(state.timothatys_trinkets$getBlessingsAnimationState(), ageInTicks, this.root);
		AnathemaVillagerBlessingParticles.emit(entity, state, ageInTicks, this.root);
		this.timothatys_trinkets$wasBlessing = true;
	}
}
