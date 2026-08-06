package net.timothaty.timothatystrinkets.client.model.curio;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

@OnlyIn(Dist.CLIENT)
public class ChampionsGauntletModel<T extends Entity> extends EntityModel<T> implements HandCurioArmModel {
	public static final ModelLayerLocation WIDE_LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "champions_gauntlet_wide"), "main");
	public static final ModelLayerLocation SLIM_LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "champions_gauntlet_slim"), "main");
	public static final ModelLayerLocation LAYER_LOCATION = WIDE_LAYER_LOCATION;

	private static final float WIDE_RIGHT_GAUNTLET_X = -1.0F;
	private static final float WIDE_LEFT_GAUNTLET_X = 1.0F;
	private static final float SLIM_RIGHT_GAUNTLET_X = -0.5F;
	private static final float SLIM_LEFT_GAUNTLET_X = 0.5F;

	private final ModelPart right_arm;
	private final ModelPart right_gauntlet;
	private final ModelPart left_arm;
	private final ModelPart left_gauntlet;

	public ChampionsGauntletModel(ModelPart root) {
		super(RenderType::entityCutoutNoCull);
		this.right_arm = root.getChild("right_arm");
		this.right_gauntlet = this.right_arm.getChild("right_gauntlet");
		this.left_arm = root.getChild("left_arm");
		this.left_gauntlet = this.left_arm.getChild("left_gauntlet");
	}

	public static LayerDefinition createBodyLayer() {
		return createWideBodyLayer();
	}

	public static LayerDefinition createWideBodyLayer() {
		return createBodyLayer(false);
	}

	public static LayerDefinition createSlimBodyLayer() {
		return createBodyLayer(true);
	}

	private static LayerDefinition createBodyLayer(boolean slim) {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		float rightGauntletOffsetX = slim ? SLIM_RIGHT_GAUNTLET_X : WIDE_RIGHT_GAUNTLET_X;
		float leftGauntletOffsetX = slim ? SLIM_LEFT_GAUNTLET_X : WIDE_LEFT_GAUNTLET_X;

		PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));

		PartDefinition right_gauntlet = right_arm.addOrReplaceChild("right_gauntlet", CubeListBuilder.create().texOffs(0, 9).mirror()
				.addBox(-2.0F, -5.2F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.25F)).mirror(false), PartPose.offset(rightGauntletOffsetX, 10.0F, 0.0F));

		PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));

		PartDefinition left_gauntlet = left_arm.addOrReplaceChild("left_gauntlet", CubeListBuilder.create().texOffs(0, 9)
				.addBox(-2.0F, -5.2F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(leftGauntletOffsetX, 10.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 32, 32);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
		this.right_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
		this.left_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
	}

	@Override
	public void copyArmPoseFrom(HumanoidModel<?> humanoidModel) {
		copyPartPose(humanoidModel.rightArm, this.right_arm);
		copyPartPose(humanoidModel.leftArm, this.left_arm);
	}

	public void renderRightArm(PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
		this.right_arm.render(poseStack, consumer, light, overlay);
	}

	public void renderLeftArm(PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
		this.left_arm.render(poseStack, consumer, light, overlay);
	}

	@Override
	public void renderArm(HumanoidArm arm, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
		if (arm == HumanoidArm.LEFT) {
			this.renderLeftArm(poseStack, consumer, light, overlay);
		} else {
			this.renderRightArm(poseStack, consumer, light, overlay);
		}
	}

	private static void copyPartPose(ModelPart source, ModelPart target) {
		target.copyFrom(source);
	}
}
