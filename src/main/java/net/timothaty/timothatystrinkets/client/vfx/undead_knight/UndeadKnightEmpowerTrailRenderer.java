package net.timothaty.timothatystrinkets.client.vfx.undead_knight;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.entity.UndeadKnightEntity;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class UndeadKnightEmpowerTrailRenderer {
	private static final float START_TICK = 10.8F;
	private static final float END_TICK = 20.0F;
	private static final float FADE_IN_PORTION = 0.38F;
	private static final float FADE_OUT_PORTION = 0.50F;
	private static final float LINGER_TICKS = 8.0F;
	private static final float MIN_VISIBLE_FADE = 0.015F;
	private static final int SPIRAL_SEGMENTS = 24;
	private static final int TRAIL_COUNT = 3;
	private static final int RED = 0x8C;
	private static final int GREEN = 0xFF;
	private static final int BLUE = 0xD7;
	private static final RenderType TRAIL_RENDER_TYPE = RenderType.create(
		"timothatys_trinkets_undead_knight_empower_trail",
		DefaultVertexFormat.POSITION_COLOR,
		VertexFormat.Mode.QUADS,
		2048,
		false,
		true,
		RenderType.CompositeState.builder()
			.setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
			.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
			.setCullState(RenderStateShard.NO_CULL)
			.setLightmapState(RenderStateShard.NO_LIGHTMAP)
			.createCompositeState(false));
	private static final Map<UndeadKnightEntity, TrailState> TRAIL_STATES = new WeakHashMap<>();

	private UndeadKnightEmpowerTrailRenderer() {
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return;

		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		Camera camera = minecraft.gameRenderer.getMainCamera();
		Vec3 cameraPosition = camera.getPosition();
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
		VertexConsumer consumer = bufferSource.getBuffer(TRAIL_RENDER_TYPE);
		double renderTime = minecraft.level.getGameTime() + partialTick;

		poseStack.pushPose();
		poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
		for (Entity entity : minecraft.level.entitiesForRendering()) {
			if (entity instanceof UndeadKnightEntity knight && knight.isEmpowering()) {
				TrailState state = TRAIL_STATES.computeIfAbsent(knight, ignored -> new TrailState());
				float elapsedTicks = knight.getEmpowerTicks() + partialTick;
				float timelineFade = timelineFade(elapsedTicks);
				state.fadeOutStartTime = -1.0D;
				if (timelineFade > MIN_VISIBLE_FADE) {
					state.lastTrailFade = timelineFade;
					state.lastTrailTime = knight.tickCount + partialTick;
					renderKnightTrail(poseStack, consumer, cameraPosition, knight, partialTick, timelineFade, state.lastTrailTime);
				}
			} else if (entity instanceof UndeadKnightEntity knight) {
				renderLingeringTrail(poseStack, consumer, cameraPosition, knight, partialTick, renderTime);
			}
		}
		poseStack.popPose();
		bufferSource.endBatch(TRAIL_RENDER_TYPE);
	}

	private static void renderLingeringTrail(PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPosition, UndeadKnightEntity knight, float partialTick, double renderTime) {
		TrailState state = TRAIL_STATES.get(knight);
		if (state == null)
			return;

		if (state.fadeOutStartTime < 0.0D) {
			state.fadeOutStartTime = renderTime;
		}

		float linger = (float) (renderTime - state.fadeOutStartTime);
		if (knight.isRemoved() || linger > LINGER_TICKS || state.lastTrailFade <= MIN_VISIBLE_FADE) {
			TRAIL_STATES.remove(knight);
			return;
		}

		float lingerFade = 1.0F - smoothStep(Mth.clamp(linger / LINGER_TICKS, 0.0F, 1.0F));
		float fade = state.lastTrailFade * lingerFade;
		if (fade <= MIN_VISIBLE_FADE)
			return;

		renderKnightTrail(poseStack, consumer, cameraPosition, knight, partialTick, fade, state.lastTrailTime);
	}

	private static void renderKnightTrail(PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPosition, UndeadKnightEntity knight, float partialTick, float fade, float trailTime) {
		if (fade <= MIN_VISIBLE_FADE)
			return;

		float alpha = 0.78F * fade;
		if (alpha <= MIN_VISIBLE_FADE)
			return;

		double x = Mth.lerp(partialTick, knight.xo, knight.getX());
		double y = Mth.lerp(partialTick, knight.yo, knight.getY());
		double z = Mth.lerp(partialTick, knight.zo, knight.getZ());
		Vec3 center = new Vec3(x, y + knight.getBbHeight() * 0.54D, z);
		double radius = Math.max(0.38D, knight.getBbWidth() * 0.82D);
		double height = knight.getBbHeight() * 0.92D;

		for (int i = 0; i < TRAIL_COUNT; i++) {
			float phase = (float) (i * Math.PI * 2.0D / TRAIL_COUNT);
			renderSpiralTrail(poseStack, consumer, cameraPosition, center, trailTime, phase, radius, height, alpha);
		}
	}

	private static float timelineFade(float elapsedTicks) {
		if (elapsedTicks < START_TICK || elapsedTicks > END_TICK)
			return 0.0F;

		float progress = Mth.clamp((elapsedTicks - START_TICK) / (END_TICK - START_TICK), 0.0F, 1.0F);
		return smoothStep(Mth.clamp(progress / FADE_IN_PORTION, 0.0F, 1.0F)) * smoothStep(Mth.clamp((1.0F - progress) / FADE_OUT_PORTION, 0.0F, 1.0F));
	}

	private static void renderSpiralTrail(PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPosition, Vec3 center,
			float time, float phase, double radius, double height, float alphaScale) {
		Vec3[] points = new Vec3[SPIRAL_SEGMENTS + 1];
		Vec3[] sides = new Vec3[SPIRAL_SEGMENTS + 1];
		float[] widths = new float[SPIRAL_SEGMENTS + 1];
		float[] alphas = new float[SPIRAL_SEGMENTS + 1];
		double arc = Math.PI * 2.35D;
		double spin = time * 0.36D + phase;

		for (int i = 0; i <= SPIRAL_SEGMENTS; i++) {
			float t = i / (float) SPIRAL_SEGMENTS;
			double angle = spin + arc * t;
			double middle = Math.sin(Math.PI * t);
			double localRadius = radius * (0.74D + 0.16D * middle + 0.04D * Math.sin(time * 0.25D + t * Math.PI * 3.0D));
			double yOffset = -height * 0.48D + height * t;
			points[i] = center.add(Math.cos(angle) * localRadius, yOffset, Math.sin(angle) * localRadius);
			widths[i] = 0.062F * widthAlongTrail(t) * alphaScale;
			alphas[i] = alphaScale * alphaAlongTrail(t);
		}

		for (int i = 0; i <= SPIRAL_SEGMENTS; i++) {
			Vec3 previous = points[Math.max(0, i - 1)];
			Vec3 next = points[Math.min(SPIRAL_SEGMENTS, i + 1)];
			Vec3 tangent = next.subtract(previous);
			if (tangent.lengthSqr() < 0.00001D) {
				sides[i] = new Vec3(1.0D, 0.0D, 0.0D);
				continue;
			}
			Vec3 side = tangent.cross(cameraPosition.subtract(points[i])).normalize();
			sides[i] = side.lengthSqr() < 0.00001D ? new Vec3(1.0D, 0.0D, 0.0D) : side;
		}

		for (int i = 0; i < SPIRAL_SEGMENTS; i++) {
			drawSegment(poseStack, consumer, points[i], points[i + 1], sides[i], sides[i + 1], widths[i], widths[i + 1], alphas[i], alphas[i + 1]);
		}
	}

	private static float widthAlongTrail(float t) {
		return (0.10F + 0.90F * (float) Math.sin(Math.PI * t)) * Mth.clamp(t / 0.12F, 0.0F, 1.0F) * Mth.clamp((1.0F - t) / 0.16F, 0.0F, 1.0F);
	}

	private static float alphaAlongTrail(float t) {
		return (0.18F + 0.82F * (float) Math.sin(Math.PI * t)) * Mth.clamp(t / 0.10F, 0.0F, 1.0F) * Mth.clamp((1.0F - t) / 0.24F, 0.0F, 1.0F);
	}

	private static float smoothStep(float value) {
		float x = Mth.clamp(value, 0.0F, 1.0F);
		return x * x * (3.0F - 2.0F * x);
	}

	private static void drawSegment(PoseStack poseStack, VertexConsumer consumer, Vec3 start, Vec3 end, Vec3 startSide, Vec3 endSide,
			float startWidth, float endWidth, float startAlpha, float endAlpha) {
		Vec3 s0 = start.add(startSide.scale(startWidth));
		Vec3 s1 = start.subtract(startSide.scale(startWidth));
		Vec3 e0 = end.add(endSide.scale(endWidth));
		Vec3 e1 = end.subtract(endSide.scale(endWidth));
		vertex(poseStack, consumer, s0, startAlpha);
		vertex(poseStack, consumer, e0, endAlpha);
		vertex(poseStack, consumer, e1, endAlpha);
		vertex(poseStack, consumer, s1, startAlpha);
	}

	private static void vertex(PoseStack poseStack, VertexConsumer consumer, Vec3 position, float alpha) {
		consumer.addVertex(poseStack.last().pose(), (float) position.x, (float) position.y, (float) position.z)
			.setColor(RED, GREEN, BLUE, Mth.clamp(Math.round(alpha * 255.0F), 0, 255));
	}

	private static final class TrailState {
		private float lastTrailFade;
		private float lastTrailTime;
		private double fadeOutStartTime = -1.0D;
	}
}
