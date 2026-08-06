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
public class DuelistsGauntletModel<T extends Entity> extends EntityModel<T> implements HandCurioArmModel {
	public static final ModelLayerLocation WIDE_LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "duelists_gauntlet_wide"), "main");
	public static final ModelLayerLocation SLIM_LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "duelists_gauntlet_slim"), "main");
	public static final ModelLayerLocation LAYER_LOCATION = WIDE_LAYER_LOCATION;

	private static final float WIDE_RIGHT_GAUNTLET_X = -1.0F;
	private static final float WIDE_LEFT_GAUNTLET_X = 1.0F;
	private static final float SLIM_RIGHT_GAUNTLET_X = -0.5F;
	private static final float SLIM_LEFT_GAUNTLET_X = 0.5F;

	private final ModelPart rightArm;
	private final ModelPart leftArm;

	public DuelistsGauntletModel(ModelPart root) {
		super(RenderType::entityCutoutNoCull);
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
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
		MeshDefinition meshDefinition = new MeshDefinition();
		PartDefinition root = meshDefinition.getRoot();

		float rightGauntletOffsetX = slim ? SLIM_RIGHT_GAUNTLET_X : WIDE_RIGHT_GAUNTLET_X;
		float leftGauntletOffsetX = slim ? SLIM_LEFT_GAUNTLET_X : WIDE_LEFT_GAUNTLET_X;

		PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
		rightArm.addOrReplaceChild("right_gauntlet", CubeListBuilder.create().texOffs(0, 3)
				.addBox(-2.0F, -7.0F, -1.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.2F)), PartPose.offset(rightGauntletOffsetX, 10.0F, -1.0F));

		PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
		leftArm.addOrReplaceChild("left_gauntlet", CubeListBuilder.create().texOffs(0, 3).mirror()
				.addBox(-2.0F, -7.0F, -1.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.2F)).mirror(false), PartPose.offset(leftGauntletOffsetX, 10.0F, -1.0F));

		return LayerDefinition.create(meshDefinition, 16, 16);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
		this.rightArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
		this.leftArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
	}

	@Override
	public void copyArmPoseFrom(HumanoidModel<?> humanoidModel) {
		copyPartPose(humanoidModel.rightArm, this.rightArm);
		copyPartPose(humanoidModel.leftArm, this.leftArm);
	}

	@Override
	public void renderArm(HumanoidArm arm, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
		if (arm == HumanoidArm.LEFT) {
			this.leftArm.render(poseStack, consumer, light, overlay);
		} else {
			this.rightArm.render(poseStack, consumer, light, overlay);
		}
	}

	private static void copyPartPose(ModelPart source, ModelPart target) {
		target.copyFrom(source);
	}
}
