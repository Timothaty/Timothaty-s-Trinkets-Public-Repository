package net.timothaty.timothatystrinkets.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import net.timothaty.timothatystrinkets.entity.SoulOrbEntity;

public class SoulOrbRenderer extends EntityRenderer<SoulOrbEntity> {
	private static final ResourceLocation SOUL_ORB_LOCATION = ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "textures/entities/soul_orb.png");
	private static final RenderType RENDER_TYPE = RenderType.itemEntityTranslucentCull(SOUL_ORB_LOCATION);
	private static final int SOUL_RED = 0x00;
	private static final int SOUL_GREEN = 0xFF;
	private static final int SOUL_BLUE = 0xA7;

	public SoulOrbRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.12F;
		this.shadowStrength = 0.45F;
	}

	@Override
	protected int getBlockLightLevel(SoulOrbEntity entity, BlockPos pos) {
		return Mth.clamp(super.getBlockLightLevel(entity, pos) + 7, 0, 15);
	}

	@Override
	public void render(SoulOrbEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		poseStack.pushPose();

		float minU = 0.0F;
		float maxU = 1.0F;
		float minV = 0.0F;
		float maxV = 1.0F;
		float pulse = ((float) entity.tickCount + partialTicks) * 0.18F;
		float scale = 0.32F + Mth.sin(pulse) * 0.025F;
		int alpha = 150 + (int) ((Mth.sin(pulse * 1.4F) + 1.0F) * 32.0F);

		poseStack.translate(0.0F, 0.1F, 0.0F);
		poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		poseStack.scale(scale, scale, scale);

		VertexConsumer consumer = buffer.getBuffer(RENDER_TYPE);
		PoseStack.Pose pose = poseStack.last();
		int light = LightTexture.FULL_BRIGHT;
		vertex(consumer, pose, -0.5F, -0.25F, minU, maxV, alpha, light);
		vertex(consumer, pose, 0.5F, -0.25F, maxU, maxV, alpha, light);
		vertex(consumer, pose, 0.5F, 0.75F, maxU, minV, alpha, light);
		vertex(consumer, pose, -0.5F, 0.75F, minU, minV, alpha, light);

		poseStack.popPose();
		super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
	}

	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v, int alpha, int packedLight) {
		consumer.addVertex(pose, x, y, 0.0F)
				.setColor(SOUL_RED, SOUL_GREEN, SOUL_BLUE, alpha)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(packedLight)
				.setNormal(pose, 0.0F, 1.0F, 0.0F);
	}

	@Override
	public ResourceLocation getTextureLocation(SoulOrbEntity entity) {
		return SOUL_ORB_LOCATION;
	}
}
