package net.timothaty.timothatystrinkets.client.vfx.spark;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.vfx.spark.SparkTrailHandler.SparkTrail;
import net.timothaty.timothatystrinkets.client.vfx.spark.SparkTrailHandler.TrailPoint;

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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class SparkTrailRenderer {
	private static final float OUTER_WIDTH = 0.018F;
	private static final float CORE_WIDTH = 0.006F;
	private static final RenderType TRAIL_RENDER_TYPE = RenderType.create(
			"timothatys_trinkets_spark_trail",
			DefaultVertexFormat.POSITION_COLOR,
			VertexFormat.Mode.QUADS,
			512,
			false,
			true,
			RenderType.CompositeState.builder()
					.setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
					.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
					.setCullState(RenderStateShard.NO_CULL)
					.setLightmapState(RenderStateShard.NO_LIGHTMAP)
					.createCompositeState(false));

	private SparkTrailRenderer() {
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		SparkTrailHandler.tick();
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || SparkTrailHandler.trails().isEmpty()) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		Camera camera = minecraft.gameRenderer.getMainCamera();
		Vec3 cameraPosition = camera.getPosition();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
		VertexConsumer consumer = bufferSource.getBuffer(TRAIL_RENDER_TYPE);

		poseStack.pushPose();
		poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
		for (SparkTrail trail : SparkTrailHandler.trails()) {
			renderTrail(poseStack, consumer, trail, cameraPosition, partialTick);
		}
		poseStack.popPose();
		bufferSource.endBatch(TRAIL_RENDER_TYPE);
	}

	private static void renderTrail(PoseStack poseStack, VertexConsumer consumer, SparkTrail trail, Vec3 cameraPosition, float partialTick) {
		int pointCount = trail.points().size();
		if (pointCount < 2) {
			return;
		}

		Iterator<TrailPoint> iterator = trail.points().iterator();
		TrailPoint startPoint = iterator.next();
		int index = 0;
		int segmentCount = pointCount - 1;
		while (iterator.hasNext()) {
			TrailPoint endPoint = iterator.next();
			renderSegmentPair(poseStack, consumer, trail, cameraPosition, partialTick, startPoint, endPoint, index, segmentCount);
			startPoint = endPoint;
			index++;
		}
	}

	private static void renderSegmentPair(PoseStack poseStack, VertexConsumer consumer, SparkTrail trail, Vec3 cameraPosition,
			float partialTick, TrailPoint startPoint, TrailPoint endPoint, int index, int segmentCount) {
		double startX = startPoint.interpolatedX(partialTick);
		double startY = startPoint.interpolatedY(partialTick);
		double startZ = startPoint.interpolatedZ(partialTick);
		double endX = endPoint.interpolatedX(partialTick);
		double endY = endPoint.interpolatedY(partialTick);
		double endZ = endPoint.interpolatedZ(partialTick);
		double segmentX = endX - startX;
		double segmentY = endY - startY;
		double segmentZ = endZ - startZ;
		if (segmentX * segmentX + segmentY * segmentY + segmentZ * segmentZ < 0.00001D) {
			return;
		}

		float startProgress = index / (float) segmentCount;
		float endProgress = (index + 1) / (float) segmentCount;
		float startAlpha = trailAlpha(startProgress, startPoint.age(), partialTick);
		float endAlpha = trailAlpha(endProgress, endPoint.age(), partialTick);
		if (startAlpha <= 0.002F && endAlpha <= 0.002F) {
			return;
		}

		double midpointX = (startX + endX) * 0.5D;
		double midpointY = (startY + endY) * 0.5D;
		double midpointZ = (startZ + endZ) * 0.5D;
		double cameraOffsetX = cameraPosition.x - midpointX;
		double cameraOffsetY = cameraPosition.y - midpointY;
		double cameraOffsetZ = cameraPosition.z - midpointZ;
		double sideX = segmentY * cameraOffsetZ - segmentZ * cameraOffsetY;
		double sideY = segmentZ * cameraOffsetX - segmentX * cameraOffsetZ;
		double sideZ = segmentX * cameraOffsetY - segmentY * cameraOffsetX;
		double sideLengthSqr = sideX * sideX + sideY * sideY + sideZ * sideZ;
		if (sideLengthSqr < 0.00001D) {
			sideX = -segmentZ;
			sideY = 0.0D;
			sideZ = segmentX;
			sideLengthSqr = sideX * sideX + sideZ * sideZ;
		}
		if (sideLengthSqr < 0.00001D) {
			sideX = 1.0D;
			sideY = 0.0D;
			sideZ = 0.0D;
		} else {
			double inverseSideLength = 1.0D / Math.sqrt(sideLengthSqr);
			sideX *= inverseSideLength;
			sideY *= inverseSideLength;
			sideZ *= inverseSideLength;
		}

		drawSegment(poseStack, consumer, trail, startX, startY, startZ, endX, endY, endZ, sideX, sideY, sideZ,
				OUTER_WIDTH, startProgress, endProgress, startAlpha * 0.18F, endAlpha * 0.18F);
		drawSegment(poseStack, consumer, trail, startX, startY, startZ, endX, endY, endZ, sideX, sideY, sideZ,
				CORE_WIDTH, startProgress, endProgress, startAlpha * 0.78F, endAlpha * 0.78F);
	}

	private static float trailAlpha(float progress, int age, float partialTick) {
		float historyFade = Mth.clamp(progress * 1.35F, 0.0F, 1.0F);
		float ageFade = 1.0F - Mth.clamp((age + partialTick) / (float) SparkTrailHandler.MAX_POINT_AGE, 0.0F, 1.0F);
		return historyFade * ageFade;
	}

	private static float widthAlongTrail(float progress) {
		float headBias = (float) Math.sin(Mth.clamp(progress, 0.0F, 1.0F) * Math.PI * 0.5D);
		return 0.12F + 0.88F * headBias;
	}

	private static void drawSegment(PoseStack poseStack, VertexConsumer consumer, SparkTrail trail,
			double startX, double startY, double startZ, double endX, double endY, double endZ,
			double sideX, double sideY, double sideZ, float baseWidth, float startProgress, float endProgress,
			float startAlpha, float endAlpha) {
		float startWidth = baseWidth * widthAlongTrail(startProgress);
		float endWidth = baseWidth * widthAlongTrail(endProgress);
		double startOffsetX = sideX * startWidth;
		double startOffsetY = sideY * startWidth;
		double startOffsetZ = sideZ * startWidth;
		double endOffsetX = sideX * endWidth;
		double endOffsetY = sideY * endWidth;
		double endOffsetZ = sideZ * endWidth;

		vertex(poseStack, consumer, trail, startX + startOffsetX, startY + startOffsetY, startZ + startOffsetZ, startAlpha);
		vertex(poseStack, consumer, trail, endX + endOffsetX, endY + endOffsetY, endZ + endOffsetZ, endAlpha);
		vertex(poseStack, consumer, trail, endX - endOffsetX, endY - endOffsetY, endZ - endOffsetZ, endAlpha);
		vertex(poseStack, consumer, trail, startX - startOffsetX, startY - startOffsetY, startZ - startOffsetZ, startAlpha);
	}

	private static void vertex(PoseStack poseStack, VertexConsumer consumer, SparkTrail trail, double x, double y, double z, float alpha) {
		consumer.addVertex(poseStack.last().pose(), (float) x, (float) y, (float) z)
				.setColor(trail.red(), trail.green(), trail.blue(), Math.max(0, Math.min(255, Math.round(alpha * 255.0F))));
	}
}
