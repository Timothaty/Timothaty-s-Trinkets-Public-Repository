package net.timothaty.timothatystrinkets.client.vfx.debtlord_altar;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.entity.DebtlordEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;

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
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class DebtlordAltarVfxRenderer {
	private static final int SEGMENTS = 24;
	private static final int TRAIL_COUNT = 3;
	private static final double RENDER_DISTANCE_SQR = 96.0D * 96.0D;
	private static final float CRIMSON_RED = 0.80F;
	private static final float CRIMSON_GREEN = 0.0F;
	private static final float CRIMSON_BLUE = 0.165F;
	private static final DustParticleOptions APPEARANCE_DUST = new DustParticleOptions(new Vector3f(0.025F, 0.006F, 0.035F), 0.92F);
	private static final RenderType ALTAR_VFX_RENDER_TYPE = RenderType.create(
		"timothatys_trinkets_debtlord_altar_vfx",
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

	private DebtlordAltarVfxRenderer() {
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null)
			return;

		ClientLevel level = minecraft.level;
		Vec3 playerPosition = minecraft.player.position();
		for (Entity entity : level.entitiesForRendering()) {
			if (!(entity instanceof DebtlordEntity debtlord))
				continue;

			if (debtlord.isAltarAppearanceVfxActive() && debtlord.position().distanceToSqr(playerPosition) <= RENDER_DISTANCE_SQR)
				spawnAppearanceDust(level, debtlord);

			BlockPos altarPos = debtlord.getBoundAltarPos();
			if (altarPos != null && debtlord.isAltarFightVfxActive() && Vec3.atCenterOf(altarPos).distanceToSqr(playerPosition) <= RENDER_DISTANCE_SQR)
				spawnAltarEnergyDots(level, altarPos);
		}
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return;

		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		Camera camera = event.getCamera();
		Vec3 cameraPosition = camera.getPosition();
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
		VertexConsumer consumer = bufferSource.getBuffer(ALTAR_VFX_RENDER_TYPE);
		boolean renderedAny = false;
		float time = minecraft.level.getGameTime() + partialTick;

		poseStack.pushPose();
		poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
		for (Entity entity : minecraft.level.entitiesForRendering()) {
			if (!(entity instanceof DebtlordEntity debtlord) || !debtlord.hasActiveAltarVfx())
				continue;

			BlockPos altarPos = debtlord.getBoundAltarPos();
			if (altarPos == null)
				continue;

			Vec3 center = new Vec3(altarPos.getX() + 0.5D, altarPos.getY() + 0.58D, altarPos.getZ() + 0.5D);
			if (center.distanceToSqr(cameraPosition) > RENDER_DISTANCE_SQR)
				continue;

			renderedAny = true;
			renderAltarTrails(poseStack, consumer, cameraPosition, center, time);
		}
		poseStack.popPose();

		if (renderedAny)
			bufferSource.endBatch(ALTAR_VFX_RENDER_TYPE);
	}

	private static void renderAltarTrails(PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPosition, Vec3 center, float time) {
		for (int trail = 0; trail < TRAIL_COUNT; trail++) {
			float phase = (float) (trail * Math.PI * 2.0D / TRAIL_COUNT);
			renderTrail(poseStack, consumer, cameraPosition, center, time, phase);
		}
	}

	private static void renderTrail(PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPosition, Vec3 center, float time, float phase) {
		Vec3[] points = new Vec3[SEGMENTS + 1];
		Vec3[] sides = new Vec3[SEGMENTS + 1];
		float[] widths = new float[SEGMENTS + 1];
		float[] alphas = new float[SEGMENTS + 1];
		double arc = Math.PI * 1.52D;
		double orbit = time * 0.09D + phase;

		for (int i = 0; i <= SEGMENTS; i++) {
			float t = i / (float) SEGMENTS;
			double angle = orbit + arc * t;
			double radius = 0.92D + Math.sin(t * Math.PI) * 0.10D;
			double y = center.y - 0.34D + 0.72D * t + Math.sin(time * 0.08D + phase + t * Math.PI * 2.0D) * 0.025D;
			points[i] = new Vec3(center.x + Math.cos(angle) * radius, y, center.z + Math.sin(angle) * radius);
			widths[i] = 0.095F * widthAlongTrail(t);
			alphas[i] = 0.76F * alphaAlongTrail(t);
		}

		for (int i = 0; i <= SEGMENTS; i++) {
			Vec3 previous = points[Math.max(0, i - 1)];
			Vec3 next = points[Math.min(SEGMENTS, i + 1)];
			Vec3 tangent = next.subtract(previous);
			if (tangent.lengthSqr() < 0.00001D) {
				sides[i] = new Vec3(1.0D, 0.0D, 0.0D);
				continue;
			}
			Vec3 side = tangent.cross(cameraPosition.subtract(points[i])).normalize();
			sides[i] = side.lengthSqr() < 0.00001D ? new Vec3(1.0D, 0.0D, 0.0D) : side;
		}

		for (int i = 0; i < SEGMENTS; i++)
			drawSegment(poseStack, consumer, points[i], points[i + 1], sides[i], sides[i + 1], widths[i], widths[i + 1], alphas[i], alphas[i + 1]);
	}

	private static float widthAlongTrail(float t) {
		float middle = (float) Math.sin(Math.PI * t);
		return (0.10F + 0.90F * middle) * Mth.clamp(t / 0.12F, 0.0F, 1.0F) * Mth.clamp((1.0F - t) / 0.18F, 0.0F, 1.0F);
	}

	private static float alphaAlongTrail(float t) {
		float middle = (float) Math.sin(Math.PI * t);
		return Mth.clamp(t / 0.12F, 0.0F, 1.0F) * Mth.clamp((1.0F - t) / 0.25F, 0.0F, 1.0F) * (0.26F + 0.74F * middle);
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
			.setColor(205, 0, 42, Mth.clamp(Math.round(alpha * 255.0F), 0, 255));
	}

	private static void spawnAppearanceDust(ClientLevel level, DebtlordEntity debtlord) {
		RandomSource random = level.random;
		boolean burst = debtlord.getAltarSummonVisualTicks() <= 7;
		int count = burst ? 18 : 7;
		double height = Math.max(1.0D, debtlord.getBbHeight());
		for (int i = 0; i < count; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0D;
			double radius = 0.42D + random.nextDouble() * 0.95D;
			double x = debtlord.getX() + Math.cos(angle) * radius;
			double y = debtlord.getY() + 0.08D + random.nextDouble() * height * 0.92D;
			double z = debtlord.getZ() + Math.sin(angle) * radius;
			double speed = burst ? 0.035D + random.nextDouble() * 0.070D : 0.010D + random.nextDouble() * 0.010D;
			double xSpeed = burst ? Math.cos(angle) * speed : -Math.sin(angle) * speed;
			double ySpeed = burst ? 0.020D + random.nextDouble() * 0.055D : 0.006D + random.nextDouble() * 0.012D;
			double zSpeed = burst ? Math.sin(angle) * speed : Math.cos(angle) * speed;
			level.addParticle(APPEARANCE_DUST, x, y, z, xSpeed, ySpeed, zSpeed);
		}
	}

	private static void spawnAltarEnergyDots(ClientLevel level, BlockPos altarPos) {
		RandomSource random = level.random;
		int count = 3 + random.nextInt(3);
		double centerX = altarPos.getX() + 0.5D;
		double centerZ = altarPos.getZ() + 0.5D;
		for (int i = 0; i < count; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0D;
			double radius = 0.20D + random.nextDouble() * 0.64D;
			double x = centerX + Math.cos(angle) * radius;
			double y = altarPos.getY() + 0.18D + random.nextDouble() * 0.48D;
			double z = centerZ + Math.sin(angle) * radius;
			level.addParticle(TimothatysTrinketsModParticleTypes.DOT.get(), x, y, z, CRIMSON_RED, CRIMSON_GREEN, CRIMSON_BLUE);
		}
	}
}
