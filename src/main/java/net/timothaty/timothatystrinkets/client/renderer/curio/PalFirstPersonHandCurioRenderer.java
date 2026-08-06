package net.timothaty.timothatystrinkets.client.renderer.curio;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;

import net.timothaty.timothatystrinkets.client.compat.FirstPersonModelCompat;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class PalFirstPersonHandCurioRenderer {
	private PalFirstPersonHandCurioRenderer() {
	}

	public static void render(
			AbstractClientPlayer player,
			PlayerModel<?> playerModel,
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			int light
	) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!FirstPersonMode.isFirstPersonPass()
				|| player != minecraft.player
				|| player != minecraft.getCameraEntity()
				|| !isConfiguredFirstPersonCamera(minecraft)
				|| FirstPersonModelCompat.isTrueFirstPersonActive()
				|| player.isSpectator()
				|| player.isInvisible()) {
			return;
		}

		if (playerModel.rightArm.visible)
			renderArm(player, playerModel, HumanoidArm.RIGHT, poseStack, bufferSource, light);
		if (playerModel.leftArm.visible)
			renderArm(player, playerModel, HumanoidArm.LEFT, poseStack, bufferSource, light);
	}

	private static boolean isConfiguredFirstPersonCamera(Minecraft minecraft) {
		/*
		 * PAL temporarily marks Camera itself as detached while rendering its
		 * THIRD_PERSON_MODEL first-person pass. The configured camera type keeps
		 * the user's real perspective and therefore distinguishes that internal
		 * implementation detail from an actual detached/F5 camera.
		 */
		return minecraft.options.getCameraType() == CameraType.FIRST_PERSON;
	}

	private static void renderArm(
			AbstractClientPlayer player,
			PlayerModel<?> playerModel,
			HumanoidArm arm,
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			int light
	) {
		ResolvedHandCurioVisuals visuals = HandCurioVisualResolver.resolve(
				player,
				arm,
				HandCurioRenderContext.PAL_FIRST_PERSON
		);
		if (visuals.isEmpty())
			return;

		HandCurioModelRenderer.INSTANCE.render(
				player,
				arm,
				playerModel,
				visuals,
				poseStack,
				bufferSource,
				light
		);
	}
}
