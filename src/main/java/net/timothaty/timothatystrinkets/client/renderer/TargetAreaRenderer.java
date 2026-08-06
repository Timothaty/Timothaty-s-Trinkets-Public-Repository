package net.timothaty.timothatystrinkets.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.entity.AreaShape;
import net.timothaty.timothatystrinkets.entity.TargetAreaEntity;

public class TargetAreaRenderer extends EntityRenderer<TargetAreaEntity> {
	private static final ResourceLocation DEFAULT_TEXTURE = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/entities/target_area/solid.png");
	private static final RenderType RENDER_TYPE = RenderType.entityTranslucent(DEFAULT_TEXTURE);
	private static final float BOTTOM_Y = 0.02F;
	private static final float TEXTURE_REPEAT_LENGTH = 1.0F;

	public TargetAreaRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.0F;
		this.shadowStrength = 0.0F;
	}

	@Override
	public void render(TargetAreaEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		float alpha = getAlpha(entity, partialTick);
		if (alpha <= 0.0F) {
			super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
			return;
		}

		int color = entity.getColor();
		int red = color >> 16 & 255;
		int green = color >> 8 & 255;
		int blue = color & 255;
		int alphaByte = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);

		poseStack.pushPose();
		VertexConsumer consumer = buffer.getBuffer(RENDER_TYPE);
		PoseStack.Pose pose = poseStack.last();
		int light = getPackedFullBright();

		if (entity.getShape() == AreaShape.CIRCLE) {
			renderCircle(entity, consumer, pose, red, green, blue, alphaByte, light);
		} else {
			renderRectangle(entity, consumer, pose, red, green, blue, alphaByte, light);
		}

		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
	}

	private void renderCircle(TargetAreaEntity entity, VertexConsumer consumer, PoseStack.Pose pose, int red, int green, int blue, int alpha, int light) {
		float radius = entity.getRadius();
		float height = entity.getAreaHeight();
		int segments = Mth.clamp((int) (radius * 12.0F), 32, 128);
		float circumference = (float) (Math.PI * 2.0D * radius);

		for (int segment = 0; segment < segments; segment++) {
			double angle0 = Math.PI * 2.0D * segment / segments;
			double angle1 = Math.PI * 2.0D * (segment + 1) / segments;
			float x0 = (float) Math.cos(angle0) * radius;
			float z0 = (float) Math.sin(angle0) * radius;
			float x1 = (float) Math.cos(angle1) * radius;
			float z1 = (float) Math.sin(angle1) * radius;
			float u0 = circumference * segment / segments / TEXTURE_REPEAT_LENGTH;
			float u1 = circumference * (segment + 1) / segments / TEXTURE_REPEAT_LENGTH;
			addVerticalQuad(consumer, pose, x0, z0, x1, z1, height, u0, u1, red, green, blue, alpha, light);
		}
	}

	private void renderRectangle(TargetAreaEntity entity, VertexConsumer consumer, PoseStack.Pose pose, int red, int green, int blue, int alpha, int light) {
		float halfWidth = entity.getHalfWidth();
		float halfDepth = entity.getHalfDepth();
		float height = entity.getAreaHeight();
		float width = halfWidth * 2.0F;
		float depth = halfDepth * 2.0F;

		addVerticalQuad(consumer, pose, -halfWidth, -halfDepth, halfWidth, -halfDepth, height, 0.0F, width, red, green, blue, alpha, light);
		addVerticalQuad(consumer, pose, halfWidth, -halfDepth, halfWidth, halfDepth, height, 0.0F, depth, red, green, blue, alpha, light);
		addVerticalQuad(consumer, pose, halfWidth, halfDepth, -halfWidth, halfDepth, height, 0.0F, width, red, green, blue, alpha, light);
		addVerticalQuad(consumer, pose, -halfWidth, halfDepth, -halfWidth, -halfDepth, height, 0.0F, depth, red, green, blue, alpha, light);
	}

	private void addVerticalQuad(VertexConsumer consumer, PoseStack.Pose pose, float x0, float z0, float x1, float z1, float height, float u0, float u1,
			int red, int green, int blue, int alpha, int light) {
		float topY = Math.max(BOTTOM_Y + 0.01F, height);
		float dx = x1 - x0;
		float dz = z1 - z0;
		float length = Mth.sqrt(dx * dx + dz * dz);
		float normalX = length <= 0.0001F ? 0.0F : -dz / length;
		float normalZ = length <= 0.0001F ? 1.0F : dx / length;
		int topRed = Mth.clamp(Math.round(red * 0.15F), 0, 255);
		int topGreen = Mth.clamp(Math.round(green * 0.15F), 0, 255);
		int topBlue = Mth.clamp(Math.round(blue * 0.15F), 0, 255);
		int topAlpha = Mth.clamp(Math.round(alpha * 0.15F), 0, 255);

		vertex(consumer, pose, x0, BOTTOM_Y, z0, u0, 1.0F, red, green, blue, alpha, light, normalX, normalZ);
		vertex(consumer, pose, x1, BOTTOM_Y, z1, u1, 1.0F, red, green, blue, alpha, light, normalX, normalZ);
		vertex(consumer, pose, x1, topY, z1, u1, 0.0F, topRed, topGreen, topBlue, topAlpha, light, normalX, normalZ);
		vertex(consumer, pose, x0, topY, z0, u0, 0.0F, topRed, topGreen, topBlue, topAlpha, light, normalX, normalZ);
	}

	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int red, int green, int blue,
			int alpha, int light, float normalX, float normalZ) {
		consumer.addVertex(pose, x, y, z)
			.setColor(red, green, blue, alpha)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(light)
			.setNormal(pose, normalX, 0.0F, normalZ);
	}

	private static int getPackedFullBright() {
		return LightTexture.FULL_BRIGHT;
	}

	private static float getAlpha(TargetAreaEntity entity, float partialTick) {
		float age = Math.max(0.0F, entity.tickCount + partialTick);
		float duration = Math.max(1.0F, entity.getDuration());
		float alpha = 1.0F;

		int fadeInTicks = entity.getFadeInTicks();
		if (fadeInTicks > 0) {
			alpha = Math.min(alpha, Mth.clamp(age / fadeInTicks, 0.0F, 1.0F));
		}

		int fadeOutTicks = entity.getFadeOutTicks();
		if (fadeOutTicks > 0) {
			alpha = Math.min(alpha, Mth.clamp((duration - age) / fadeOutTicks, 0.0F, 1.0F));
		}

		int encodedAlpha = entity.getColor() >>> 24;
		if ((entity.getColor() & 0xFF000000) != 0) {
			alpha *= encodedAlpha / 255.0F;
		}

		return Mth.clamp(alpha, 0.0F, 1.0F);
	}

	@Override
	public ResourceLocation getTextureLocation(TargetAreaEntity entity) {
		return DEFAULT_TEXTURE;
	}
}
