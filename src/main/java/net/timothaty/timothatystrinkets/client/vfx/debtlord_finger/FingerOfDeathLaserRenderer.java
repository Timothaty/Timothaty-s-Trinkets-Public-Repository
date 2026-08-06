package net.timothaty.timothatystrinkets.client.vfx.debtlord_finger;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.vfx.debtlord_finger.FingerOfDeathLaserHandler.LaserVisual;

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

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class FingerOfDeathLaserRenderer {
	private static final int OUTER_SEGMENTS = 10;
	private static final int CORE_SEGMENTS = 8;
	private static final float OUTER_RADIUS = 0.16F;
	private static final float MID_RADIUS = 0.095F;
	private static final float CORE_RADIUS = 0.038F;
	private static final RenderType LASER_RENDER_TYPE = RenderType.create(
		"timothatys_trinkets_finger_of_death_laser",
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

	private FingerOfDeathLaserRenderer() {
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		FingerOfDeathLaserHandler.tick();
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || FingerOfDeathLaserHandler.activeLasers().isEmpty())
			return;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return;

		Camera camera = minecraft.gameRenderer.getMainCamera();
		Vec3 cameraPosition = camera.getPosition();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		float time = minecraft.level.getGameTime() + partialTick;
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
		VertexConsumer consumer = bufferSource.getBuffer(LASER_RENDER_TYPE);

		poseStack.pushPose();
		poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
		for (LaserVisual laser : FingerOfDeathLaserHandler.activeLasers()) {
			Vec3 start = laser.source();
			Vec3 end = laser.end();
			renderLaser(poseStack, consumer, start, end, time);
		}
		poseStack.popPose();
		bufferSource.endBatch(LASER_RENDER_TYPE);
	}

	private static void renderLaser(PoseStack poseStack, VertexConsumer consumer, Vec3 start, Vec3 end, float time) {
		Vec3 direction = end.subtract(start);
		if (direction.lengthSqr() < 0.0025D)
			return;

		float pulse = 0.88F + 0.12F * Mth.sin(time * 0.75F);
		drawBeamShell(poseStack, consumer, start, end, OUTER_RADIUS * pulse, OUTER_SEGMENTS, time * 0.08F, 102, 1, 15, 0.34F);
		drawBeamShell(poseStack, consumer, start, end, MID_RADIUS * pulse, OUTER_SEGMENTS, time * -0.12F, 178, 5, 24, 0.52F);
		drawBeamShell(poseStack, consumer, start, end, CORE_RADIUS * pulse, CORE_SEGMENTS, time * 0.16F, 255, 246, 238, 0.93F);
	}

	private static void drawBeamShell(PoseStack poseStack, VertexConsumer consumer, Vec3 start, Vec3 end,
			float radius, int segments, float roll, int red, int green, int blue, float alpha) {
		Vec3 direction = end.subtract(start).normalize();
		Vec3 reference = Math.abs(direction.y) > 0.96D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
		Vec3 side = direction.cross(reference).normalize();
		Vec3 up = side.cross(direction).normalize();

		for (int segment = 0; segment < segments; segment++) {
			double angleA = roll + (Math.PI * 2.0D * segment) / segments;
			double angleB = roll + (Math.PI * 2.0D * (segment + 1)) / segments;
			Vec3 offsetA = radialOffset(side, up, angleA, radius);
			Vec3 offsetB = radialOffset(side, up, angleB, radius);
			float faceAlpha = alpha * (0.82F + 0.18F * Mth.sin((float) angleA * 2.0F));

			vertex(poseStack, consumer, start.add(offsetA), red, green, blue, faceAlpha);
			vertex(poseStack, consumer, end.add(offsetA), red, green, blue, faceAlpha);
			vertex(poseStack, consumer, end.add(offsetB), red, green, blue, faceAlpha);
			vertex(poseStack, consumer, start.add(offsetB), red, green, blue, faceAlpha);
		}
	}

	private static Vec3 radialOffset(Vec3 side, Vec3 up, double angle, float radius) {
		return side.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
	}

	private static void vertex(PoseStack poseStack, VertexConsumer consumer, Vec3 position, int red, int green, int blue, float alpha) {
		consumer.addVertex(poseStack.last().pose(), (float) position.x, (float) position.y, (float) position.z)
			.setColor(red, green, blue, Mth.clamp(Math.round(alpha * 255.0F), 0, 255));
	}
}
