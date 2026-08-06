// Made with Blockbench 5.1.6
// Exported for Minecraft version 1.17 or later with Mojang mappings
package net.timothaty.timothatystrinkets.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.model.animations.BeatificPalliumAnimations;
import net.timothaty.timothatystrinkets.entity.BeatificPalliumEntity;

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

public final class ModelBeatificPallium extends HierarchicalModel<BeatificPalliumEntity> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "beatific_pallium"),
			"main"
	);
	private final ModelPart root;
	private final ModelPart pallium;
	private final ModelPart palium_inner;
	private final ModelPart pallium_runes;

	public ModelBeatificPallium(ModelPart root) {
		this.root = root;
		this.pallium = root.getChild("pallium");
		this.palium_inner = this.pallium.getChild("palium_inner");
		this.pallium_runes = this.pallium.getChild("pallium_runes");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition pallium = partdefinition.addOrReplaceChild("pallium", CubeListBuilder.create(), PartPose.offset(-0.1F, 2.5F, -1.5F));

		PartDefinition palium_inner = pallium.addOrReplaceChild("palium_inner", CubeListBuilder.create().texOffs(0, 48).addBox(-11.5F, -11.5F, -11.5F, 23.0F, 23.0F, 23.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition pallium_runes = pallium.addOrReplaceChild("pallium_runes", CubeListBuilder.create().texOffs(0, 0).addBox(-11.9F, -12.0F, -12.1F, 24.0F, 24.0F, 24.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public ModelPart root() {
		return root;
	}

	@Override
	public void setupAnim(BeatificPalliumEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		root().getAllParts().forEach(ModelPart::resetPose);
		this.animate(entity.appearanceAnimationState, BeatificPalliumAnimations.SHIELD_APPEARANCE, ageInTicks);
		this.animate(entity.loopAnimationState, BeatificPalliumAnimations.SHIELD_LOOP, ageInTicks);
		this.animate(entity.fadeAnimationState, BeatificPalliumAnimations.SHIELD_FADE, ageInTicks);
		this.animate(entity.burstAnimationState, BeatificPalliumAnimations.SHIELD_BURST, ageInTicks);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
		pallium.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
	}

	public void renderInnerToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
		renderChildWithAnimatedParent(palium_inner, poseStack, vertexConsumer, packedLight, packedOverlay, color);
	}

	public void renderRunesToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
		renderChildWithAnimatedParent(pallium_runes, poseStack, vertexConsumer, packedLight, packedOverlay, color);
	}

	public void translatePallium(PoseStack poseStack) {
		pallium.translateAndRotate(poseStack);
	}

	private void renderChildWithAnimatedParent(ModelPart child, PoseStack poseStack, VertexConsumer vertexConsumer,
			int packedLight, int packedOverlay, int color) {
		poseStack.pushPose();
		pallium.translateAndRotate(poseStack);
		child.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
		poseStack.popPose();
	}
}
