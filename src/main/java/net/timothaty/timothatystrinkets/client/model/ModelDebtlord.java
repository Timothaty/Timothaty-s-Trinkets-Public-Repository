package net.timothaty.timothatystrinkets.client.model;

import net.timothaty.timothatystrinkets.client.model.animations.DebtlordAnimation;
import org.joml.Vector4f;
import org.joml.Vector3f;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.animation.AnimationDefinition;

import net.timothaty.timothatystrinkets.entity.DebtlordEntity;

import java.util.List;
import java.util.ArrayList;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

public class ModelDebtlord<T extends Entity> extends HierarchicalModel<T> {
	private static final Vector3f ANIMATION_VECTOR_CACHE = new Vector3f();
	private boolean suppressDeathOverlay;
	private float renderAlpha = 1.0F;
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "model_debtlord"), "main");
	public final ModelPart root;
	public final ModelPart allBody;
	public final ModelPart head;
	public final ModelPart body;
	public final ModelPart body2;
	public final ModelPart pauldronLeft;
	public final ModelPart pauldronRight;
	public final ModelPart cloth;
	public final ModelPart rightArm;
	public final ModelPart finger4;
	public final ModelPart finger3;
	public final ModelPart finger2;
	public final ModelPart finger1;
	public final ModelPart leftArm;
	public final ModelPart finger5;
	public final ModelPart finger6;
	public final ModelPart finger7;
	public final ModelPart finger8;
	public final ModelPart Cape;
	public final ModelPart cape2;
	public final ModelPart leftHoof;
	public final ModelPart rightHoof;

	public ModelDebtlord(ModelPart root) {
		this.root = root;
		this.allBody = root.getChild("allBody");
		this.head = this.allBody.getChild("head");
		this.body = this.allBody.getChild("body");
		this.body2 = this.body.getChild("body2");
		this.pauldronLeft = this.body.getChild("pauldronLeft");
		this.pauldronRight = this.body.getChild("pauldronRight");
		this.cloth = this.body.getChild("cloth");
		this.rightArm = this.body.getChild("rightArm");
		this.finger4 = this.rightArm.getChild("finger4");
		this.finger3 = this.rightArm.getChild("finger3");
		this.finger2 = this.rightArm.getChild("finger2");
		this.finger1 = this.rightArm.getChild("finger1");
		this.leftArm = this.body.getChild("leftArm");
		this.finger5 = this.leftArm.getChild("finger5");
		this.finger6 = this.leftArm.getChild("finger6");
		this.finger7 = this.leftArm.getChild("finger7");
		this.finger8 = this.leftArm.getChild("finger8");
		this.Cape = this.body.getChild("Cape");
		this.cape2 = this.Cape.getChild("cape2");
		this.leftHoof = this.allBody.getChild("leftHoof");
		this.rightHoof = this.allBody.getChild("rightHoof");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition allBody = partdefinition.addOrReplaceChild("allBody", CubeListBuilder.create(), PartPose.offset(-4.0F, -15.5F, -2.0F));
		PartDefinition head = allBody.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 18).addBox(-4.5F, -9.159F, -4.4433F, 9.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(3.5F, 0.159F, 2.4433F));
		PartDefinition cube_r1 = head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(96, 64).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-5.3F, -14.159F, -2.0433F, -0.4796F, -0.0201F, 1.0085F));
		PartDefinition cube_r2 = head.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(92, 74).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-5.1F, -10.159F, -2.9433F, 0.0F, -0.48F, -0.5236F));
		PartDefinition cube_r3 = head.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(80, 88).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-3.7F, -10.159F, -5.4433F, 0.5688F, 0.1557F, 0.0207F));
		PartDefinition cube_r4 = head.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(64, 96).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-3.6F, -14.559F, -5.7433F, -0.4363F, 0.1571F, 0.0F));
		PartDefinition cube_r5 = head.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(96, 24).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-3.5017F, -15.6779F, -4.991F, -1.7453F, 0.1571F, 0.0F));
		PartDefinition cube_r6 = head.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(92, 82).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(3.5017F, -15.6779F, -4.991F, -1.7453F, -0.1571F, 0.0F));
		PartDefinition cube_r7 = head.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(96, 94).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(5.3F, -14.159F, -2.0433F, -0.4796F, 0.0201F, -1.0085F));
		PartDefinition cube_r8 = head.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(96, 57).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(3.6F, -14.559F, -5.7433F, -0.4363F, -0.1571F, 0.0F));
		PartDefinition cube_r9 = head.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(72, 88).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(3.7F, -10.159F, -5.4433F, 0.5688F, -0.1557F, -0.0207F));
		PartDefinition cube_r10 = head.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(88, 94).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(5.1F, -10.159F, -2.9433F, 0.0F, 0.48F, 0.5236F));
		PartDefinition cube_r11 = head.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(24, 80).addBox(0.0F, -3.5F, 0.0F, 2.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(4.5F, -4.659F, 0.5567F, 0.0F, -0.8727F, 0.0F));
		PartDefinition cube_r12 = head.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(24, 75).addBox(-2.0F, -3.5F, 0.0F, 2.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-4.5F, -4.659F, 0.5567F, 0.0F, 0.8727F, 0.0F));
		PartDefinition body = allBody.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 0).addBox(-6.1875F, -19.8572F, -3.26F, 13.0F, 11.0F, 7.0F, new CubeDeformation(0.0F)).texOffs(28, 99).addBox(-1.1875F, -3.8529F, -3.29F, 3.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offset(3.1875F, 19.9572F, 2.26F));
		PartDefinition cube_r13 = body.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(52, 31).addBox(-2.5F, -2.0F, -0.5F, 5.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-3.5875F, -0.5869F, -3.29F, 0.5236F, 0.0F, 0.0F));
		PartDefinition cube_r14 = body.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(88, 41).addBox(-2.5F, -2.0F, -0.5F, 5.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(4.2125F, -0.5869F, -3.19F, 0.5236F, 0.0F, 0.0F));
		PartDefinition body2 = body.addOrReplaceChild("body2", CubeListBuilder.create().texOffs(40, 0).addBox(2.0F, -9.0F, 0.0F, 11.0F, 10.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(-7.1875F, 0.1428F, -2.26F));
		PartDefinition pauldronLeft = body.addOrReplaceChild("pauldronLeft", CubeListBuilder.create().texOffs(92, 71).addBox(-3.3926F, 3.0957F, -3.65F, 7.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(36, 18).mirror()
				.addBox(-3.3926F, 0.0957F, -5.05F, 8.0F, 3.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(10.2051F, -19.9529F, -0.21F));
		PartDefinition cube_r15 = pauldronLeft.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(64, 31).mirror().addBox(-1.0F, -2.5F, -5.0F, 2.0F, 5.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(-3.4221F, 0.4128F, 0.05F, 0.0F, 0.0F, -0.2618F));
		PartDefinition cube_r16 = pauldronLeft.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(72, 15).mirror().addBox(-1.0F, -2.0F, -5.0F, 2.0F, 4.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(-2.3926F, -2.4043F, 0.05F, 0.0F, 0.0F, 0.829F));
		PartDefinition cube_r17 = pauldronLeft.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(88, 91).addBox(-3.5F, -1.5F, 0.0F, 7.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.1074F, 4.5957F, 4.35F, 0.0F, 3.1416F, 0.0F));
		PartDefinition cube_r18 = pauldronLeft.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(40, 15).addBox(-3.5F, -1.0F, 0.0F, 8.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(3.6074F, 4.0957F, -0.15F, 0.0F, -1.5708F, 0.0F));
		PartDefinition pauldronRight = body.addOrReplaceChild("pauldronRight",
				CubeListBuilder.create().texOffs(36, 18).addBox(-4.6074F, 0.0957F, -5.05F, 8.0F, 3.0F, 10.0F, new CubeDeformation(0.0F)).texOffs(56, 15).addBox(-3.6074F, 3.0957F, -3.65F, 7.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-9.5801F, -19.9529F, -0.21F));
		PartDefinition cube_r19 = pauldronRight.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(88, 88).addBox(-3.5F, -1.5F, 0.0F, 7.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.1074F, 4.5957F, 4.35F, 0.0F, 3.1416F, 0.0F));
		PartDefinition cube_r20 = pauldronRight.addOrReplaceChild("cube_r20", CubeListBuilder.create().texOffs(36, 31).addBox(-4.5F, -1.0F, 0.0F, 8.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-3.6074F, 4.0957F, -0.15F, 0.0F, 1.5708F, 0.0F));
		PartDefinition cube_r21 = pauldronRight.addOrReplaceChild("cube_r21", CubeListBuilder.create().texOffs(64, 31).addBox(-1.0F, -2.5F, -5.0F, 2.0F, 5.0F, 10.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(3.4221F, 0.4128F, 0.05F, 0.0F, 0.0F, 0.2618F));
		PartDefinition cube_r22 = pauldronRight.addOrReplaceChild("cube_r22", CubeListBuilder.create().texOffs(72, 15).addBox(-1.0F, -2.0F, -5.0F, 2.0F, 4.0F, 10.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(2.3926F, -2.4043F, 0.05F, 0.0F, 0.0F, -0.829F));
		PartDefinition cloth = body.addOrReplaceChild("cloth", CubeListBuilder.create().texOffs(72, 97).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 9.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.3125F, 0.1471F, -2.79F));
		PartDefinition rightArm = body.addOrReplaceChild("rightArm", CubeListBuilder.create().texOffs(28, 49).addBox(-3.2F, -0.2F, -3.1F, 6.0F, 18.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(-8.9875F, -17.6572F, 0.24F));
		PartDefinition finger4 = rightArm.addOrReplaceChild("finger4", CubeListBuilder.create(), PartPose.offset(-2.2F, 17.8F, 1.9F));
		PartDefinition finger4_r1 = finger4.addOrReplaceChild("finger4_r1", CubeListBuilder.create().texOffs(48, 96).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.1F, 0.0F, 0.0F, -0.2618F));
		PartDefinition finger3 = rightArm.addOrReplaceChild("finger3", CubeListBuilder.create(), PartPose.offset(-2.2F, 17.8F, -0.1F));
		PartDefinition finger3_r1 = finger3.addOrReplaceChild("finger3_r1", CubeListBuilder.create().texOffs(40, 96).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3054F));
		PartDefinition finger2 = rightArm.addOrReplaceChild("finger2", CubeListBuilder.create(), PartPose.offset(-2.2F, 17.8F, -2.1F));
		PartDefinition finger2_r1 = finger2.addOrReplaceChild("finger2_r1", CubeListBuilder.create().texOffs(56, 96).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -0.4363F));
		PartDefinition finger1 = rightArm.addOrReplaceChild("finger1", CubeListBuilder.create().texOffs(78, 97).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(1.8F, 17.8F, -2.1F));
		PartDefinition leftArm = body.addOrReplaceChild("leftArm", CubeListBuilder.create().texOffs(52, 49).addBox(-2.8F, -0.2F, -2.6F, 6.0F, 18.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(9.6125F, -17.6572F, -0.26F));
		PartDefinition finger5 = leftArm.addOrReplaceChild("finger5", CubeListBuilder.create(), PartPose.offset(2.2F, 17.8F, 2.4F));
		PartDefinition finger4_r2 = finger5.addOrReplaceChild("finger4_r2", CubeListBuilder.create().texOffs(96, 16).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.2618F));
		PartDefinition finger6 = leftArm.addOrReplaceChild("finger6", CubeListBuilder.create(), PartPose.offset(2.2F, 17.8F, 0.4F));
		PartDefinition finger3_r2 = finger6.addOrReplaceChild("finger3_r2", CubeListBuilder.create().texOffs(96, 8).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3054F));
		PartDefinition finger7 = leftArm.addOrReplaceChild("finger7", CubeListBuilder.create(), PartPose.offset(2.2F, 17.8F, -1.6F));
		PartDefinition finger2_r2 = finger7.addOrReplaceChild("finger2_r2", CubeListBuilder.create().texOffs(96, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.1F, 0.0F, -0.1F, 0.0F, 0.0F, 0.4363F));
		PartDefinition finger8 = leftArm.addOrReplaceChild("finger8", CubeListBuilder.create().texOffs(20, 99).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.8F, 17.8F, -1.6F));
		PartDefinition Cape = body.addOrReplaceChild("Cape", CubeListBuilder.create(), PartPose.offsetAndRotation(0.3125F, -19.7572F, 5.89F, 3.0107F, 0.0F, 3.1416F));
		PartDefinition largecape_r1 = Cape.addOrReplaceChild("largecape_r1", CubeListBuilder.create().texOffs(0, 36).addBox(-7.0F, 0.0377F, -0.1368F, 14.0F, 26.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.1F, -0.3925F, 1.3868F, 0.0873F, 0.0F, 0.0F));
		PartDefinition cape2 = Cape.addOrReplaceChild("cape2", CubeListBuilder.create(), PartPose.offset(-0.4F, 25.5075F, 3.4868F));
		PartDefinition smallcape_r1 = cape2.addOrReplaceChild("smallcape_r1", CubeListBuilder.create().texOffs(0, 62).addBox(-6.5F, 0.0F, 0.0F, 14.0F, 13.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1309F, 0.0F, 0.0F));
		PartDefinition leftHoof = allBody.addOrReplaceChild("leftHoof", CubeListBuilder.create().texOffs(76, 46).addBox(-3.0F, 15.455F, -5.7287F, 6.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(7.5F, 19.0296F, 3.484F));
		PartDefinition cube_r23 = leftHoof.addOrReplaceChild("cube_r23", CubeListBuilder.create().texOffs(88, 29).addBox(-2.5F, -3.5F, -3.0F, 5.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.1F, 8.8312F, -1.4699F, 0.6545F, 0.0F, 0.0F));
		PartDefinition cube_r24 = leftHoof.addOrReplaceChild("cube_r24", CubeListBuilder.create().texOffs(72, 73).addBox(-2.5F, -7.0F, -3.0F, 5.0F, 10.0F, 5.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 6.0549F, -2.3287F, -0.3927F, 0.0F, 0.0F));
		PartDefinition cube_r25 = leftHoof.addOrReplaceChild("cube_r25", CubeListBuilder.create().texOffs(0, 86).addBox(-2.5F, -6.0F, -3.0F, 5.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.1F, 14.455F, -1.6287F, -0.48F, 0.0F, 0.0F));
		PartDefinition rightHoof = allBody.addOrReplaceChild("rightHoof", CubeListBuilder.create().texOffs(0, 75).addBox(-4.0F, 15.455F, -5.7287F, 6.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 19.0296F, 3.4841F));
		PartDefinition cube_r26 = rightHoof.addOrReplaceChild("cube_r26", CubeListBuilder.create().texOffs(76, 57).addBox(-2.5F, -6.0F, -3.0F, 5.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-1.1F, 14.455F, -1.6287F, -0.48F, 0.0F, 0.0F));
		PartDefinition cube_r27 = rightHoof.addOrReplaceChild("cube_r27", CubeListBuilder.create().texOffs(52, 73).addBox(-2.5F, -7.0F, -3.0F, 5.0F, 10.0F, 5.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-1.0F, 6.055F, -2.3287F, -0.3927F, 0.0F, 0.0F));
		PartDefinition cube_r28 = rightHoof.addOrReplaceChild("cube_r28", CubeListBuilder.create().texOffs(20, 87).addBox(-2.5F, -3.5F, -3.0F, 5.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.9F, 8.8313F, -1.4699F, 0.6545F, 0.0F, 0.0F));
		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	public ModelPart root() {
		return this.root;
	}

	public List<Vector3f> getDeathBloodRenderPositions(PoseStack poseStack) {
		List<Vector3f> positions = new ArrayList<>();
		addBonePosition(poseStack, positions, new ModelPart[]{this.allBody, this.head}, 0.0F, -4.0F, 0.0F);
		addBonePosition(poseStack, positions, new ModelPart[]{this.allBody, this.body}, 0.0F, -10.0F, 0.0F);
		addBonePosition(poseStack, positions, new ModelPart[]{this.allBody, this.body, this.rightArm}, 0.0F, 10.0F, 0.0F);
		addBonePosition(poseStack, positions, new ModelPart[]{this.allBody, this.body, this.leftArm}, 0.0F, 10.0F, 0.0F);
		addBonePosition(poseStack, positions, new ModelPart[]{this.allBody, this.leftHoof}, 0.0F, 17.0F, -2.0F);
		addBonePosition(poseStack, positions, new ModelPart[]{this.allBody, this.rightHoof}, -1.0F, 17.0F, -2.0F);
		return positions;
	}

	public List<Vector3f> getClawFingerRenderPositions(PoseStack poseStack) {
		List<Vector3f> positions = new ArrayList<>();
		addBonePosition(poseStack, positions, new ModelPart[]{this.allBody, this.body, this.rightArm, this.finger1}, 0.0F, 4.5F, 0.0F);
		addBonePosition(poseStack, positions, new ModelPart[]{this.allBody, this.body, this.rightArm, this.finger2}, 0.0F, 4.5F, 0.0F);
		addBonePosition(poseStack, positions, new ModelPart[]{this.allBody, this.body, this.rightArm, this.finger3}, 0.0F, 4.5F, 0.0F);
		addBonePosition(poseStack, positions, new ModelPart[]{this.allBody, this.body, this.rightArm, this.finger4}, 0.0F, 4.5F, 0.0F);
		addBonePosition(poseStack, positions, new ModelPart[]{this.allBody, this.body, this.leftArm, this.finger5}, 0.0F, 4.5F, 0.0F);
		addBonePosition(poseStack, positions, new ModelPart[]{this.allBody, this.body, this.leftArm, this.finger6}, 0.0F, 4.5F, 0.0F);
		addBonePosition(poseStack, positions, new ModelPart[]{this.allBody, this.body, this.leftArm, this.finger7}, 0.0F, 4.5F, 0.0F);
		addBonePosition(poseStack, positions, new ModelPart[]{this.allBody, this.body, this.leftArm, this.finger8}, 0.0F, 4.5F, 0.0F);
		return positions;
	}

	public List<Vector3f> getChainSourceRenderPositions(PoseStack poseStack) {
		List<Vector3f> positions = new ArrayList<>();
		addBonePosition(poseStack, positions, new ModelPart[]{this.allBody, this.body, this.leftArm, this.finger5}, 0.0F, 4.5F, 0.0F);
		addBonePosition(poseStack, positions, new ModelPart[]{this.allBody, this.body, this.leftArm, this.finger8}, 0.0F, 4.5F, 0.0F);
		return positions;
	}

	public List<Vector3f> getFingerOfDeathLaserRenderPositions(PoseStack poseStack) {
		List<Vector3f> positions = new ArrayList<>();
		ModelPart finger7Tip = this.finger7.getChild("finger2_r2");
		addBonePosition(poseStack, positions, new ModelPart[]{this.allBody, this.body, this.leftArm, this.finger7, finger7Tip}, 0.0F, 3.0F, 0.0F);
		addBonePosition(poseStack, positions, new ModelPart[]{this.allBody, this.body, this.leftArm, this.finger7, finger7Tip}, 0.0F, 5.2F, 0.0F);
		return positions;
	}

	private static void addBonePosition(PoseStack poseStack, List<Vector3f> positions, ModelPart[] path, float localX, float localY, float localZ) {
		poseStack.pushPose();
		for (ModelPart part : path) {
			part.translateAndRotate(poseStack);
		}
		Vector4f transformed = poseStack.last().pose().transform(new Vector4f(localX / 16.0F, localY / 16.0F, localZ / 16.0F, 1.0F));
		positions.add(new Vector3f(transformed.x, transformed.y, transformed.z));
		poseStack.popPose();
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		this.suppressDeathOverlay = false;
		this.renderAlpha = 1.0F;
		if (entity instanceof DebtlordEntity debtlord) {
			this.suppressDeathOverlay = debtlord.isDeathAnimationActive();
			float partialTick = Mth.clamp(ageInTicks - (float) debtlord.tickCount, 0.0F, 1.0F);
			this.renderAlpha = debtlord.getRenderAlpha(partialTick);
			if (debtlord.isDeathAnimationActive()) {
				this.animate(debtlord.deathAnimationState, DebtlordAnimation.DEATH, ageInTicks);
				return;
			}
			if (debtlord.isAltarIntroOrDismissalActive()) {
				this.animateAltarIntro(debtlord, ageInTicks, partialTick);
				this.applyHeadLook(netHeadYaw, headPitch);
				return;
			}
			if (debtlord.isFingerOfDeathChargeAnimationActive()) {
				this.animate(debtlord.fingerOfDeathChargeAnimationState, DebtlordAnimation.FINGER_OF_DEATH_CHARGE, ageInTicks);
				this.applyHeadLook(netHeadYaw, headPitch);
				return;
			}
			if (debtlord.isFingerOfDeathIdleAnimationActive()) {
				this.animate(debtlord.fingerOfDeathIdleAnimationState, DebtlordAnimation.FINGER_OF_DEATH_POSE_IDLE, ageInTicks);
				this.applyHeadLook(netHeadYaw, headPitch);
				return;
			}
			if (debtlord.isFingerOfDeathShotAnimationActive()) {
				this.animate(debtlord.fingerOfDeathShotAnimationState, DebtlordAnimation.FINGER_OF_DEATH_SHOT, ageInTicks);
				this.applyHeadLook(netHeadYaw, headPitch);
				return;
			}
			if (debtlord.isChainSuccessAnimationActive()) {
				this.animate(debtlord.chainSuccessAnimationState, DebtlordAnimation.CHAINED_SUCCESFUL, ageInTicks);
				this.applyHeadLook(netHeadYaw, headPitch);
				return;
			}
			if (debtlord.isChainFailedAnimationActive()) {
				this.animate(debtlord.chainFailedAnimationState, DebtlordAnimation.CHAINED_FAILED, ageInTicks);
				this.applyHeadLook(netHeadYaw, headPitch);
				return;
			}
			if (debtlord.isChainCastAnimationActive()) {
				this.animate(debtlord.chainAnimationState, DebtlordAnimation.CHAINED, ageInTicks);
				this.applyHeadLook(netHeadYaw, headPitch);
				return;
			}
			if (debtlord.isDesolationAnimationActive()) {
				this.animate(debtlord.desolationAnimationState, DebtlordAnimation.CAST_DESOLATION, ageInTicks);
				return;
			}
			if (debtlord.isFearAnimationActive()) {
				this.animate(debtlord.fearAnimationState, DebtlordAnimation.FEAR, ageInTicks);
				this.applyHeadLook(netHeadYaw, headPitch);
				return;
			}
			if (debtlord.isStompAnimationActive()) {
				this.animate(debtlord.stompAnimationState, DebtlordAnimation.STOMP, ageInTicks, debtlord.getCurrentStompAnimationSpeed());
				return;
			}
			if (debtlord.isClawAnimationActive()) {
				this.animate(debtlord.clawAnimationState, DebtlordAnimation.FEAR_MY_CLAWS, ageInTicks, debtlord.getClawAnimationSpeed());
				this.applyHeadLook(netHeadYaw, headPitch);
				return;
			}
			if (debtlord.isDisarmAnimationActive()) {
				this.animate(debtlord.disarmAnimationState, DebtlordAnimation.REMOVE_WEAPON, ageInTicks);
				return;
			}
			if (debtlord.isHornsAnimationActive()) {
				this.animate(debtlord.hornsAnimationState, DebtlordAnimation.HORNS, ageInTicks);
				return;
			}
			float walkWeight = debtlord.getMovementAnimationBlend(partialTick);
			this.animateBlended(debtlord.idleAnimationState, DebtlordAnimation.IDLE, ageInTicks, 1.0F - walkWeight);
			this.animateBlended(debtlord.walkAnimationState, DebtlordAnimation.WALK, ageInTicks, walkWeight);
			this.applyHeadLook(netHeadYaw, headPitch);
		}
	}

	private void applyHeadLook(float netHeadYaw, float headPitch) {
		this.head.yRot = this.head.yRot + Mth.clamp(netHeadYaw, -45.0F, 45.0F) * (float) (Math.PI / 180.0);
		this.head.xRot = this.head.xRot + Mth.clamp(headPitch, -30.0F, 30.0F) * (float) (Math.PI / 180.0);
	}

	private void animateAltarIntro(DebtlordEntity debtlord, float ageInTicks, float partialTick) {
		float appearanceWeight = debtlord.getAltarAppearanceAnimationWeight(partialTick);
		float talkingWeight = debtlord.getAltarTalkingAnimationWeight(partialTick);
		float idleWeight = debtlord.getAltarIdleAnimationWeight(partialTick);
		if (appearanceWeight > 0.001F) {
			this.animateBlended(debtlord.appearanceAnimationState, DebtlordAnimation.APPEARANCE, ageInTicks, appearanceWeight);
		}
		if (talkingWeight > 0.001F) {
			this.animateBlended(debtlord.talkingAnimationState, DebtlordAnimation.TALKING, ageInTicks, talkingWeight);
		}
		if (idleWeight > 0.001F) {
			this.animateBlended(debtlord.idleAnimationState, DebtlordAnimation.IDLE, ageInTicks, idleWeight);
		}
	}

	private void applyFingerOfDeathArmAim(DebtlordEntity debtlord) {
		float yawDelta = Mth.wrapDegrees(debtlord.getFingerOfDeathLaserYaw() - debtlord.yBodyRot);
		float pitch = debtlord.getFingerOfDeathLaserPitch();
		this.leftArm.yRot = this.leftArm.yRot + Mth.clamp(yawDelta, -55.0F, 55.0F) * (float) (Math.PI / 180.0) * 0.65F;
		this.leftArm.xRot = this.leftArm.xRot + Mth.clamp(pitch, -55.0F, 45.0F) * (float) (Math.PI / 180.0) * 0.55F;
	}

	private void animateBlended(AnimationState state, AnimationDefinition animation, float ageInTicks, float weight) {
		state.updateTime(ageInTicks, 1.0F);
		state.ifStarted(startedState -> KeyframeAnimations.animate(this, animation, startedState.getAccumulatedTime(), weight, ANIMATION_VECTOR_CACHE));
	}

	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int rgb) {
		int effectiveOverlay = this.suppressDeathOverlay ? OverlayTexture.NO_OVERLAY : packedOverlay;
		int effectiveRgb = rgb;
		if (this.renderAlpha < 0.999F) {
			int alpha = Mth.clamp(Math.round((float) (rgb >>> 24 & 0xFF) * this.renderAlpha), 0, 255);
			effectiveRgb = rgb & 16777215 | alpha << 24;
		}
		this.allBody.render(poseStack, vertexConsumer, packedLight, effectiveOverlay, effectiveRgb);
	}
}
