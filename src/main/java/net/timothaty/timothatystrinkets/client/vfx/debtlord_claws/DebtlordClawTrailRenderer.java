package net.timothaty.timothatystrinkets.client.vfx.debtlord_claws;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.vfx.debtlord_claws.DebtlordClawTrailHandler.FingerTrails;
import net.timothaty.timothatystrinkets.client.vfx.debtlord_claws.DebtlordClawTrailHandler.TrailPoint;

import net.neoforged.api.distmarker.Dist;
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

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class DebtlordClawTrailRenderer {
	private static final float CORE_WIDTH = 0.035F;
	private static final RenderType TRAIL_RENDER_TYPE = RenderType.create(
		"timothatys_trinkets_debtlord_claw_trail",
		DefaultVertexFormat.POSITION_COLOR,
		VertexFormat.Mode.QUADS,
		1024,
		false,
		true,
		RenderType.CompositeState.builder()
			.setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
			.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
			.setCullState(RenderStateShard.NO_CULL)
			.setLightmapState(RenderStateShard.NO_LIGHTMAP)
			.createCompositeState(false));

	private DebtlordClawTrailRenderer() {
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		DebtlordClawTrailHandler.tick();
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || DebtlordClawTrailHandler.trails().isEmpty())
			return;

		Minecraft minecraft = Minecraft.getInstance();
		Camera camera = minecraft.gameRenderer.getMainCamera();
		Vec3 cameraPosition = camera.getPosition();
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
		VertexConsumer consumer = bufferSource.getBuffer(TRAIL_RENDER_TYPE);

		poseStack.pushPose();
		poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
		for (FingerTrails trails : DebtlordClawTrailHandler.trails()) {
			for (Deque<TrailPoint> finger : trails.fingers())
				renderFingerTrail(poseStack, consumer, finger, cameraPosition);
		}
		poseStack.popPose();
		bufferSource.endBatch(TRAIL_RENDER_TYPE);
	}

	private static void renderFingerTrail(PoseStack poseStack, VertexConsumer consumer, Deque<TrailPoint> trail, Vec3 cameraPosition) {
		if (trail.size() < 2)
			return;

		List<TrailPoint> points = new ArrayList<>(trail);
		for (int i = 0; i < points.size() - 1; i++) {
			TrailPoint startPoint = points.get(i);
			TrailPoint endPoint = points.get(i + 1);
			Vec3 start = startPoint.position();
			Vec3 end = endPoint.position();
			Vec3 direction = end.subtract(start);
			if (direction.lengthSqr() < 0.00001D)
				continue;

			float startProgress = i / (float) (points.size() - 1);
			float endProgress = (i + 1) / (float) (points.size() - 1);
			float startAlpha = trailAlpha(startProgress, startPoint.age());
			float endAlpha = trailAlpha(endProgress, endPoint.age());
			Vec3 midpoint = start.add(end).scale(0.5D);
			Vec3 side = direction.cross(cameraPosition.subtract(midpoint)).normalize();
			if (side.lengthSqr() < 0.00001D)
				side = new Vec3(1.0D, 0.0D, 0.0D);

			drawSegment(poseStack, consumer, start, end, side, CORE_WIDTH * 2.8F, startAlpha * 0.22F, endAlpha * 0.22F, 255, 0, 22);
			drawSegment(poseStack, consumer, start, end, side, CORE_WIDTH, startAlpha * 0.92F, endAlpha * 0.92F, 255, 35, 48);
		}
	}

	private static float trailAlpha(float progress, int age) {
		float historyFade = Mth.clamp(progress * 1.35F, 0.0F, 1.0F);
		float ageFade = 1.0F - Mth.clamp(age / (float) DebtlordClawTrailHandler.MAX_POINT_AGE, 0.0F, 1.0F);
		return historyFade * ageFade;
	}

	private static void drawSegment(PoseStack poseStack, VertexConsumer consumer, Vec3 start, Vec3 end, Vec3 side,
			float width, float startAlpha, float endAlpha, int red, int green, int blue) {
		Vec3 startOffset = side.scale(width * Math.max(0.15F, startAlpha));
		Vec3 endOffset = side.scale(width * Math.max(0.15F, endAlpha));
		vertex(poseStack, consumer, start.add(startOffset), red, green, blue, startAlpha);
		vertex(poseStack, consumer, end.add(endOffset), red, green, blue, endAlpha);
		vertex(poseStack, consumer, end.subtract(endOffset), red, green, blue, endAlpha);
		vertex(poseStack, consumer, start.subtract(startOffset), red, green, blue, startAlpha);
	}

	private static void vertex(PoseStack poseStack, VertexConsumer consumer, Vec3 position, int red, int green, int blue, float alpha) {
		consumer.addVertex(poseStack.last().pose(), (float) position.x, (float) position.y, (float) position.z)
			.setColor(red, green, blue, Math.max(0, Math.min(255, Math.round(alpha * 255.0F))));
	}
}
