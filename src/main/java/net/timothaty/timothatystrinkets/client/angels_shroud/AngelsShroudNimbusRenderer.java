package net.timothaty.timothatystrinkets.client.angels_shroud;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.angels_shroud.AngelsShroudData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class AngelsShroudNimbusRenderer {
	private static final ResourceLocation NIMBUS_TEXTURE = ResourceLocation.fromNamespaceAndPath(
			TimothatysTrinketsMod.MODID,
			"textures/particle/angels_shroud_nimbus.png"
	);
	private static final ResourceLocation THORN_TEXTURE = ResourceLocation.fromNamespaceAndPath(
			TimothatysTrinketsMod.MODID,
			"textures/particle/angels_shroud_thorn.png"
	);
	private static final RenderType NIMBUS_RENDER_TYPE = RenderType.entityTranslucent(NIMBUS_TEXTURE);
	private static final RenderType THORN_RENDER_TYPE = RenderType.entityTranslucent(THORN_TEXTURE);
	private static final float RING_HALF_SIZE = 0.42F;
	private static final float THORN_RADIUS = 0.3F;
	private static final float THORN_HALF_WIDTH = 0.25F;
	private static final float THORN_HALF_HEIGHT = 0.25F;
	private static final float EMISSIVE_STRENGTH = 0.35F;

	private AngelsShroudNimbusRenderer() {
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null)
			return;

		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		if (!hasRenderablePlayer(level, partialTick))
			return;

		Vec3 camera = event.getCamera().getPosition();
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
		poseStack.pushPose();
		poseStack.translate(-camera.x, -camera.y, -camera.z);

		VertexConsumer ringConsumer = buffers.getBuffer(NIMBUS_RENDER_TYPE);
		if (renderPass(level, poseStack, ringConsumer, partialTick, true))
			buffers.endBatch(NIMBUS_RENDER_TYPE);

		VertexConsumer thornConsumer = buffers.getBuffer(THORN_RENDER_TYPE);
		if (renderPass(level, poseStack, thornConsumer, partialTick, false))
			buffers.endBatch(THORN_RENDER_TYPE);
		poseStack.popPose();
	}

	private static boolean hasRenderablePlayer(ClientLevel level, float partialTick) {
		for (Player player : level.players()) {
			MobEffectInstance effect = player.getEffect(TimothatysTrinketsModMobEffects.ANGELS_SHROUD);
			if (player.isAlive() && !player.isRemoved() && effectAlpha(effect, partialTick) > 0.002F)
				return true;
		}
		return false;
	}

	private static boolean renderPass(ClientLevel level, PoseStack poseStack, VertexConsumer consumer,
			float partialTick, boolean ring) {
		boolean rendered = false;
		for (Player player : level.players()) {
			if (!player.isAlive() || player.isRemoved())
				continue;
			MobEffectInstance effect = player.getEffect(TimothatysTrinketsModMobEffects.ANGELS_SHROUD);
			float alpha = effectAlpha(effect, partialTick);
			if (alpha <= 0.002F)
				continue;

			float time = player.tickCount + partialTick;
			double x = Mth.lerp(partialTick, player.xo, player.getX());
			double y = Mth.lerp(partialTick, player.yo, player.getY())
					+ player.getBbHeight() + 0.16D + Mth.sin(time * 0.12F) * 0.025D;
			double z = Mth.lerp(partialTick, player.zo, player.getZ());
			int light = applySoftEmissiveLight(
					LevelRenderer.getLightColor(level, player.blockPosition().above(2))
			);

			poseStack.pushPose();
			poseStack.translate(x, y, z);
			poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.1F));
			poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(time * 0.055F) * 1.4F));
			poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.cos(time * 0.047F) * 1.1F));
			if (ring)
				renderRing(poseStack.last(), consumer, alpha, light);
			else
				renderThorns(poseStack, consumer, alpha, light);
			poseStack.popPose();
			rendered = true;
		}
		return rendered;
	}

	private static float effectAlpha(MobEffectInstance effect, float partialTick) {
		if (effect == null)
			return 0.0F;
		float remaining = Math.max(0.0F, effect.getDuration() - partialTick);
		float elapsed = Math.max(0.0F, AngelsShroudData.DURATION_TICKS - remaining);
		float fadeIn = Mth.clamp(elapsed / AngelsShroudData.SCREEN_FADE_IN_TICKS, 0.0F, 1.0F);
		float fadeOut = Mth.clamp(remaining / AngelsShroudData.VISUAL_FADE_OUT_TICKS, 0.0F, 1.0F);
		return 0.94F * fadeIn * fadeOut;
	}

	private static int applySoftEmissiveLight(int light) {
		int block = Math.round(Mth.lerp(EMISSIVE_STRENGTH, LightTexture.block(light), 15.0F));
		int sky = Math.round(Mth.lerp(EMISSIVE_STRENGTH, LightTexture.sky(light), 15.0F));
		return LightTexture.pack(block, sky);
	}

	private static void renderRing(PoseStack.Pose pose, VertexConsumer consumer, float alpha, int light) {
		quad(
				consumer, pose,
				-RING_HALF_SIZE, 0.0F, -RING_HALF_SIZE,
				-RING_HALF_SIZE, 0.0F, RING_HALF_SIZE,
				RING_HALF_SIZE, 0.0F, RING_HALF_SIZE,
				RING_HALF_SIZE, 0.0F, -RING_HALF_SIZE,
				alpha, light,
				0.0F, 1.0F, 0.0F
		);
	}

	private static void renderThorns(PoseStack poseStack, VertexConsumer consumer, float alpha, int light) {
		for (int index = 0; index < 4; index++) {
			poseStack.pushPose();
			poseStack.mulPose(Axis.YP.rotationDegrees(index * 90.0F));
			poseStack.translate(0.0F, 0.0F, THORN_RADIUS);
			renderCrossedThorn(poseStack.last(), consumer, alpha, light);
			poseStack.popPose();
		}
	}

	private static void renderCrossedThorn(PoseStack.Pose pose, VertexConsumer consumer, float alpha, int light) {
		quad(consumer, pose,
				-THORN_HALF_WIDTH, -THORN_HALF_HEIGHT, 0.0F,
				-THORN_HALF_WIDTH, THORN_HALF_HEIGHT, 0.0F,
				THORN_HALF_WIDTH, THORN_HALF_HEIGHT, 0.0F,
				THORN_HALF_WIDTH, -THORN_HALF_HEIGHT, 0.0F,
				alpha, light, 0.0F, 0.0F, 1.0F);
		quad(consumer, pose,
				0.0F, -THORN_HALF_HEIGHT, -THORN_HALF_WIDTH,
				0.0F, THORN_HALF_HEIGHT, -THORN_HALF_WIDTH,
				0.0F, THORN_HALF_HEIGHT, THORN_HALF_WIDTH,
				0.0F, -THORN_HALF_HEIGHT, THORN_HALF_WIDTH,
				alpha, light, 1.0F, 0.0F, 0.0F);
	}

	private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
			float x0, float y0, float z0, float x1, float y1, float z1,
			float x2, float y2, float z2, float x3, float y3, float z3,
			float alpha, int light, float nx, float ny, float nz) {
		vertex(consumer, pose, x0, y0, z0, 0.0F, 1.0F, alpha, light, nx, ny, nz);
		vertex(consumer, pose, x1, y1, z1, 0.0F, 0.0F, alpha, light, nx, ny, nz);
		vertex(consumer, pose, x2, y2, z2, 1.0F, 0.0F, alpha, light, nx, ny, nz);
		vertex(consumer, pose, x3, y3, z3, 1.0F, 1.0F, alpha, light, nx, ny, nz);
	}

	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
			float x, float y, float z, float u, float v, float alpha, int light,
			float nx, float ny, float nz) {
		consumer.addVertex(pose.pose(), x, y, z)
				.setColor(255, 255, 255, Mth.clamp(Math.round(alpha * 255.0F), 0, 255))
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, nx, ny, nz);
	}
}
