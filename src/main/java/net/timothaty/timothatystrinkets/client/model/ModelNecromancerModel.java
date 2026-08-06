package net.timothaty.timothatystrinkets.client.model;

import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.HeadedModel;
import net.timothaty.timothatystrinkets.client.model.animations.NecromancerAnimations;
import net.timothaty.timothatystrinkets.entity.NecromancerEntity;

public class ModelNecromancerModel<T extends Entity> extends HierarchicalModel<T> implements HeadedModel {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "model_necromancer_model"), "main");
	public final ModelPart all;
	public final ModelPart Waist;
	public final ModelPart Head;
	public final ModelPart Body;
	public final ModelPart cape;
	public final ModelPart hvostiks;
	public final ModelPart smallhvostiks;
	public final ModelPart plate1;
	public final ModelPart plate2;
	public final ModelPart RightArm;
	public final ModelPart LeftArm;
	public final ModelPart RightLeg;
	public final ModelPart LeftLeg;

	public ModelNecromancerModel(ModelPart root) {
		this.all = root.getChild("all");
		this.Waist = this.all.getChild("Waist");
		this.Head = this.Waist.getChild("Head");
		this.Body = this.Waist.getChild("Body");
		this.cape = this.Body.getChild("cape");
		this.hvostiks = this.cape.getChild("hvostiks");
		this.smallhvostiks = this.hvostiks.getChild("smallhvostiks");
		this.plate1 = this.Body.getChild("plate1");
		this.plate2 = this.Body.getChild("plate2");
		this.RightArm = this.Waist.getChild("RightArm");
		this.LeftArm = this.Waist.getChild("LeftArm");
		this.RightLeg = this.all.getChild("RightLeg");
		this.LeftLeg = this.all.getChild("LeftLeg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition all = partdefinition.addOrReplaceChild("all", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
		PartDefinition Waist = all.addOrReplaceChild("Waist", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition Head = Waist.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.125F, -4.5F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)).texOffs(0, 16)
				.addBox(-4.0F, -8.125F, -4.5F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)).texOffs(32, 0).addBox(0.0F, -13.625F, -4.0F, 0.0F, 5.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -11.875F, 0.5F));
		PartDefinition cube_r1 = Head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(58, 0).addBox(0.0F, -2.5F, -4.0F, 0.0F, 5.0F, 8.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, -4.625F, 6.5F, -1.5708F, 0.0F, 0.0F));
		PartDefinition Body = Waist
				.addOrReplaceChild(
						"Body", CubeListBuilder.create().texOffs(32, 18).addBox(-9.0F, -12.1F, -3.7933F, 18.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(0, 44).addBox(4.0F, -13.1F, -3.8933F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
								.texOffs(36, 32).addBox(-4.0F, -12.0F, -2.6933F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(24, 44).addBox(6.0F, -17.0F, -0.8933F, 2.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)),
						PartPose.offset(0.0F, 0.0F, 0.6933F));
		PartDefinition cube_r2 = Body.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(32, 44).addBox(-1.0F, -3.0F, 0.0F, 2.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(10.6F, -11.9F, -0.8933F, 0.0F, 0.0F, 1.2217F));
		PartDefinition cube_r3 = Body.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(28, 44).addBox(-1.0F, -3.0F, 0.0F, 2.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(9.4F, -13.6F, -0.8933F, 0.0F, 0.0F, 0.4363F));
		PartDefinition cube_r4 = Body.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(32, 26).addBox(-9.0F, -4.0F, 0.0F, 18.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, -12.1F, 0.2067F, 1.5708F, 0.0F, 0.0F));
		PartDefinition cube_r5 = Body.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(60, 32).addBox(-6.0F, -4.0F, 0.0F, 6.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(9.0F, -8.1F, -3.7933F, 0.0F, 1.5708F, 0.0F));
		PartDefinition cube_r6 = Body.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(60, 40).addBox(-6.0F, -4.0F, 0.0F, 6.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-9.0F, -8.1F, -3.7933F, 0.0F, 1.5708F, 0.0F));
		PartDefinition cape = Body.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(0, 32).addBox(-9.0F, 0.0F, 0.0F, 18.0F, 12.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.1F, 2.2067F));
		PartDefinition hvostiks = cape.addOrReplaceChild("hvostiks", CubeListBuilder.create().texOffs(16, 56).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 9.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(32, 64)
				.addBox(-9.0F, 0.0F, 0.0F, 4.0F, 9.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(24, 64).addBox(-16.0F, 0.0F, 0.0F, 4.0F, 9.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(7.0F, 12.0F, 0.0F));
		PartDefinition smallhvostiks = hvostiks.addOrReplaceChild("smallhvostiks", CubeListBuilder.create(), PartPose.offset(0.0F, 9.0F, 0.0F));
		PartDefinition cube_r7 = smallhvostiks.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(58, 13).addBox(-2.0F, 0.0506F, 0.0707F, 4.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(58, 13)
				.addBox(-9.0F, 0.0506F, 0.0707F, 4.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(58, 13).addBox(5.0F, 0.0506F, 0.0707F, 4.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-7.0F, 0.0F, -0.1F, 0.7854F, 0.0F, 0.0F));
		PartDefinition plate1 = Body.addOrReplaceChild("plate1", CubeListBuilder.create(), PartPose.offset(-4.5F, 0.1F, -0.6933F));
		PartDefinition cube_r8 = plate1.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(0, 76).addBox(-4.7214F, -1.2556F, -2.0F, 5.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.5272F));
		PartDefinition plate2 = Body.addOrReplaceChild("plate2", CubeListBuilder.create(), PartPose.offsetAndRotation(4.5F, 0.1F, -0.6933F, 0.0F, 0.0F, -0.0873F));
		PartDefinition cube_r9 = plate2.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(0, 76).mirror().addBox(-0.2786F, -1.2556F, -2.0F, 5.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.6144F));
		PartDefinition RightArm = Waist.addOrReplaceChild("RightArm", CubeListBuilder.create().texOffs(24, 48).addBox(-1.9836F, -2.0F, -1.5909F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-6.0F, -9.6741F, -0.3096F));
		PartDefinition LeftArm = Waist.addOrReplaceChild("LeftArm", CubeListBuilder.create().texOffs(40, 48).addBox(-2.0F, -1.8113F, -2.0689F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(6.0F, -9.6741F, 0.0995F));
		PartDefinition RightLeg = all.addOrReplaceChild("RightLeg", CubeListBuilder.create().texOffs(0, 56).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.9F, 0.0F, 0.0F));
		PartDefinition LeftLeg = all.addOrReplaceChild("LeftLeg", CubeListBuilder.create().texOffs(56, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(1.9F, 0.0F, 0.0F));
		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public ModelPart root() {
		return this.all;
	}

	@Override
	public ModelPart getHead() {
		return this.Head;
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		this.Head.yRot = netHeadYaw * ((float) Math.PI / 180F);
		this.Head.xRot = headPitch * ((float) Math.PI / 180F);
		float walkSpeed = 1.0F;
		float walkDegree = 1.0F;
		this.RightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount * walkDegree;
		this.LeftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount * walkDegree;
		this.RightArm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.2F * limbSwingAmount * walkSpeed;
		this.LeftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 1.2F * limbSwingAmount * walkSpeed;
		if (entity instanceof NecromancerEntity necromancer) {
			this.animate(necromancer.summonStartAnimationState, NecromancerAnimations.NECRO_CAST_START, ageInTicks);
			this.animate(necromancer.summonLoopAnimationState, NecromancerAnimations.NECRO_LOOP, ageInTicks);
			this.animate(necromancer.summonEndAnimationState, NecromancerAnimations.NECRO_CAST_END, ageInTicks);
			this.animate(necromancer.undeadificationAnimationState, NecromancerAnimations.NECRO_CAST_UNDEADIFICATION, ageInTicks);
		}
	}
}
