package net.timothaty.timothatystrinkets.client.soul_empower;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.entity.HumanoidArm;

public final class SoulOrbAbsorptionFirstPersonPose {
	private static final float CENTER_TRANSLATE_X = 0.16F;
	private static final float RAISE_TRANSLATE_Y = 0.11F;
	private static final float FORWARD_TRANSLATE_Z = 0.16F;
	private static final float HOLD_ROTATE_X_DEGREES = -17.0F;
	private static final float HOLD_ROTATE_Y_DEGREES = -11.0F;
	private static final float HOLD_ROTATE_Z_DEGREES = 9.0F;

	private static final float PULL_SHAKE_TRANSLATE_X = 0.006F;
	private static final float PULL_SHAKE_TRANSLATE_Y = 0.004F;
	private static final float PULL_SHAKE_X_ROTATION_DEGREES = 1.15F;
	private static final float PULL_SHAKE_Z_ROTATION_DEGREES = 0.9F;
	private static final float PULSE_RECOIL_Z = -0.10F;
	private static final float PULSE_RECOIL_X_DEGREES = 7.0F;
	private static final float PULSE_SHAKE_X = 0.012F;
	private static final float PULSE_SHAKE_Z_DEGREES = 1.8F;

	private SoulOrbAbsorptionFirstPersonPose() {
	}

	public static void apply(PoseStack poseStack, HumanoidArm arm, float partialTick) {
		if (!SoulOrbAbsorptionClient.shouldTransformArm(arm)) {
			return;
		}

		SoulOrbAbsorptionFirstPersonAnimation.VisualPose pose = SoulOrbAbsorptionFirstPersonAnimation.sample(partialTick);
		if (!pose.visible()) {
			return;
		}

		float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
		float progress = pose.holdProgress();
		poseStack.translate(
				-side * CENTER_TRANSLATE_X * progress + side * PULL_SHAKE_TRANSLATE_X * pose.pullWave()
						+ side * PULSE_SHAKE_X * pose.pulseWave(),
				-RAISE_TRANSLATE_Y * progress + PULL_SHAKE_TRANSLATE_Y * pose.pullVerticalWave(),
				FORWARD_TRANSLATE_Z * progress + PULSE_RECOIL_Z * pose.pulse()
		);
		poseStack.mulPose(Axis.XP.rotationDegrees(HOLD_ROTATE_X_DEGREES * progress
				+ PULL_SHAKE_X_ROTATION_DEGREES * pose.pullWave() + PULSE_RECOIL_X_DEGREES * pose.pulse()));
		poseStack.mulPose(Axis.YP.rotationDegrees(side * HOLD_ROTATE_Y_DEGREES * progress));
		poseStack.mulPose(Axis.ZP.rotationDegrees(side * (HOLD_ROTATE_Z_DEGREES * progress
				+ PULL_SHAKE_Z_ROTATION_DEGREES * pose.pullVerticalWave() + PULSE_SHAKE_Z_DEGREES * pose.pulseWave())));
	}
}
