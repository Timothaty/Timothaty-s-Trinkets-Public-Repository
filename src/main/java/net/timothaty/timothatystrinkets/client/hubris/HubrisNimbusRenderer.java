package net.timothaty.timothatystrinkets.client.hubris;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class HubrisNimbusRenderer {
	private static final ResourceLocation RING_TEXTURE = ResourceLocation.fromNamespaceAndPath(
			TimothatysTrinketsMod.MODID,
			"textures/particle/nimbus_of_prideful.png"
	);
	private static final ResourceLocation THORN_TEXTURE = ResourceLocation.fromNamespaceAndPath(
			TimothatysTrinketsMod.MODID,
			"textures/particle/prideful_thorn.png"
	);
	private static final RenderType RING_RENDER_TYPE = RenderType.entityTranslucent(RING_TEXTURE);
	private static final RenderType THORN_RENDER_TYPE = RenderType.entityTranslucent(THORN_TEXTURE);
	private static final float RING_HALF_SIZE = 0.39F;
	private static final float THORN_RADIUS = RING_HALF_SIZE - 0.01F;
	private static final float THORN_HALF_WIDTH = 0.05F;
	private static final float THORN_HALF_HEIGHT = 0.16F;
	private static final float THORN_U0 = 7.0F / 16.0F;
	private static final float THORN_U1 = 9.0F / 16.0F;
	private static final float THORN_V0 = 5.0F / 16.0F;
	private static final float THORN_V1 = 11.0F / 16.0F;
	private static final float EMISSIVE_STRENGTH = 0.3F;

	private HubrisNimbusRenderer() {
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || HubrisClientState.states().isEmpty())
			return;
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null)
			return;

		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		Vec3 camera = event.getCamera().getPosition();
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

		poseStack.pushPose();
		poseStack.translate(-camera.x, -camera.y, -camera.z);
		VertexConsumer ringConsumer = buffers.getBuffer(RING_RENDER_TYPE);
		boolean ringRendered = renderPass(level, poseStack, ringConsumer, partialTick, true);
		if (ringRendered)
			buffers.endBatch(RING_RENDER_TYPE);

		VertexConsumer thornConsumer = buffers.getBuffer(THORN_RENDER_TYPE);
		boolean thornsRendered = renderPass(level, poseStack, thornConsumer, partialTick, false);
		if (thornsRendered)
			buffers.endBatch(THORN_RENDER_TYPE);
		poseStack.popPose();
	}

	private static boolean renderPass(
			ClientLevel level,
			PoseStack poseStack,
			VertexConsumer consumer,
			float partialTick,
			boolean ring
	) {
		boolean rendered = false;
		for (HubrisClientState.NimbusState state : HubrisClientState.states()) {
			Entity rawEntity = level.getEntity(state.entityId());
			if (!(rawEntity instanceof LivingEntity entity) || !entity.isAlive())
				continue;
			float alpha = state.alpha(partialTick);
			if (alpha <= 0.002F)
				continue;

			double x = Mth.lerp(partialTick, entity.xo, entity.getX());
			double y = Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getBbHeight() + 0.15D + state.bob(partialTick);
			double z = Mth.lerp(partialTick, entity.zo, entity.getZ());
			int light = applySoftEmissiveLight(
					LevelRenderer.getLightColor(level, entity.blockPosition().above(2))
			);

			poseStack.pushPose();
			poseStack.translate(x, y, z);
			poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw(partialTick)));
			poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch(partialTick)));
			poseStack.mulPose(Axis.ZP.rotationDegrees(state.roll(partialTick)));
			if (ring) {
				renderRing(poseStack, consumer, alpha, light);
			} else {
				renderThorns(poseStack, consumer, state, alpha, partialTick, light);
			}
			poseStack.popPose();
			rendered = true;
		}
		return rendered;
	}

	private static int applySoftEmissiveLight(int light) {
		int block = Math.round(Mth.lerp(EMISSIVE_STRENGTH, LightTexture.block(light), 15.0F));
		int sky = Math.round(Mth.lerp(EMISSIVE_STRENGTH, LightTexture.sky(light), 15.0F));
		return LightTexture.pack(block, sky);
	}

	private static void renderRing(PoseStack poseStack, VertexConsumer consumer, float alpha, int light) {
		PoseStack.Pose pose = poseStack.last();
		quad(
				consumer,
				pose,
				-RING_HALF_SIZE, 0.0F, -RING_HALF_SIZE,
				-RING_HALF_SIZE, 0.0F, RING_HALF_SIZE,
				RING_HALF_SIZE, 0.0F, RING_HALF_SIZE,
				RING_HALF_SIZE, 0.0F, -RING_HALF_SIZE,
				alpha,
				light,
				0.0F, 1.0F, 0.0F,
				0.0F, 0.0F, 1.0F, 1.0F
		);
	}

	private static void renderThorns(
			PoseStack poseStack,
			VertexConsumer consumer,
			HubrisClientState.NimbusState state,
			float nimbusAlpha,
			float partialTick,
			int light
	) {
		for (int index = 0; index < 4; index++) {
			float alpha = nimbusAlpha * state.thornAlpha(index, partialTick);
			if (alpha <= 0.002F)
				continue;
			float scale = state.thornScale(index, partialTick);
			poseStack.pushPose();
			poseStack.mulPose(Axis.YP.rotationDegrees(index * 90.0F));
			poseStack.translate(0.0F, 0.0F, THORN_RADIUS);
			poseStack.scale(scale, scale, scale);
			renderCrossedThorn(poseStack.last(), consumer, alpha, light);
			poseStack.popPose();
		}
	}

	private static void renderCrossedThorn(PoseStack.Pose pose, VertexConsumer consumer, float alpha, int light) {
		quad(consumer, pose, -THORN_HALF_WIDTH, -THORN_HALF_HEIGHT, 0.0F, -THORN_HALF_WIDTH, THORN_HALF_HEIGHT, 0.0F, THORN_HALF_WIDTH, THORN_HALF_HEIGHT, 0.0F, THORN_HALF_WIDTH, -THORN_HALF_HEIGHT, 0.0F, alpha, light, 0.0F, 0.0F, 1.0F, THORN_U0, THORN_V0, THORN_U1, THORN_V1);
		quad(consumer, pose, 0.0F, -THORN_HALF_HEIGHT, -THORN_HALF_WIDTH, 0.0F, THORN_HALF_HEIGHT, -THORN_HALF_WIDTH, 0.0F, THORN_HALF_HEIGHT, THORN_HALF_WIDTH, 0.0F, -THORN_HALF_HEIGHT, THORN_HALF_WIDTH, alpha, light, 1.0F, 0.0F, 0.0F, THORN_U0, THORN_V0, THORN_U1, THORN_V1);
	}

	private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float alpha, int light, float nx, float ny, float nz, float u0, float v0, float u1, float v1) {
		vertex(consumer, pose, x0, y0, z0, u0, v1, alpha, light, nx, ny, nz);
		vertex(consumer, pose, x1, y1, z1, u0, v0, alpha, light, nx, ny, nz);
		vertex(consumer, pose, x2, y2, z2, u1, v0, alpha, light, nx, ny, nz);
		vertex(consumer, pose, x3, y3, z3, u1, v1, alpha, light, nx, ny, nz);
	}

	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, float alpha, int light, float nx, float ny, float nz) {
		consumer.addVertex(pose.pose(), x, y, z)
				.setColor(255, 255, 255, Mth.clamp((int) (alpha * 255.0F), 0, 255))
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, nx, ny, nz);
	}
}
