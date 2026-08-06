package net.timothaty.timothatystrinkets.client.vfx.soul_rip;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class SoulRipTrailRenderer {
	private static final RenderType TRAIL_RENDER_TYPE = RenderType.create(
			"timothatys_trinkets_soul_rip_trail",
			DefaultVertexFormat.POSITION_COLOR,
			VertexFormat.Mode.QUADS,
			256,
			false,
			true,
			RenderType.CompositeState.builder()
					.setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
					.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
					.setCullState(RenderStateShard.NO_CULL)
					.setLightmapState(RenderStateShard.NO_LIGHTMAP)
					.createCompositeState(false)
	);

	private SoulRipTrailRenderer() {
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		SoulRipTrailHandler.tick();
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;
		if (SoulRipTrailHandler.trails().isEmpty())
			return;

		Minecraft minecraft = Minecraft.getInstance();
		Camera camera = minecraft.gameRenderer.getMainCamera();
		Vec3 cameraPosition = camera.getPosition();
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
		VertexConsumer consumer = bufferSource.getBuffer(TRAIL_RENDER_TYPE);

		poseStack.pushPose();
		poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
		for (SoulRipTrail trail : SoulRipTrailHandler.trails()) {
			renderTrail(poseStack, consumer, trail, cameraPosition);
		}
		poseStack.popPose();
		bufferSource.endBatch(TRAIL_RENDER_TYPE);
	}

	private static void renderTrail(PoseStack poseStack, VertexConsumer consumer, SoulRipTrail trail, Vec3 cameraPosition) {
		float baseWidth = trail.width();
		float baseAlpha = trail.alpha();
		if (baseWidth <= 0.001F || baseAlpha <= 0.001F)
			return;

		int pointCount = trail.pointCount();
		float visibleProgress = trail.visibleProgress();
		int visibleSegments = Math.max(1, (int) Math.ceil((pointCount - 1) * visibleProgress));
		for (int i = 0; i < visibleSegments; i++) {
			float t0 = i / (float) (pointCount - 1);
			float t1 = Math.min((i + 1) / (float) (pointCount - 1), visibleProgress);
			if (t1 <= t0)
				continue;

			Vec3 start = trail.pointAt(t0);
			Vec3 end = trail.pointAt(t1);
			Vec3 segment = end.subtract(start);
			if (segment.lengthSqr() < 0.00001D)
				continue;

			Vec3 mid = start.add(end).scale(0.5D);
			Vec3 toCamera = cameraPosition.subtract(mid);
			Vec3 side = segment.cross(toCamera).normalize();
			if (side.lengthSqr() < 0.00001D) {
				side = new Vec3(1.0D, 0.0D, 0.0D);
			}

			float width0 = baseWidth * widthAlongTrail(t0);
			float width1 = baseWidth * widthAlongTrail(t1);
			float alpha0 = baseAlpha * alphaAlongTrail(t0);
			float alpha1 = baseAlpha * alphaAlongTrail(t1);

			Vec3 s0 = start.add(side.scale(width0));
			Vec3 s1 = start.subtract(side.scale(width0));
			Vec3 e0 = end.add(side.scale(width1));
			Vec3 e1 = end.subtract(side.scale(width1));

			vertex(poseStack, consumer, s0, alpha0);
			vertex(poseStack, consumer, e0, alpha1);
			vertex(poseStack, consumer, e1, alpha1);
			vertex(poseStack, consumer, s1, alpha0);
		}
	}

	private static float widthAlongTrail(float t) {
		float middle = (float) Math.sin(Math.PI * t);
		return 0.08F + 0.92F * middle;
	}

	private static float alphaAlongTrail(float t) {
		float middle = (float) Math.sin(Math.PI * t);
		return (1.0F - t) * (0.20F + 0.80F * middle);
	}

	private static void vertex(PoseStack poseStack, VertexConsumer consumer, Vec3 position, float alpha) {
		consumer.addVertex(poseStack.last().pose(), (float) position.x, (float) position.y, (float) position.z)
				.setColor(8, 232, 222, Math.max(0, Math.min(255, (int) (alpha * 255.0F))));
	}
}
