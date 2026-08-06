package net.timothaty.timothatystrinkets.client.stunned;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public final class StunnedHeadAnimation {
	private static final float HEAD_DOWN = 28.0F * Mth.DEG_TO_RAD;
	private static final float HEAD_BOB = 1.5F * Mth.DEG_TO_RAD;
	private static final float YAW_SWAY = 4.0F * Mth.DEG_TO_RAD;
	private static final float ROLL_SWAY = 2.25F * Mth.DEG_TO_RAD;
	private static final float SWAY_SPEED = 0.42F;
	private static final float HEAD_DOWN_BLEND = 0.75F;

	private StunnedHeadAnimation() {
	}

	public static boolean shouldAnimate(Entity entity) {
		return StunnedClientAnimationState.isStunned(entity);
	}

	public static void apply(ModelPart head, float ageInTicks) {
		if (head == null)
			return;

		float wave = Mth.sin(ageInTicks * SWAY_SPEED);
		float softBob = Mth.sin(ageInTicks * SWAY_SPEED * 0.55F) * HEAD_BOB;

		head.xRot = Mth.lerp(HEAD_DOWN_BLEND, head.xRot, HEAD_DOWN + softBob);
		head.yRot += wave * YAW_SWAY;
		head.zRot = wave * ROLL_SWAY;
	}

	public static void applyWithDetachedHat(ModelPart head, ModelPart hat, float ageInTicks) {
		apply(head, ageInTicks);

		if (head != null && hat != null)
			hat.copyFrom(head);
	}

	public static void resetRoll(ModelPart head) {
		if (head != null)
			head.zRot = 0.0F;
	}

	public static void resetDetachedHatRoll(ModelPart head, ModelPart hat) {
		resetRoll(head);

		if (head != null && hat != null)
			hat.copyFrom(head);
	}
}
