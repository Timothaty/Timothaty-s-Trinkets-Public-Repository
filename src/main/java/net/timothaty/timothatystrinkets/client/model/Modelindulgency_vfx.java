package net.timothaty.timothatystrinkets.client.model;

import net.minecraft.world.entity.Entity;
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

public class Modelindulgency_vfx<T extends Entity> extends EntityModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "modelindulgency_vfx"), "main");
	public final ModelPart indulgency_vfx;

	public Modelindulgency_vfx(ModelPart root) {
		this.indulgency_vfx = root.getChild("indulgency_vfx");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition indulgency_vfx = partdefinition.addOrReplaceChild("indulgency_vfx",
				CubeListBuilder.create().texOffs(0, 128).addBox(-15.0F, -32.0F, -1.0F, 16.0F, 32.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(0, 128).addBox(-15.0F, -32.0F, 15.0F, 16.0F, 32.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offset(7.0F, 24.0F, -7.0F));
		PartDefinition cube_r1 = indulgency_vfx.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 128).addBox(-1.0F, -32.0F, -1.0F, 16.0F, 32.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-16.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));
		PartDefinition cube_r2 = indulgency_vfx.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 128).addBox(-1.0F, -32.0F, -1.0F, 16.0F, 32.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));
		return LayerDefinition.create(meshdefinition, 16, 160);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int rgb) {
		indulgency_vfx.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}
}