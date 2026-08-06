package net.timothaty.timothatystrinkets.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

public final class OrbHaloRenderer {
	private static final ResourceLocation HALO_TEXTURE = ResourceLocation.fromNamespaceAndPath(
			TimothatysTrinketsMod.MODID,
			"textures/particle/halo.png"
	);
	private static final RenderType HALO_RENDER_TYPE = TimothatysTrinketsRenderTypes.orbitingOrbHalo(HALO_TEXTURE);

	private static final float HALO_SIZE = 0.58F;
	private static final float HALO_ALPHA = 0.70F;
	private static final float CAMERA_OFFSET = 0.06F;

	private OrbHaloRenderer() {
	}

	public static void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedRgb, float orbScale) {
		int red = packedRgb >> 16 & 0xFF;
		int green = packedRgb >> 8 & 0xFF;
		int blue = packedRgb & 0xFF;
		int alpha = Math.round(HALO_ALPHA * 255.0F);
		float halfSize = HALO_SIZE * orbScale * 0.5F;

		poseStack.pushPose();
		try {
			poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
			poseStack.translate(0.0F, 0.0F, CAMERA_OFFSET * orbScale);

			VertexConsumer consumer = bufferSource.getBuffer(HALO_RENDER_TYPE);
			PoseStack.Pose pose = poseStack.last();
			vertex(consumer, pose, -halfSize, -halfSize, 0.0F, 1.0F, red, green, blue, alpha);
			vertex(consumer, pose, halfSize, -halfSize, 1.0F, 1.0F, red, green, blue, alpha);
			vertex(consumer, pose, halfSize, halfSize, 1.0F, 0.0F, red, green, blue, alpha);
			vertex(consumer, pose, -halfSize, halfSize, 0.0F, 0.0F, red, green, blue, alpha);
		} finally {
			poseStack.popPose();
		}
	}

	private static void vertex(
			VertexConsumer consumer,
			PoseStack.Pose pose,
			float x,
			float y,
			float u,
			float v,
			int red,
			int green,
			int blue,
			int alpha
	) {
		consumer.addVertex(pose, x, y, 0.0F)
				.setColor(red, green, blue, alpha)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightTexture.FULL_BRIGHT)
				.setNormal(pose, 0.0F, 0.0F, 1.0F);
	}
}
