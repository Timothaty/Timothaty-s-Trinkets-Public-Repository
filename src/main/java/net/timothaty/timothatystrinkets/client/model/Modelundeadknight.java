package net.timothaty.timothatystrinkets.client.model;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.EntityModel;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

public class Modelundeadknight<T extends LivingEntity> extends EntityModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "modelundeadknight"), "main");
	public final ModelPart allbod;
	public final ModelPart Waist;
	public final ModelPart Head;
	public final ModelPart Body;
	public final ModelPart RightArm;
	public final ModelPart LeftArm;
	public final ModelPart RightLeg;
	public final ModelPart LeftLeg;

	public Modelundeadknight(ModelPart root) {
		this.allbod = root.getChild("allbod");
		this.Waist = this.allbod.getChild("Waist");
		this.Head = this.Waist.getChild("Head");
		this.Body = this.Waist.getChild("Body");
		this.RightArm = this.Waist.getChild("RightArm");
		this.LeftArm = this.Waist.getChild("LeftArm");
		this.RightLeg = this.allbod.getChild("RightLeg");
		this.LeftLeg = this.allbod.getChild("LeftLeg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition allbod = partdefinition.addOrReplaceChild("allbod", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
		PartDefinition Waist = allbod.addOrReplaceChild("Waist", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition Head = Waist.addOrReplaceChild("Head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)).texOffs(0, 16).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)),
				PartPose.offset(0.0F, -12.0F, 0.0F));
		PartDefinition Body = Waist.addOrReplaceChild("Body",
				CubeListBuilder.create().texOffs(0, 32).addBox(-4.0F, -12.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(32, 0).addBox(-4.0F, -12.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition RightArm = Waist.addOrReplaceChild("RightArm",
				CubeListBuilder.create().texOffs(32, 16).addBox(-4.3152F, -1.1588F, -2.0008F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(56, 0).addBox(-5.0152F, -2.1588F, -2.5008F, 5.0F, 3.0F, 5.0F, new CubeDeformation(0.0F))
						.texOffs(0, 94).addBox(-5.1152F, 3.5412F, -1.5008F, 1.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(116, 124).mirror().addBox(-4.5152F, 0.8412F, -2.3008F, 4.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offset(-3.7F, -11.0F, 0.0F));
		PartDefinition cube_r1 = RightArm.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(116, 124).addBox(-2.0F, -2.0F, -1.0F, 4.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-3.5152F, 2.8412F, -0.0008F, 0.0F, 1.5708F, 0.0F));
		PartDefinition cube_r2 = RightArm.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(116, 124).mirror().addBox(-2.0F, -2.0F, -1.0F, 4.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(-2.5152F, 2.8412F, 1.1992F, 0.0F, 3.1416F, 0.0F));
		PartDefinition cube_r3 = RightArm.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(8, 94).addBox(-0.5F, -1.0F, -1.5F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-5.0152F, 2.7412F, -0.0008F, 0.0F, 0.0F, -0.5236F));
		PartDefinition cube_r4 = RightArm.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(56, 32).addBox(-2.5F, -0.5F, -2.5F, 5.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-2.5152F, -2.6588F, -0.1008F, 0.0F, 0.0F, 0.3927F));
		PartDefinition LeftArm = Waist
				.addOrReplaceChild(
						"LeftArm", CubeListBuilder.create().texOffs(40, 32).addBox(0.3F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(56, 0).mirror().addBox(0.0F, -1.0F, -2.5F, 5.0F, 3.0F, 5.0F, new CubeDeformation(0.0F))
								.mirror(false).texOffs(0, 103).addBox(0.3F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(116, 124).addBox(0.5F, 2.0F, -2.3F, 4.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)),
						PartPose.offset(3.7F, -12.0F, 0.0F));
		PartDefinition cube_r5 = LeftArm.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(116, 124).addBox(-2.0F, -2.0F, -1.0F, 4.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(2.5F, 4.0F, 1.2F, 0.0F, 3.1416F, 0.0F));
		PartDefinition cube_r6 = LeftArm.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(116, 124).addBox(-2.0F, -2.0F, -1.0F, 4.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(3.5F, 4.0F, 0.0F, 0.0F, -1.5708F, 0.0F));
		PartDefinition cube_r7 = LeftArm.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(56, 32).mirror().addBox(-2.5F, -0.5F, -2.5F, 5.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(2.5F, -1.5F, -0.1F, 0.0F, 0.0F, -0.3927F));
		PartDefinition RightLeg = allbod.addOrReplaceChild("RightLeg",
				CubeListBuilder.create().texOffs(16, 48).addBox(-2.05F, 0.0F, -1.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(1, 1).addBox(-1.45F, 2.0F, -1.5F, 3.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-1.85F, 0.0F, -1.0F));
		PartDefinition LeftLeg = allbod.addOrReplaceChild("LeftLeg",
				CubeListBuilder.create().texOffs(32, 48).addBox(-1.95F, 0.0F, -1.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(1, 1).mirror().addBox(-1.55F, 2.0F, -1.5F, 3.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offset(1.85F, 0.0F, -1.0F));
		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int rgb) {
		allbod.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
	}
}
