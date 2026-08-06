package net.timothaty.timothatystrinkets.client.vfx.debtlord_chains;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.vfx.debtlord_chains.DebtlordChainVisualHandler.ChainVisual;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class DebtlordChainVisualRenderer {
	private static final ResourceLocation CHAIN_TEXTURE = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/particle/debtlord_chain.png");
	private static final double CHAIN_SEGMENT_LENGTH = 0.34D;
	private static final float CHAIN_WIDTH = 0.16F;
	private static final int BIND_TRAIL_COUNT = 4;
	private static final int BIND_TRAIL_SEGMENTS = 18;
	private static final RenderType CHAIN_RENDER_TYPE = RenderType.entityTranslucent(CHAIN_TEXTURE);
	private static final RenderType BIND_TRAIL_RENDER_TYPE = RenderType.create(
		"timothatys_trinkets_debtlord_chain_bind_trail",
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

	private DebtlordChainVisualRenderer() {
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		DebtlordChainVisualHandler.tick();
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || DebtlordChainVisualHandler.chains().isEmpty())
			return;

		Minecraft minecraft = Minecraft.getInstance();
		Camera camera = minecraft.gameRenderer.getMainCamera();
		Vec3 cameraPosition = camera.getPosition();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		float time = minecraft.level == null ? 0.0F : minecraft.level.getGameTime() + partialTick;
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

		poseStack.pushPose();
		poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

		VertexConsumer chainConsumer = bufferSource.getBuffer(CHAIN_RENDER_TYPE);
		for (ChainVisual chain : DebtlordChainVisualHandler.chains()) {
			renderChain(poseStack, chainConsumer, cameraPosition, chain.firstSource(), chain.firstTarget(), time, 0, chain.launchProgress());
			renderChain(poseStack, chainConsumer, cameraPosition, chain.secondSource(), chain.secondTarget(), time, 1, chain.launchProgress());
		}
		bufferSource.endBatch(CHAIN_RENDER_TYPE);

		VertexConsumer trailConsumer = bufferSource.getBuffer(BIND_TRAIL_RENDER_TYPE);
		boolean renderedBinding = false;
		for (ChainVisual chain : DebtlordChainVisualHandler.chains()) {
			if (chain.bound()) {
				renderBindingTrails(poseStack, trailConsumer, cameraPosition, chain, time);
				renderedBinding = true;
			}
		}
		if (renderedBinding)
			bufferSource.endBatch(BIND_TRAIL_RENDER_TYPE);

		poseStack.popPose();
	}

	private static void renderChain(PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPosition, Vec3 start, Vec3 end, float time, int chainIndex, float launchProgress) {
		if (launchProgress <= 0.01F)
			return;

		Vec3 visibleEnd = start.lerp(end, Mth.clamp(launchProgress, 0.0F, 1.0F));
		end = visibleEnd;
		double length = start.distanceTo(end);
		if (length < 0.05D)
			return;

		int segments = Math.max(2, (int) Math.ceil(length / CHAIN_SEGMENT_LENGTH));
		for (int i = 0; i < segments; i++) {
			float t0 = i / (float) segments;
			float t1 = (i + 1) / (float) segments;
			Vec3 p0 = chainPoint(start, end, t0, time, chainIndex);
			Vec3 p1 = chainPoint(start, end, t1, time, chainIndex);
			Vec3 direction = p1.subtract(p0);
			if (direction.lengthSqr() < 0.00001D)
				continue;

			Vec3 midpoint = p0.add(p1).scale(0.5D);
			Vec3 side = direction.cross(cameraPosition.subtract(midpoint)).normalize();
			if (side.lengthSqr() < 0.00001D)
				side = new Vec3(1.0D, 0.0D, 0.0D);

			Vec3 offset = side.scale(CHAIN_WIDTH);
			chainVertex(poseStack, consumer, p0.add(offset), 0.0F, 0.0F);
			chainVertex(poseStack, consumer, p1.add(offset), 0.0F, 1.0F);
			chainVertex(poseStack, consumer, p1.subtract(offset), 1.0F, 1.0F);
			chainVertex(poseStack, consumer, p0.subtract(offset), 1.0F, 0.0F);
		}
	}

	private static Vec3 chainPoint(Vec3 start, Vec3 end, float t, float time, int chainIndex) {
		Vec3 base = start.lerp(end, t);
		Vec3 direction = end.subtract(start);
		Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
		Vec3 side = horizontal.lengthSqr() < 0.0001D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(-horizontal.z, 0.0D, horizontal.x).normalize();
		double sag = Math.sin(Math.PI * t) * Mth.clamp(direction.length() * 0.045D, 0.08D, 0.48D);
		double sway = Math.sin(time * 0.18D + chainIndex * 1.9D + t * Math.PI * 3.0D) * Math.sin(Math.PI * t) * 0.045D;
		return base.add(side.scale(sway)).add(0.0D, -sag, 0.0D);
	}

	private static void renderBindingTrails(PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPosition, ChainVisual chain, float time) {
		Vec3 center = chain.boundCenter();
		double radiusBase = Math.max(0.58D, chain.boundWidth() * 1.15D);
		for (int trail = 0; trail < BIND_TRAIL_COUNT; trail++) {
			float phase = (float) (trail * Math.PI * 2.0D / BIND_TRAIL_COUNT);
			renderBindingTrail(poseStack, consumer, cameraPosition, center, radiusBase, chain.boundHeight(), time, phase);
		}
	}

	private static void renderBindingTrail(PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPosition, Vec3 center, double radiusBase, float targetHeight, float time, float phase) {
		Vec3[] points = new Vec3[BIND_TRAIL_SEGMENTS + 1];
		Vec3[] sides = new Vec3[BIND_TRAIL_SEGMENTS + 1];
		float[] widths = new float[BIND_TRAIL_SEGMENTS + 1];
		float[] alphas = new float[BIND_TRAIL_SEGMENTS + 1];
		double arc = Math.PI * 1.65D;
		double orbit = time * 0.18D + phase;

		for (int i = 0; i <= BIND_TRAIL_SEGMENTS; i++) {
			float t = i / (float) BIND_TRAIL_SEGMENTS;
			double angle = orbit + arc * t;
			double radius = radiusBase + Math.sin(t * Math.PI) * 0.12D;
			double x = center.x + Math.cos(angle) * radius;
			double z = center.z + Math.sin(angle) * radius;
			double y = center.y - targetHeight * 0.42D + targetHeight * 0.86D * t + Math.sin(time * 0.14D + phase + t * Math.PI * 2.0D) * 0.06D;
			points[i] = new Vec3(x, y, z);
			widths[i] = 0.075F * widthAlongTrail(t);
			alphas[i] = 0.84F * alphaAlongTrail(t);
		}

		for (int i = 0; i <= BIND_TRAIL_SEGMENTS; i++) {
			Vec3 previous = points[Math.max(0, i - 1)];
			Vec3 next = points[Math.min(BIND_TRAIL_SEGMENTS, i + 1)];
			Vec3 tangent = next.subtract(previous);
			if (tangent.lengthSqr() < 0.00001D) {
				sides[i] = new Vec3(1.0D, 0.0D, 0.0D);
				continue;
			}
			Vec3 side = tangent.cross(cameraPosition.subtract(points[i])).normalize();
			sides[i] = side.lengthSqr() < 0.00001D ? new Vec3(1.0D, 0.0D, 0.0D) : side;
		}

		for (int i = 0; i < BIND_TRAIL_SEGMENTS; i++) {
			Vec3 s0 = points[i].add(sides[i].scale(widths[i]));
			Vec3 s1 = points[i].subtract(sides[i].scale(widths[i]));
			Vec3 e0 = points[i + 1].add(sides[i + 1].scale(widths[i + 1]));
			Vec3 e1 = points[i + 1].subtract(sides[i + 1].scale(widths[i + 1]));
			trailVertex(poseStack, consumer, s0, alphas[i]);
			trailVertex(poseStack, consumer, e0, alphas[i + 1]);
			trailVertex(poseStack, consumer, e1, alphas[i + 1]);
			trailVertex(poseStack, consumer, s1, alphas[i]);
		}
	}

	private static float widthAlongTrail(float t) {
		return (0.12F + 0.88F * (float) Math.sin(Math.PI * t)) * Mth.clamp(t / 0.10F, 0.0F, 1.0F) * Mth.clamp((1.0F - t) / 0.16F, 0.0F, 1.0F);
	}

	private static float alphaAlongTrail(float t) {
		float middle = (float) Math.sin(Math.PI * t);
		return Mth.clamp(t / 0.12F, 0.0F, 1.0F) * Mth.clamp((1.0F - t) / 0.22F, 0.0F, 1.0F) * (0.20F + 0.80F * middle);
	}

	private static void chainVertex(PoseStack poseStack, VertexConsumer consumer, Vec3 position, float u, float v) {
		consumer.addVertex(poseStack.last().pose(), (float) position.x, (float) position.y, (float) position.z)
			.setColor(255, 255, 255, 238)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(LightTexture.FULL_BRIGHT)
			.setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
	}

	private static void trailVertex(PoseStack poseStack, VertexConsumer consumer, Vec3 position, float alpha) {
		consumer.addVertex(poseStack.last().pose(), (float) position.x, (float) position.y, (float) position.z)
			.setColor(190, 0, 28, Mth.clamp((int) (alpha * 255.0F), 0, 255));
	}
}
