package net.timothaty.timothatystrinkets.client.morgenshtern;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class MorgenshternThirdPersonAnimation {
	private MorgenshternThirdPersonAnimation() {
	}

	public static void apply(
			LivingEntity entity,
			float ageInTicks,
			ModelPart rightArm,
			ModelPart leftArm
	) {
		if (!(entity instanceof Player)
				|| rightArm == null
				|| leftArm == null)
			return;

		float elapsed = MorgenshternStrikeClientState.elapsedTicks(
				entity,
				ageInTicks
		);
		if (elapsed < 0.0F)
			return;

		MorgenshternOberhauAnimation.Pose pose =
				MorgenshternOberhauAnimation.sample(elapsed);
		boolean right = entity.getMainArm() == HumanoidArm.RIGHT;
		ModelPart arm = right ? rightArm : leftArm;
		float mirror = right ? 1.0F : -1.0F;

		arm.xRot = pose.xDegrees() * Mth.DEG_TO_RAD;
		arm.yRot = pose.yDegrees() * Mth.DEG_TO_RAD * mirror;
		arm.zRot = pose.zDegrees() * Mth.DEG_TO_RAD * mirror;
	}
}
