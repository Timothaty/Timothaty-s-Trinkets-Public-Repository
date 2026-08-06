package net.timothaty.timothatystrinkets.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.beatific_pallium.BeatificPalliumClientState;
import net.timothaty.timothatystrinkets.client.model.ModelBeatificPallium;
import net.timothaty.timothatystrinkets.entity.BeatificPalliumEntity;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.beatific_pallium.BeatificPalliumData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public final class BeatificPalliumRenderer extends EntityRenderer<BeatificPalliumEntity> {
	private static final ResourceLocation PALLIUM_TEXTURE = ResourceLocation.fromNamespaceAndPath(
			TimothatysTrinketsMod.MODID, "textures/entities/pallium/beatific_pallium.png");
	private static final ResourceLocation RIPPLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
			TimothatysTrinketsMod.MODID, "textures/particle/beatific_pallium_ripple.png");
	private static final RenderType PALLIUM_RENDER_TYPE = BeatificPalliumRenderTypes.translucentColorOnly(PALLIUM_TEXTURE);
	private static final RenderType RIPPLE_RENDER_TYPE = BeatificPalliumRenderTypes.translucentColorOnly(RIPPLE_TEXTURE);
	private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;
	private static final float SHELL_HALF_SIZE = (float) BeatificPalliumData.SHELL_HALF_SIZE;
	private static final float RUNE_PULSE_MAX_ALPHA = 0.30F;
	private static final int RUNE_PULSE_RGB = 0xFFF3B0;

	private final ModelBeatificPallium model;

	public BeatificPalliumRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new ModelBeatificPallium(context.bakeLayer(ModelBeatificPallium.LAYER_LOCATION));
		this.shadowRadius = 0.0F;
		this.shadowStrength = 0.0F;
	}

	@Override
	public void render(BeatificPalliumEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
			MultiBufferSource buffer, int packedLight) {
		LivingEntity target = entity.getTarget();
		if (target == null || shouldHideInFirstPerson(target)
				|| entity.getVisualPhase() == BeatificPalliumEntity.VisualPhase.BURST
				&& entity.getVisualPhaseAge(partialTick) > 2.0F) {
			return;
		}

		poseStack.pushPose();
		translateToInterpolatedTarget(entity, target, partialTick, poseStack);
		float bodyYaw = Mth.rotLerp(partialTick, target.yBodyRotO, target.yBodyRot);
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		poseStack.translate(0.00625F, -0.15625F, 0.09375F);

		float ageInTicks = entity.tickCount + partialTick;
		entity.ensureAnimationStateInitialized();
		this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
		BeatificPalliumClientState.RenderView renderView = BeatificPalliumClientState.renderView(entity, partialTick);

		VertexConsumer shellConsumer = buffer.getBuffer(PALLIUM_RENDER_TYPE);
		this.model.renderInnerToBuffer(poseStack, shellConsumer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, colorWithAlpha(0.58F));
		renderRipples(poseStack, buffer.getBuffer(RIPPLE_RENDER_TYPE), renderView);
		VertexConsumer runeConsumer = buffer.getBuffer(PALLIUM_RENDER_TYPE);
		this.model.renderRunesToBuffer(poseStack, runeConsumer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, colorWithAlpha(0.68F));
		if (renderView.runePulse() > 0.0F) {
			float pulseAlpha = RUNE_PULSE_MAX_ALPHA * renderView.runePulse();
			this.model.renderRunesToBuffer(poseStack, runeConsumer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
					colorWithAlpha(pulseAlpha, RUNE_PULSE_RGB));
		}

		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
	}

	private static boolean shouldHideInFirstPerson(LivingEntity target) {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.player == target && minecraft.options.getCameraType().isFirstPerson();
	}

	private static void translateToInterpolatedTarget(BeatificPalliumEntity entity, LivingEntity target,
			float partialTick, PoseStack poseStack) {
		double entityX = Mth.lerp(partialTick, entity.xo, entity.getX());
		double entityY = Mth.lerp(partialTick, entity.yo, entity.getY());
		double entityZ = Mth.lerp(partialTick, entity.zo, entity.getZ());
		double targetX = Mth.lerp(partialTick, target.xo, target.getX());
		double targetY = Mth.lerp(partialTick, target.yo, target.getY())
				+ target.getBbHeight() * BeatificPalliumData.VISUAL_CENTER_HEIGHT_FACTOR;
		double targetZ = Mth.lerp(partialTick, target.zo, target.getZ());
		poseStack.translate(targetX - entityX, targetY - entityY, targetZ - entityZ);
	}

	private void renderRipples(PoseStack poseStack, VertexConsumer consumer, BeatificPalliumClientState.RenderView renderView) {
		if (renderView.rippleCount() == 0)
			return;

		poseStack.pushPose();
		this.model.translatePallium(poseStack);
		PoseStack.Pose pose = poseStack.last();
		for (int index = 0; index < renderView.rippleCount(); index++) {
			BeatificPalliumClientState.Ripple ripple = renderView.rippleAt(index);
			if (ripple == null)
				continue;
			float progress = Mth.clamp((float) ((renderView.now() - ripple.startGameTime()) / ripple.lifetimeTicks()), 0.0F, 1.0F);
			float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
			float radius = Mth.lerp(eased, 1.0F / 16.0F, 3.75F / 16.0F);
			float alpha = Mth.sin(progress * (float) Math.PI) * 0.42F;
			if (alpha <= 0.001F)
				continue;
			float centerU = (ripple.u() - 0.5F) * SHELL_HALF_SIZE * 2.0F;
			float centerV = (ripple.v() - 0.5F) * SHELL_HALF_SIZE * 2.0F;
			renderRippleQuad(consumer, pose, ripple.face(), centerU, centerV, radius, Mth.clamp(Math.round(alpha * 255.0F), 0, 255));
		}
		poseStack.popPose();
	}

	private static void renderRippleQuad(VertexConsumer consumer, PoseStack.Pose pose, int face,
			float centerU, float centerV, float radius, int alpha) {
		float minU = Mth.clamp(centerU - radius, -SHELL_HALF_SIZE, SHELL_HALF_SIZE);
		float maxU = Mth.clamp(centerU + radius, -SHELL_HALF_SIZE, SHELL_HALF_SIZE);
		float minV = Mth.clamp(centerV - radius, -SHELL_HALF_SIZE, SHELL_HALF_SIZE);
		float maxV = Mth.clamp(centerV + radius, -SHELL_HALF_SIZE, SHELL_HALF_SIZE);
		float h = SHELL_HALF_SIZE;
		switch (face) {
			case 0 -> quad(consumer, pose, h, minV, minU, h, minV, maxU, h, maxV, maxU, h, maxV, minU, alpha, 1, 0, 0);
			case 1 -> quad(consumer, pose, -h, minV, maxU, -h, minV, minU, -h, maxV, minU, -h, maxV, maxU, alpha, -1, 0, 0);
			case 2 -> quad(consumer, pose, minU, -h, maxV, maxU, -h, maxV, maxU, -h, minV, minU, -h, minV, alpha, 0, -1, 0);
			case 3 -> quad(consumer, pose, minU, h, minV, maxU, h, minV, maxU, h, maxV, minU, h, maxV, alpha, 0, 1, 0);
			case 4 -> quad(consumer, pose, minU, minV, h, maxU, minV, h, maxU, maxV, h, minU, maxV, h, alpha, 0, 0, 1);
			default -> quad(consumer, pose, maxU, minV, -h, minU, minV, -h, minU, maxV, -h, maxU, maxV, -h, alpha, 0, 0, -1);
		}
	}

	private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
			float x0, float y0, float z0, float x1, float y1, float z1,
			float x2, float y2, float z2, float x3, float y3, float z3,
			int alpha, float nx, float ny, float nz) {
		vertex(consumer, pose, x0, y0, z0, 0.0F, 1.0F, alpha, nx, ny, nz);
		vertex(consumer, pose, x1, y1, z1, 1.0F, 1.0F, alpha, nx, ny, nz);
		vertex(consumer, pose, x2, y2, z2, 1.0F, 0.0F, alpha, nx, ny, nz);
		vertex(consumer, pose, x3, y3, z3, 0.0F, 0.0F, alpha, nx, ny, nz);
		vertex(consumer, pose, x3, y3, z3, 0.0F, 0.0F, alpha, -nx, -ny, -nz);
		vertex(consumer, pose, x2, y2, z2, 1.0F, 0.0F, alpha, -nx, -ny, -nz);
		vertex(consumer, pose, x1, y1, z1, 1.0F, 1.0F, alpha, -nx, -ny, -nz);
		vertex(consumer, pose, x0, y0, z0, 0.0F, 1.0F, alpha, -nx, -ny, -nz);
	}

	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
			float u, float v, int alpha, float nx, float ny, float nz) {
		consumer.addVertex(pose, x, y, z)
				.setColor(255, 255, 255, alpha)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(FULL_BRIGHT)
				.setNormal(pose, nx, ny, nz);
	}

	private static int colorWithAlpha(float alpha) {
		return colorWithAlpha(alpha, 0xFFFFFF);
	}

	private static int colorWithAlpha(float alpha, int rgb) {
		return Mth.clamp(Math.round(alpha * 255.0F), 0, 255) << 24 | (rgb & 0xFFFFFF);
	}

	@Override
	public ResourceLocation getTextureLocation(BeatificPalliumEntity entity) {
		return PALLIUM_TEXTURE;
	}
}
