package net.timothaty.timothatystrinkets.client.hubris;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris.HubrisAnimationVariant;

import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;

public final class HubrisSwordItemTransform {
	private static final float THIRD_PERSON_RIGHT_GRIP_Y = -0.13F;
	private static final float THIRD_PERSON_RIGHT_GRIP_Z = 0.116F;
	private static final float THIRD_PERSON_LEFT_GRIP_Y = 0.335F;
	private static final float THIRD_PERSON_LEFT_GRIP_Z = 0.512F;
	private static final float FIRST_PERSON_RIGHT_GRIP_Y = -0.167F;
	private static final float FIRST_PERSON_RIGHT_GRIP_Z = -0.063F;
	private static final float FIRST_PERSON_LEFT_GRIP_Y = 0.066F;
	private static final float FIRST_PERSON_LEFT_GRIP_Z = 0.438F;

	private HubrisSwordItemTransform() {
	}

	public static void apply(
			PoseStack poseStack,
			HubrisActivationClientState.View state,
			HumanoidArm renderedArm,
			ItemDisplayContext context
	) {
		if (state == null
				|| state.variant() != HubrisAnimationVariant.SWORD
				|| state.mainArm() != renderedArm)
			return;

		float gripY = gripY(context);
		float gripZ = gripZ(context);
		poseStack.translate(0.0F, gripY, gripZ);
		poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
		poseStack.translate(0.0F, -gripY, -gripZ);
	}

	private static float gripY(ItemDisplayContext context) {
		return switch (context) {
			case THIRD_PERSON_RIGHT_HAND -> THIRD_PERSON_RIGHT_GRIP_Y;
			case THIRD_PERSON_LEFT_HAND -> THIRD_PERSON_LEFT_GRIP_Y;
			case FIRST_PERSON_RIGHT_HAND -> FIRST_PERSON_RIGHT_GRIP_Y;
			case FIRST_PERSON_LEFT_HAND -> FIRST_PERSON_LEFT_GRIP_Y;
			default -> 0.0F;
		};
	}

	private static float gripZ(ItemDisplayContext context) {
		return switch (context) {
			case THIRD_PERSON_RIGHT_HAND -> THIRD_PERSON_RIGHT_GRIP_Z;
			case THIRD_PERSON_LEFT_HAND -> THIRD_PERSON_LEFT_GRIP_Z;
			case FIRST_PERSON_RIGHT_HAND -> FIRST_PERSON_RIGHT_GRIP_Z;
			case FIRST_PERSON_LEFT_HAND -> FIRST_PERSON_LEFT_GRIP_Z;
			default -> 0.0F;
		};
	}

}
