package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.client.morgenshtern.MorgenshternDecapitationClientState;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnergySwirlLayer.class)
public abstract class MorgenshternEnergySwirlLayerMixin {
	@Shadow
	protected abstract EntityModel<?> model();

	@Unique
	private ModelPart timothatys_trinkets$hiddenSwirlHead;

	@Unique
	private boolean timothatys_trinkets$savedSwirlHeadVisible;

	@Inject(method = "render", at = @At("HEAD"), require = 0)
	private void timothatys_trinkets$hideDecapitatedSwirlHead(
			PoseStack poseStack,
			MultiBufferSource buffer,
			int packedLight,
			Entity entity,
			float limbSwing,
			float limbSwingAmount,
			float partialTicks,
			float ageInTicks,
			float netHeadYaw,
			float headPitch,
			CallbackInfo ci
	) {
		if (!(entity instanceof LivingEntity livingEntity)
				|| !MorgenshternDecapitationClientState.isDecapitated(
						livingEntity
				))
			return;

		EntityModel<?> swirlModel = this.model();
		if (!(swirlModel instanceof HierarchicalModel<?> hierarchicalModel)
				|| !hierarchicalModel.root().hasChild("head"))
			return;

		ModelPart head = hierarchicalModel.root().getChild("head");
		this.timothatys_trinkets$hiddenSwirlHead = head;
		this.timothatys_trinkets$savedSwirlHeadVisible = head.visible;
		head.visible = false;
	}

	@Inject(method = "render", at = @At("RETURN"), require = 0)
	private void timothatys_trinkets$restoreDecapitatedSwirlHead(
			PoseStack poseStack,
			MultiBufferSource buffer,
			int packedLight,
			Entity entity,
			float limbSwing,
			float limbSwingAmount,
			float partialTicks,
			float ageInTicks,
			float netHeadYaw,
			float headPitch,
			CallbackInfo ci
	) {
		if (this.timothatys_trinkets$hiddenSwirlHead == null)
			return;

		this.timothatys_trinkets$hiddenSwirlHead.visible =
				this.timothatys_trinkets$savedSwirlHeadVisible;
		this.timothatys_trinkets$hiddenSwirlHead = null;
	}
}
