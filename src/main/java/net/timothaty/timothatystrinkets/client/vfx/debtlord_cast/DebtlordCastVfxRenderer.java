package net.timothaty.timothatystrinkets.client.vfx.debtlord_cast;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.vfx.debtlord_cast.DebtlordCastVfxHandler.CastVfx;
import net.timothaty.timothatystrinkets.client.vfx.debtlord_cast.DebtlordCastVfxHandler.TrailPoint;

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
public final class DebtlordCastVfxRenderer {
	private static final int ORBIT_SEGMENTS = 16;
	private static final RenderType CAST_VFX_RENDER_TYPE = RenderType.create(
		"timothatys_trinkets_debtlord_cast_vfx",
		DefaultVertexFormat.POSITION_COLOR,
		VertexFormat.Mode.QUADS,
		1536,
		false,
		true,
		RenderType.CompositeState.builder()
			.setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
			.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
			.setCullState(RenderStateShard.NO_CULL)
			.setLightmapState(RenderStateShard.NO_LIGHTMAP)
			.createCompositeState(false));

	private DebtlordCastVfxRenderer() {
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		DebtlordCastVfxHandler.tick();
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || DebtlordCastVfxHandler.activeVfx().isEmpty())
			return;

		Minecraft minecraft = Minecraft.getInstance();
		Camera camera = minecraft.gameRenderer.getMainCamera();
		Vec3 cameraPosition = camera.getPosition();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		float time = minecraft.level == null ? 0.0F : minecraft.level.getGameTime() + partialTick;
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
		VertexConsumer consumer = bufferSource.getBuffer(CAST_VFX_RENDER_TYPE);

		poseStack.pushPose();
		poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
		for (CastVfx vfx : DebtlordCastVfxHandler.activeVfx()) {
			if (vfx.chainWarning())
				renderOrbit(poseStack, consumer, cameraPosition, vfx.leftArmPosition(), time, 0.34D, 0.48D, 2, 205, 0, 42, 0.86F);
			if (vfx.desolation()) {
				renderOrbit(poseStack, consumer, cameraPosition, vfx.bodyCenter(), time * 0.82F, 1.25D, 2.35D, 4, 180, 0, 38, 0.72F);
				renderDarkTrail(poseStack, consumer, cameraPosition, vfx.darkTrail());
			}
			if (vfx.laserWarning()) {
				renderOrbit(poseStack, consumer, cameraPosition, vfx.laserWarningPosition(), time * 1.45F, 0.24D, 0.24D, 2, 255, 0, 48, 0.92F);
			}
			renderLaserWarningTrail(poseStack, consumer, cameraPosition, vfx.laserWarningTrail());
		}
		poseStack.popPose();
		bufferSource.endBatch(CAST_VFX_RENDER_TYPE);
	}

	private static void renderOrbit(PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPosition, Vec3 center,
			float time, double radius, double height, int trailCount, int red, int green, int blue, float alphaScale) {
		for (int trail = 0; trail < trailCount; trail++) {
			float phase = (float) (trail * Math.PI * 2.0D / trailCount);
			renderOrbitTrail(poseStack, consumer, cameraPosition, center, time, radius, height, phase, red, green, blue, alphaScale);
		}
	}

	private static void renderOrbitTrail(PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPosition, Vec3 center,
			float time, double radius, double height, float phase, int red, int green, int blue, float alphaScale) {
		Vec3[] points = new Vec3[ORBIT_SEGMENTS + 1];
		Vec3[] sides = new Vec3[ORBIT_SEGMENTS + 1];
		float[] widths = new float[ORBIT_SEGMENTS + 1];
		float[] alphas = new float[ORBIT_SEGMENTS + 1];
		double arc = Math.PI * 1.55D;
		double orbit = time * 0.28D + phase;

		for (int i = 0; i <= ORBIT_SEGMENTS; i++) {
			float t = i / (float) ORBIT_SEGMENTS;
			double angle = orbit + arc * t;
			double yOffset = -height * 0.5D + height * t;
			double pulse = Math.sin(t * Math.PI) * 0.08D;
			points[i] = center.add(Math.cos(angle) * (radius + pulse), yOffset, Math.sin(angle) * (radius + pulse));
			widths[i] = 0.08F * widthAlongTrail(t);
			alphas[i] = alphaScale * alphaAlongTrail(t);
		}

		for (int i = 0; i <= ORBIT_SEGMENTS; i++) {
			Vec3 previous = points[Math.max(0, i - 1)];
			Vec3 next = points[Math.min(ORBIT_SEGMENTS, i + 1)];
			Vec3 tangent = next.subtract(previous);
			if (tangent.lengthSqr() < 0.00001D) {
				sides[i] = new Vec3(1.0D, 0.0D, 0.0D);
				continue;
			}
			Vec3 side = tangent.cross(cameraPosition.subtract(points[i])).normalize();
			sides[i] = side.lengthSqr() < 0.00001D ? new Vec3(1.0D, 0.0D, 0.0D) : side;
		}

		for (int i = 0; i < ORBIT_SEGMENTS; i++)
			drawSegment(poseStack, consumer, points[i], points[i + 1], sides[i], sides[i + 1], widths[i], widths[i + 1], alphas[i], alphas[i + 1], red, green, blue);
	}

	private static void renderDarkTrail(PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPosition, Deque<TrailPoint> trail) {
		renderTrail(poseStack, consumer, cameraPosition, trail, 0.055F, 0.78F, DebtlordCastVfxHandler.DARK_TRAIL_MAX_AGE, 34, 3, 46);
	}

	private static void renderLaserWarningTrail(PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPosition, Deque<TrailPoint> trail) {
		renderTrail(poseStack, consumer, cameraPosition, trail, 0.065F, 0.92F, DebtlordCastVfxHandler.LASER_WARNING_TRAIL_MAX_AGE, 255, 12, 46);
	}

	private static void renderTrail(PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPosition, Deque<TrailPoint> trail,
			float baseWidth, float alphaScale, int maxAge, int red, int green, int blue) {
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

			Vec3 midpoint = start.add(end).scale(0.5D);
			Vec3 side = direction.cross(cameraPosition.subtract(midpoint)).normalize();
			if (side.lengthSqr() < 0.00001D)
				side = new Vec3(1.0D, 0.0D, 0.0D);

			float startAlpha = trailAlpha(i / (float) (points.size() - 1), startPoint.age(), maxAge) * alphaScale;
			float endAlpha = trailAlpha((i + 1) / (float) (points.size() - 1), endPoint.age(), maxAge) * alphaScale;
			float startWidth = baseWidth * Math.max(0.2F, startAlpha);
			float endWidth = baseWidth * Math.max(0.2F, endAlpha);
			drawSegment(poseStack, consumer, start, end, side, side, startWidth, endWidth, startAlpha, endAlpha, red, green, blue);
		}
	}

	private static float widthAlongTrail(float t) {
		return (0.12F + 0.88F * (float) Math.sin(Math.PI * t)) * Mth.clamp(t / 0.10F, 0.0F, 1.0F) * Mth.clamp((1.0F - t) / 0.18F, 0.0F, 1.0F);
	}

	private static float alphaAlongTrail(float t) {
		return Mth.clamp(t / 0.12F, 0.0F, 1.0F) * Mth.clamp((1.0F - t) / 0.22F, 0.0F, 1.0F) * (0.25F + 0.75F * (float) Math.sin(Math.PI * t));
	}

	private static float trailAlpha(float progress, int age, int maxAge) {
		float historyFade = Mth.clamp(progress * 1.25F, 0.0F, 1.0F);
		float ageFade = 1.0F - Mth.clamp(age / (float) maxAge, 0.0F, 1.0F);
		return historyFade * ageFade;
	}

	private static void drawSegment(PoseStack poseStack, VertexConsumer consumer, Vec3 start, Vec3 end, Vec3 startSide, Vec3 endSide,
			float startWidth, float endWidth, float startAlpha, float endAlpha, int red, int green, int blue) {
		Vec3 s0 = start.add(startSide.scale(startWidth));
		Vec3 s1 = start.subtract(startSide.scale(startWidth));
		Vec3 e0 = end.add(endSide.scale(endWidth));
		Vec3 e1 = end.subtract(endSide.scale(endWidth));
		vertex(poseStack, consumer, s0, red, green, blue, startAlpha);
		vertex(poseStack, consumer, e0, red, green, blue, endAlpha);
		vertex(poseStack, consumer, e1, red, green, blue, endAlpha);
		vertex(poseStack, consumer, s1, red, green, blue, startAlpha);
	}

	private static void vertex(PoseStack poseStack, VertexConsumer consumer, Vec3 position, int red, int green, int blue, float alpha) {
		consumer.addVertex(poseStack.last().pose(), (float) position.x, (float) position.y, (float) position.z)
			.setColor(red, green, blue, Mth.clamp(Math.round(alpha * 255.0F), 0, 255));
	}
}
