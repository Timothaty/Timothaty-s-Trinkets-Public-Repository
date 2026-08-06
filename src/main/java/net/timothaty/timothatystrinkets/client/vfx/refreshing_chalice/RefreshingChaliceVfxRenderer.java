package net.timothaty.timothatystrinkets.client.vfx.refreshing_chalice;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class RefreshingChaliceVfxRenderer {
	private static final int RED = 0xE0;
	private static final int GREEN = 0x1D;
	private static final int BLUE = 0x16;
	private static final int TRAIL_COUNT = 3;
	private static final int SEGMENTS = 20;
	private static final float BASE_WIDTH = 0.052F;
	private static final float BASE_ALPHA = 0.54F;
	private static final RenderType TRAIL_RENDER_TYPE = RenderType.create(
			"timothatys_trinkets_refreshing_chalice_trail",
			DefaultVertexFormat.POSITION_COLOR,
			VertexFormat.Mode.QUADS,
			768,
			false,
			true,
			RenderType.CompositeState.builder()
					.setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
					.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
					.setCullState(RenderStateShard.NO_CULL)
					.setLightmapState(RenderStateShard.NO_LIGHTMAP)
					.createCompositeState(false)
	);

	private RefreshingChaliceVfxRenderer() {
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		RefreshingChaliceVfxHandler.tick();
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;
		if (RefreshingChaliceVfxHandler.effects().isEmpty())
			return;

		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null)
			return;

		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		Camera camera = event.getCamera();
		Vec3 cameraPosition = camera.getPosition();
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
		VertexConsumer consumer = bufferSource.getBuffer(TRAIL_RENDER_TYPE);
		boolean renderedAny = false;

		poseStack.pushPose();
		poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
		for (RefreshingChaliceVfxHandler.Effect effect : RefreshingChaliceVfxHandler.effects()) {
			Entity entity = level.getEntity(effect.entityId());
			if (entity instanceof LivingEntity living && living.isAlive()) {
				renderedAny |= renderEffect(poseStack, consumer, cameraPosition, living, effect, partialTick);
			}
		}
		poseStack.popPose();

		if (renderedAny) {
			bufferSource.endBatch(TRAIL_RENDER_TYPE);
		}
	}

	private static boolean renderEffect(PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPosition, LivingEntity living, RefreshingChaliceVfxHandler.Effect effect, float partialTick) {
		float fade = effect.fade(partialTick);
		if (fade <= 0.002F)
			return false;

		float visibleProgress = RefreshingChaliceVfxHandler.smoothstep(0.0F, 0.22F, effect.progress(partialTick));
		if (visibleProgress <= 0.002F)
			return false;

		Vec3 center = new Vec3(
				Mth.lerp(partialTick, living.xo, living.getX()),
				Mth.lerp(partialTick, living.yo, living.getY()),
				Mth.lerp(partialTick, living.zo, living.getZ())
		);

		float time = living.tickCount + partialTick;
		for (int trail = 0; trail < TRAIL_COUNT; trail++) {
			float trailPhase = effect.phase() + (float) (trail * Math.PI * 2.0D / TRAIL_COUNT);
			renderTrail(poseStack, consumer, cameraPosition, center, living.getBbWidth(), living.getBbHeight(), time, trailPhase, fade, visibleProgress);
		}
		return true;
	}

	private static void renderTrail(PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPosition, Vec3 center, float entityWidth, float entityHeight, float time, float trailPhase, float fade, float visibleProgress) {
		int visibleSegments = Math.max(1, (int) Math.ceil(SEGMENTS * visibleProgress));
		int sampleCount = visibleSegments + 1;
		Vec3[] points = new Vec3[sampleCount];
		Vec3[] sides = new Vec3[sampleCount];
		float[] widths = new float[sampleCount];
		float[] alphas = new float[sampleCount];
		float[] progress = new float[sampleCount];

		double radiusBase = Math.max(0.42D, entityWidth * 0.92D);
		double orbit = time * 0.18D + trailPhase;
		for (int i = 0; i < sampleCount; i++) {
			float t = i == sampleCount - 1 ? visibleProgress : i / (float) SEGMENTS;
			progress[i] = t;

			double angle = orbit + t * Math.PI * 2.0D * 1.42D;
			double radius = radiusBase + Math.sin(t * Math.PI) * 0.10D;
			double x = center.x + Math.cos(angle) * radius;
			double y = center.y + 0.18D + entityHeight * (0.12D + 0.76D * t) + Math.sin(time * 0.11D + trailPhase + t * Math.PI * 2.0D) * 0.045D;
			double z = center.z + Math.sin(angle) * radius;

			points[i] = new Vec3(x, y, z);
			widths[i] = BASE_WIDTH * widthAlongTrail(t) * fade;
			alphas[i] = BASE_ALPHA * alphaAlongTrail(t) * fade;
		}

		for (int i = 0; i < sampleCount; i++) {
			Vec3 previous = points[Math.max(0, i - 1)];
			Vec3 next = points[Math.min(sampleCount - 1, i + 1)];
			Vec3 tangent = next.subtract(previous);
			if (tangent.lengthSqr() < 0.00001D) {
				sides[i] = new Vec3(1.0D, 0.0D, 0.0D);
				continue;
			}

			Vec3 toCamera = cameraPosition.subtract(points[i]);
			Vec3 side = tangent.cross(toCamera).normalize();
			sides[i] = side.lengthSqr() < 0.00001D ? new Vec3(1.0D, 0.0D, 0.0D) : side;
		}

		for (int i = 0; i < visibleSegments; i++) {
			if (progress[i + 1] <= progress[i])
				continue;

			Vec3 start = points[i];
			Vec3 end = points[i + 1];
			if (end.subtract(start).lengthSqr() < 0.00001D)
				continue;

			Vec3 s0 = start.add(sides[i].scale(widths[i]));
			Vec3 s1 = start.subtract(sides[i].scale(widths[i]));
			Vec3 e0 = end.add(sides[i + 1].scale(widths[i + 1]));
			Vec3 e1 = end.subtract(sides[i + 1].scale(widths[i + 1]));

			vertex(poseStack, consumer, s0, alphas[i]);
			vertex(poseStack, consumer, e0, alphas[i + 1]);
			vertex(poseStack, consumer, e1, alphas[i + 1]);
			vertex(poseStack, consumer, s1, alphas[i]);
		}
	}

	private static float widthAlongTrail(float t) {
		float middle = (float) Math.sin(Math.PI * t);
		float root = Mth.clamp(t / 0.12F, 0.0F, 1.0F);
		float tip = Mth.clamp((1.0F - t) / 0.16F, 0.0F, 1.0F);
		return (0.10F + 0.90F * middle) * root * tip;
	}

	private static float alphaAlongTrail(float t) {
		float middle = (float) Math.sin(Math.PI * t);
		float root = Mth.clamp(t / 0.10F, 0.0F, 1.0F);
		float tip = Mth.clamp((1.0F - t) / 0.22F, 0.0F, 1.0F);
		return root * tip * (0.18F + 0.82F * middle);
	}

	private static void vertex(PoseStack poseStack, VertexConsumer consumer, Vec3 position, float alpha) {
		consumer.addVertex(poseStack.last().pose(), (float) position.x, (float) position.y, (float) position.z)
				.setColor(RED, GREEN, BLUE, Mth.clamp((int) (alpha * 255.0F), 0, 255));
	}
}
