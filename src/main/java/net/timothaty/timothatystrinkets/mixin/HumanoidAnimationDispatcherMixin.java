package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.client.PaganCharmMeditationModelAnimation;
import net.timothaty.timothatystrinkets.client.animation.HumanoidAnimationRenderPassHelper;
import net.timothaty.timothatystrinkets.client.duelist.DuelistGuardThirdPersonAnimation;
import net.timothaty.timothatystrinkets.client.gorge.GorgeAnimationState;
import net.timothaty.timothatystrinkets.client.gorge.GorgeModelAnimation;
import net.timothaty.timothatystrinkets.client.hubris.HubrisActivationClientState;
import net.timothaty.timothatystrinkets.client.hubris.HubrisModelAnimation;
import net.timothaty.timothatystrinkets.client.morgenshtern.MorgenshternThirdPersonAnimation;
import net.timothaty.timothatystrinkets.client.soul_empower.SoulOrbAbsorptionThirdPersonAnimation;
import net.timothaty.timothatystrinkets.client.stunned.StunnedClientAnimationState;
import net.timothaty.timothatystrinkets.client.stunned.StunnedHeadAnimation;
import net.timothaty.timothatystrinkets.client.wrath_of_the_wicked.WrathOfTheWickedClientState;
import net.timothaty.timothatystrinkets.client.wrath_of_the_wicked.WrathOfTheWickedModelAnimation;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.mechanics.pagans_charm.PaganCharmMeditationPlayerState;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.model.HumanoidModel")
public abstract class HumanoidAnimationDispatcherMixin {
	@Shadow
	@Final
	public ModelPart head;

	@Shadow
	@Final
	public ModelPart hat;

	@Shadow
	@Final
	public ModelPart body;

	@Shadow
	@Final
	public ModelPart rightArm;

	@Shadow
	@Final
	public ModelPart leftArm;

	@Shadow
	@Final
	public ModelPart rightLeg;

	@Shadow
	@Final
	public ModelPart leftLeg;

	@Unique
	private boolean timothatys_trinkets$wasStunned;
	@Unique
	private boolean timothatys_trinkets$wasPaganCharmMeditating;
	@Unique
	private boolean timothatys_trinkets$wasAnimatingGorge;
	@Unique
	private boolean timothatys_trinkets$wasAnimatingWrathOfTheWicked;
	@Unique
	private boolean timothatys_trinkets$wasAnimatingHubris;
	@Unique
	private boolean timothatys_trinkets$debtlordHeadHidden;
	@Unique
	private boolean timothatys_trinkets$previousHeadVisible;
	@Unique
	private boolean timothatys_trinkets$previousHatVisible;
	@Unique
	private boolean timothatys_trinkets$firstPersonArmPass;

	@Inject(
			method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
			at = @At("HEAD"),
			require = 0
	)
	private void timothatys_trinkets$resetPreviousCustomPose(
			LivingEntity entity,
			float limbSwing,
			float limbSwingAmount,
			float ageInTicks,
			float netHeadYaw,
			float headPitch,
			CallbackInfo ci
	) {
		if (this.timothatys_trinkets$wasPaganCharmMeditating
				|| this.timothatys_trinkets$wasAnimatingHubris
				|| this.timothatys_trinkets$wasAnimatingWrathOfTheWicked) {
			this.timothatys_trinkets$resetAllParts();
		} else if (this.timothatys_trinkets$wasAnimatingGorge) {
			this.timothatys_trinkets$resetUpperBodyParts();
		}
	}

	@Inject(
			method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
			at = @At("TAIL"),
			require = 0
	)
	private void timothatys_trinkets$applyBaseHumanoidAnimations(
			LivingEntity entity,
			float limbSwing,
			float limbSwingAmount,
			float ageInTicks,
			float netHeadYaw,
			float headPitch,
			CallbackInfo ci
	) {
		this.timothatys_trinkets$firstPersonArmPass = HumanoidAnimationRenderPassHelper.isVanillaFirstPersonArmPass(
				entity,
				limbSwing,
				limbSwingAmount,
				ageInTicks,
				netHeadYaw,
				headPitch
		);

		this.timothatys_trinkets$applyStunnedHeadAnimation(entity, ageInTicks);
		this.timothatys_trinkets$applyPaganCharmMeditationAnimation(entity, ageInTicks);

		if (this.timothatys_trinkets$firstPersonArmPass) {
			this.timothatys_trinkets$wasAnimatingGorge = false;
			this.timothatys_trinkets$wasAnimatingWrathOfTheWicked = false;
			this.timothatys_trinkets$wasAnimatingHubris = false;
		} else {
			DuelistGuardThirdPersonAnimation.apply(entity, ageInTicks, this.rightArm);
			SoulOrbAbsorptionThirdPersonAnimation.apply(entity, ageInTicks, this.rightArm, this.leftArm);
		}
	}

	@Inject(
			method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
			at = @At("TAIL"),
			require = 0,
			order = 1500
	)
	private void timothatys_trinkets$applyActionHumanoidAnimations(
			LivingEntity entity,
			float limbSwing,
			float limbSwingAmount,
			float ageInTicks,
			float netHeadYaw,
			float headPitch,
			CallbackInfo ci
	) {
		if (this.timothatys_trinkets$firstPersonArmPass) {
			this.timothatys_trinkets$wasAnimatingGorge = false;
			return;
		}

		this.timothatys_trinkets$applyGorgePose(entity, ageInTicks);
		MorgenshternThirdPersonAnimation.apply(entity, ageInTicks, this.rightArm, this.leftArm);
	}

	@Inject(
			method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
			at = @At("TAIL"),
			require = 0,
			order = 2000
	)
	private void timothatys_trinkets$applyFinalHumanoidAnimations(
			LivingEntity entity,
			float limbSwing,
			float limbSwingAmount,
			float ageInTicks,
			float netHeadYaw,
			float headPitch,
			CallbackInfo ci
	) {
		if (this.timothatys_trinkets$firstPersonArmPass) {
			this.timothatys_trinkets$wasAnimatingWrathOfTheWicked = false;
			this.timothatys_trinkets$wasAnimatingHubris = false;
		} else {
			this.timothatys_trinkets$applyWrathOfTheWickedPose(entity, ageInTicks);
			this.timothatys_trinkets$applyHubrisPose(entity, ageInTicks);
		}

		this.timothatys_trinkets$updateDebtlordHeadVisibility(entity);
	}

	@Unique
	private void timothatys_trinkets$applyStunnedHeadAnimation(LivingEntity entity, float ageInTicks) {
		if (!StunnedHeadAnimation.shouldAnimate(entity)) {
			if (this.timothatys_trinkets$wasStunned)
				StunnedHeadAnimation.resetDetachedHatRoll(this.head, this.hat);

			this.timothatys_trinkets$wasStunned = false;
			return;
		}

		this.timothatys_trinkets$wasStunned = true;
		StunnedHeadAnimation.applyWithDetachedHat(this.head, this.hat, ageInTicks);
	}

	@Unique
	private void timothatys_trinkets$applyPaganCharmMeditationAnimation(LivingEntity entity, float ageInTicks) {
		if (!(entity instanceof PaganCharmMeditationPlayerState meditationState)
				|| StunnedClientAnimationState.isStunned(entity)) {
			this.timothatys_trinkets$wasPaganCharmMeditating = false;
			return;
		}

		int phase = meditationState.timothatys_trinkets$getPaganCharmMeditationPhase(ageInTicks);
		if (phase == PaganCharmMeditationPlayerState.PHASE_NONE) {
			this.timothatys_trinkets$wasPaganCharmMeditating = false;
			return;
		}

		if (phase == PaganCharmMeditationPlayerState.PHASE_MEDITATE) {
			PaganCharmMeditationModelAnimation.applyMeditate(
					meditationState.timothatys_trinkets$getPaganCharmMeditateAnimationState(),
					ageInTicks,
					this.head,
					this.body,
					this.rightArm,
					this.leftArm,
					this.rightLeg,
					this.leftLeg
			);
		} else {
			PaganCharmMeditationModelAnimation.applyLoop(
					meditationState.timothatys_trinkets$getPaganCharmMeditateLoopAnimationState(),
					ageInTicks,
					this.head,
					this.body,
					this.rightArm,
					this.leftArm,
					this.rightLeg,
					this.leftLeg
			);
		}

		this.hat.copyFrom(this.head);
		this.timothatys_trinkets$wasPaganCharmMeditating = true;
	}

	@Unique
	private void timothatys_trinkets$applyGorgePose(LivingEntity entity, float ageInTicks) {
		if (StunnedClientAnimationState.isStunned(entity)
				|| HubrisActivationClientState.isCasting(entity)
				|| WrathOfTheWickedClientState.isActive(entity)) {
			this.timothatys_trinkets$wasAnimatingGorge = false;
			return;
		}

		float elapsedTicks = GorgeAnimationState.elapsedTicks(entity, ageInTicks);
		if (elapsedTicks < 0.0F) {
			this.timothatys_trinkets$wasAnimatingGorge = false;
			return;
		}

		this.head.resetPose();
		this.hat.resetPose();
		this.rightArm.resetPose();
		this.leftArm.resetPose();
		GorgeModelAnimation.apply(
				elapsedTicks,
				this.head,
				this.body,
				this.rightArm,
				this.leftArm
		);
		this.hat.copyFrom(this.head);
		this.timothatys_trinkets$wasAnimatingGorge = true;
	}

	@Unique
	private void timothatys_trinkets$applyWrathOfTheWickedPose(LivingEntity entity, float ageInTicks) {
		if (!WrathOfTheWickedClientState.isActive(entity)
				|| HubrisActivationClientState.isCasting(entity)
				|| StunnedClientAnimationState.isStunned(entity)) {
			this.timothatys_trinkets$wasAnimatingWrathOfTheWicked = false;
			return;
		}

		float elapsedTicks = WrathOfTheWickedClientState.getAnimationElapsedTicks(entity, ageInTicks);
		if (elapsedTicks < 0.0F) {
			this.timothatys_trinkets$wasAnimatingWrathOfTheWicked = false;
			return;
		}

		this.timothatys_trinkets$resetAllParts();
		WrathOfTheWickedModelAnimation.apply(
				elapsedTicks,
				this.head,
				this.body,
				this.rightArm,
				this.leftArm,
				this.rightLeg,
				this.leftLeg
		);
		this.hat.copyFrom(this.head);
		this.timothatys_trinkets$wasAnimatingWrathOfTheWicked = true;
	}

	@Unique
	private void timothatys_trinkets$applyHubrisPose(LivingEntity entity, float ageInTicks) {
		HubrisActivationClientState.View state = HubrisActivationClientState.getView(entity);
		if (state == null) {
			this.timothatys_trinkets$wasAnimatingHubris = false;
			return;
		}

		float elapsedTicks = HubrisActivationClientState.elapsedTicks(entity, ageInTicks);
		if (elapsedTicks < 0.0F) {
			this.timothatys_trinkets$wasAnimatingHubris = false;
			return;
		}

		this.timothatys_trinkets$resetAllParts();
		HubrisModelAnimation.apply(
				elapsedTicks,
				state.variant(),
				state.mainArm(),
				this.head,
				this.body,
				this.rightArm,
				this.leftArm,
				this.rightLeg,
				this.leftLeg
		);
		this.hat.copyFrom(this.head);
		this.timothatys_trinkets$wasAnimatingHubris = true;
	}

	@Unique
	private void timothatys_trinkets$updateDebtlordHeadVisibility(LivingEntity entity) {
		boolean wearingDebtlordHead = entity instanceof Player
				&& entity.getItemBySlot(EquipmentSlot.HEAD).is(TimothatysTrinketsModItems.DEBTLORDS_HEAD.get());
		if (wearingDebtlordHead) {
			if (!this.timothatys_trinkets$debtlordHeadHidden) {
				this.timothatys_trinkets$previousHeadVisible = this.head.visible;
				this.timothatys_trinkets$previousHatVisible = this.hat.visible;
			}
			this.head.visible = false;
			this.hat.visible = false;
			this.timothatys_trinkets$debtlordHeadHidden = true;
			return;
		}

		if (this.timothatys_trinkets$debtlordHeadHidden) {
			this.head.visible = this.timothatys_trinkets$previousHeadVisible;
			this.hat.visible = this.timothatys_trinkets$previousHatVisible;
			this.timothatys_trinkets$debtlordHeadHidden = false;
		}
	}

	@Unique
	private void timothatys_trinkets$resetAllParts() {
		this.head.resetPose();
		this.hat.resetPose();
		this.body.resetPose();
		this.rightArm.resetPose();
		this.leftArm.resetPose();
		this.rightLeg.resetPose();
		this.leftLeg.resetPose();
	}

	@Unique
	private void timothatys_trinkets$resetUpperBodyParts() {
		this.head.resetPose();
		this.hat.resetPose();
		this.body.resetPose();
		this.rightArm.resetPose();
		this.leftArm.resetPose();
	}
}
