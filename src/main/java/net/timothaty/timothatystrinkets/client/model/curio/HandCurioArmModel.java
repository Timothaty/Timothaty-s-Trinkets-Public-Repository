package net.timothaty.timothatystrinkets.client.model.curio;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface HandCurioArmModel {
	void copyArmPoseFrom(HumanoidModel<?> model);

	void renderArm(
			HumanoidArm arm,
			PoseStack poseStack,
			VertexConsumer consumer,
			int light,
			int overlay
	);
}
