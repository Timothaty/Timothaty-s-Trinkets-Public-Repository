package net.timothaty.timothatystrinkets.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public final class ModelEchoSphere extends HierarchicalModel<Entity> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "echo_sphere"),
			"main"
	);

	private final ModelPart root;
	private final ModelPart echoSphere;
	private final ModelPart core;
	private final ModelPart outline;

	public ModelEchoSphere(ModelPart root) {
		this.root = root;
		this.echoSphere = root.getChild("echo_sphere");
		this.core = echoSphere.getChild("core");
		this.outline = echoSphere.getChild("outline");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition echoSphere = root.addOrReplaceChild(
				"echo_sphere",
				CubeListBuilder.create(),
				PartPose.offset(0.0F, 16.0F, 1.0F)
		);
		echoSphere.addOrReplaceChild(
				"core",
				CubeListBuilder.create()
						.texOffs(0, 0)
						.addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, CubeDeformation.NONE),
				PartPose.ZERO
		);
		echoSphere.addOrReplaceChild(
				"outline",
				CubeListBuilder.create()
						.texOffs(0, 0)
						.addBox(2.2F, 2.2F, 2.2F, -4.4F, -4.4F, -4.4F, CubeDeformation.NONE),
				PartPose.ZERO
		);
		return LayerDefinition.create(mesh, 16, 16);
	}

	@Override
	public ModelPart root() {
		return root;
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
		echoSphere.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
	}

	public void renderCoreToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
		renderChildWithAnimatedParent(core, poseStack, vertexConsumer, packedLight, packedOverlay, color);
	}

	public void renderOutlineToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
		renderChildWithAnimatedParent(outline, poseStack, vertexConsumer, packedLight, packedOverlay, color);
	}

	private void renderChildWithAnimatedParent(ModelPart child, PoseStack poseStack, VertexConsumer vertexConsumer,
			int packedLight, int packedOverlay, int color) {
		poseStack.pushPose();
		echoSphere.translateAndRotate(poseStack);
		child.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
		poseStack.popPose();
	}
}
