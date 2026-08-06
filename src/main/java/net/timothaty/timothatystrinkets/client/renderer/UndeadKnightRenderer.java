package net.timothaty.timothatystrinkets.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import net.timothaty.timothatystrinkets.entity.UndeadKnightEntity;
import net.timothaty.timothatystrinkets.client.model.Modelundeadknight;
import net.timothaty.timothatystrinkets.client.model.animations.undeadknightAnimation;
import net.timothaty.timothatystrinkets.client.stunned.StunnedHeadAnimation;

public class UndeadKnightRenderer extends MobRenderer<UndeadKnightEntity, UndeadKnightRenderer.UndeadKnightArmedModel> {
	public UndeadKnightRenderer(EntityRendererProvider.Context context) {
		super(context, new UndeadKnightArmedModel(context.bakeLayer(Modelundeadknight.LAYER_LOCATION)), 0.5f);
		this.addLayer(new UndeadKnightGlowLayer(this));
		this.addLayer(new ItemInHandLayer<UndeadKnightEntity, UndeadKnightArmedModel>(this, context.getItemInHandRenderer()));
	}

	@Override
	public ResourceLocation getTextureLocation(UndeadKnightEntity entity) {
		return ResourceLocation.parse("timothatys_trinkets:textures/entities/" + entity.getTexture() + ".png");
	}

	@Override
	protected float getFlipDegrees(UndeadKnightEntity entity) {
		return 0.0F;
	}

	public static class UndeadKnightArmedModel extends HierarchicalModel<UndeadKnightEntity> implements ArmedModel, HeadedModel {
		private static final float DEG_TO_RAD = (float) Math.PI / 180.0F;
		public final ModelPart root;
		public final ModelPart allbod;
		public final ModelPart Waist;
		public final ModelPart Head;
		public final ModelPart Body;
		public final ModelPart RightArm;
		public final ModelPart LeftArm;
		public final ModelPart RightLeg;
		public final ModelPart LeftLeg;

		public UndeadKnightArmedModel(ModelPart root) {
			this.root = root;
			this.allbod = root.getChild("allbod");
			this.Waist = this.allbod.getChild("Waist");
			this.Head = this.Waist.getChild("Head");
			this.Body = this.Waist.getChild("Body");
			this.RightArm = this.Waist.getChild("RightArm");
			this.LeftArm = this.Waist.getChild("LeftArm");
			this.RightLeg = this.allbod.getChild("RightLeg");
			this.LeftLeg = this.allbod.getChild("LeftLeg");
		}

		@Override
		public ModelPart root() {
			return this.root;
		}

		@Override
		public void setupAnim(UndeadKnightEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
			this.root().getAllParts().forEach(ModelPart::resetPose);
			if (entity.isDeathAnimationActive()) {
				this.setupDeathAnimation(entity, ageInTicks);
				return;
			}
			if (entity.isReincarnating()) {
				this.setupReincarnationAnimation(entity, ageInTicks);
				return;
			}
			if (entity.isSoulAbsorbing()) {
				this.setupSoulAbsorptionAnimation(entity, ageInTicks);
				return;
			}
			if (entity.isBlocking()) {
				this.setupBlockAnimation(entity, ageInTicks);
				return;
			}
			if (entity.isEmpowering()) {
				this.setupEmpowerAnimation(entity, ageInTicks, netHeadYaw, headPitch);
				return;
			}

			float clampedWalk = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
			float idleWeight = 1.0F - clampedWalk;
			float walkCycle = limbSwing * 0.6662F;
			float armSwing = 1.15F * clampedWalk;
			float legSwing = 1.25F * clampedWalk;

			this.Head.yRot = netHeadYaw * DEG_TO_RAD;
			this.Head.xRot = headPitch * DEG_TO_RAD;

			this.RightLeg.xRot = Mth.cos(walkCycle) * legSwing;
			this.LeftLeg.xRot = Mth.cos(walkCycle + (float) Math.PI) * legSwing;
			this.RightArm.xRot = Mth.cos(walkCycle + (float) Math.PI) * armSwing;
			this.LeftArm.xRot = Mth.cos(walkCycle) * armSwing;

			float idle = Mth.sin(ageInTicks * 0.08F) * idleWeight;
			this.Waist.xRot += idle * 0.015F;
			this.Waist.y += idle * 0.18F;
			this.Head.xRot += Mth.sin(ageInTicks * 0.06F + 1.0F) * 0.015F * idleWeight;
			this.RightArm.zRot += 0.04F * idleWeight + Mth.sin(ageInTicks * 0.067F) * 0.012F * idleWeight;
			this.LeftArm.zRot -= 0.04F * idleWeight + Mth.sin(ageInTicks * 0.067F) * 0.012F * idleWeight;

			if (StunnedHeadAnimation.shouldAnimate(entity)) {
				StunnedHeadAnimation.apply(this.Head, ageInTicks);
			}

			this.setupAttackAnimation(entity);
		}

		private void setupEmpowerAnimation(UndeadKnightEntity entity, float ageInTicks, float netHeadYaw, float headPitch) {
			this.animate(entity.empowerAnimationState, undeadknightAnimation.EMPOWER, ageInTicks);
			this.Head.yRot += netHeadYaw * DEG_TO_RAD;
			this.Head.xRot += headPitch * DEG_TO_RAD;
		}

		private void setupBlockAnimation(UndeadKnightEntity entity, float ageInTicks) {
			this.animate(entity.blockAnimationState, undeadknightAnimation.BLOCK, ageInTicks);
		}

		private void setupSoulAbsorptionAnimation(UndeadKnightEntity entity, float ageInTicks) {
			if (entity.isSoulAbsorptionStarting()) {
				entity.soulAbsorptionStartAnimationState.startIfStopped(entity.tickCount - entity.getSoulAbsorptionTicks());
				this.animate(entity.soulAbsorptionStartAnimationState, undeadknightAnimation.SOUL_ABSORPTION_START, ageInTicks);
			} else if (entity.isSoulAbsorptionLooping()) {
				entity.soulAbsorptionLoopAnimationState.startIfStopped(entity.tickCount - entity.getSoulAbsorptionTicks() % 30);
				this.animate(entity.soulAbsorptionLoopAnimationState, undeadknightAnimation.SOUL_ABSORPTION_LOOP, ageInTicks);
			} else if (entity.isSoulAbsorptionEnding()) {
				entity.soulAbsorptionEndAnimationState.startIfStopped(entity.tickCount - entity.getSoulAbsorptionTicks());
				this.animate(entity.soulAbsorptionEndAnimationState, undeadknightAnimation.SOUL_ABSORPTION_END, ageInTicks);
			}
		}

		private void setupDeathAnimation(UndeadKnightEntity entity, float ageInTicks) {
			switch (entity.getDeathAnimationVariant()) {
				case UndeadKnightEntity.DEATH_ANIMATION_TWO:
					this.animate(entity.deathTwoAnimationState, undeadknightAnimation.DEATH_TWO, ageInTicks);
					break;
				case UndeadKnightEntity.DEATH_ANIMATION_THREE:
					this.animate(entity.deathThreeAnimationState, undeadknightAnimation.DEATH_THREE, ageInTicks);
					break;
				case UndeadKnightEntity.DEATH_ANIMATION_ONE:
				default:
					this.animate(entity.deathOneAnimationState, undeadknightAnimation.DEATH_ONE, ageInTicks);
					break;
			}
		}

		private void setupReincarnationAnimation(UndeadKnightEntity entity, float ageInTicks) {
			switch (entity.getReincarnationVariant()) {
				case 1:
					this.animate(entity.undyingTwoAnimationState, undeadknightAnimation.UNDYING_TWO, ageInTicks);
					break;
				case 2:
					this.animate(entity.undyingThreeAnimationState, undeadknightAnimation.UNDYING_THREE, ageInTicks);
					break;
				case 0:
				default:
					this.animate(entity.undyingOneAnimationState, undeadknightAnimation.UNDYING_ONE, ageInTicks);
					break;
			}
		}

		private void setupAttackAnimation(UndeadKnightEntity entity) {
			if (this.attackTime <= 0.0F) {
				return;
			}

			HumanoidArm attackingArm = this.getAttackArm(entity);
			ModelPart armPart = attackingArm == HumanoidArm.RIGHT ? this.RightArm : this.LeftArm;
			float bodyTwist = Mth.sin(Mth.sqrt(this.attackTime) * ((float) Math.PI * 2.0F)) * 0.2F;
			if (attackingArm == HumanoidArm.LEFT) {
				bodyTwist *= -1.0F;
			}

			this.Waist.yRot += bodyTwist;
			float easedAttack = 1.0F - this.attackTime;
			easedAttack *= easedAttack;
			easedAttack *= easedAttack;
			easedAttack = 1.0F - easedAttack;
			float swingDown = Mth.sin(easedAttack * (float) Math.PI);
			float headCompensation = Mth.sin(this.attackTime * (float) Math.PI) * -(this.Head.xRot - 0.7F) * 0.75F;

			armPart.xRot -= swingDown * 1.2F + headCompensation;
			armPart.yRot += bodyTwist * 2.0F;
			armPart.zRot += Mth.sin(this.attackTime * (float) Math.PI) * (attackingArm == HumanoidArm.RIGHT ? -0.4F : 0.4F);
		}

		private HumanoidArm getAttackArm(UndeadKnightEntity entity) {
			HumanoidArm mainArm = entity.getMainArm();
			return entity.swingingArm == InteractionHand.MAIN_HAND ? mainArm : mainArm.getOpposite();
		}

		@Override
		public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
			this.allbod.translateAndRotate(poseStack);
			this.Waist.translateAndRotate(poseStack);
			ModelPart armPart = arm == HumanoidArm.RIGHT ? this.RightArm : this.LeftArm;
			armPart.translateAndRotate(poseStack);
			poseStack.translate((arm == HumanoidArm.RIGHT ? -1.3F : 1.3F) / 16.0F, 0.0F, 0.0F);
		}

		@Override
		public ModelPart getHead() {
			return this.Head;
		}
	}
}
