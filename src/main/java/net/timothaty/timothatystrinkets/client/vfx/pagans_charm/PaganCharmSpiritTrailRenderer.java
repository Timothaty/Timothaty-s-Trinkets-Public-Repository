package net.timothaty.timothatystrinkets.client.vfx.pagans_charm;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.pagans_charm.PaganCharmMeditationPlayerState;
import net.timothaty.timothatystrinkets.mechanics.pagans_charm.PaganCharmMeditationRules;
import net.timothaty.timothatystrinkets.mechanics.pagans_charm.PaganCharmTuning;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class PaganCharmSpiritTrailRenderer {
	private static final int TRAIL_COUNT = PaganCharmTuning.SPIRIT_TRAIL_COUNT;
	private static final int SEGMENTS = PaganCharmTuning.SPIRIT_TRAIL_SEGMENTS;
	private static final double RENDER_DISTANCE_SQR = PaganCharmTuning.SPIRIT_TRAIL_RENDER_DISTANCE_SQR;
	private static final float BASE_WIDTH = PaganCharmTuning.SPIRIT_TRAIL_BASE_WIDTH;
	private static final float BASE_ALPHA = PaganCharmTuning.SPIRIT_TRAIL_BASE_ALPHA;
	private static final RenderType SPIRIT_TRAIL_RENDER_TYPE = RenderType.create(
			"timothatys_trinkets_pagan_charm_spirit_trail",
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
					.createCompositeState(false)
	);

	private PaganCharmSpiritTrailRenderer() {
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
		Camera camera = event.getCamera();
		Vec3 cameraPosition = camera.getPosition();
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
		VertexConsumer consumer = bufferSource.getBuffer(SPIRIT_TRAIL_RENDER_TYPE);
		boolean renderedAny = false;

		poseStack.pushPose();
		poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

		for (Player player : level.players()) {
			if (distanceToCameraSqr(player, cameraPosition) > RENDER_DISTANCE_SQR)
				continue;
			if (!(player instanceof PaganCharmMeditationPlayerState state))
				continue;

			int phase = state.timothatys_trinkets$getPaganCharmMeditationPhase(player.tickCount + partialTick);
			if (phase == PaganCharmMeditationPlayerState.PHASE_NONE)
				continue;

			float activeTicks = state.timothatys_trinkets$getPaganCharmMeditationActiveTicks(player.tickCount + partialTick);
			float fade = smoothstep(0.0F, PaganCharmMeditationPlayerState.MEDITATE_TICKS, activeTicks);
			if (fade <= 0.01F)
				continue;

			renderedAny = true;
			boolean netherBiome = PaganCharmMeditationRules.isInNetherBiome(player);
			int red = netherBiome ? PaganCharmTuning.SPIRIT_TRAIL_NETHER_RED : PaganCharmTuning.SPIRIT_TRAIL_DEFAULT_RED;
			int green = netherBiome ? PaganCharmTuning.SPIRIT_TRAIL_NETHER_GREEN : PaganCharmTuning.SPIRIT_TRAIL_DEFAULT_GREEN;
			int blue = netherBiome ? PaganCharmTuning.SPIRIT_TRAIL_NETHER_BLUE : PaganCharmTuning.SPIRIT_TRAIL_DEFAULT_BLUE;
			renderPlayerTrails(poseStack, consumer, cameraPosition, player, partialTick, fade, red, green, blue);
		}

		poseStack.popPose();
		if (renderedAny) {
			bufferSource.endBatch(SPIRIT_TRAIL_RENDER_TYPE);
		}
	}

	private static void renderPlayerTrails(PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPosition, Player player, float partialTick, float fade, int red, int green, int blue) {
		Vec3 center = new Vec3(
				Mth.lerp(partialTick, player.xo, player.getX()),
				Mth.lerp(partialTick, player.yo, player.getY()),
				Mth.lerp(partialTick, player.zo, player.getZ())
		);

		float time = player.tickCount + partialTick;
		for (int trail = 0; trail < TRAIL_COUNT; trail++) {
			float trailPhase = (float) (trail * Math.PI * 2.0D / TRAIL_COUNT);
			renderTrail(poseStack, consumer, cameraPosition, center, player.getBbWidth(), time, trailPhase, fade, red, green, blue);
		}
	}

	private static double distanceToCameraSqr(Player player, Vec3 cameraPosition) {
		double dx = player.getX() - cameraPosition.x;
		double dy = player.getY() - cameraPosition.y;
		double dz = player.getZ() - cameraPosition.z;
		return dx * dx + dy * dy + dz * dz;
	}

	private static void renderTrail(PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPosition, Vec3 center, float playerWidth, float time, float trailPhase, float fade, int red, int green, int blue) {
		Vec3[] points = new Vec3[SEGMENTS + 1];
		Vec3[] sides = new Vec3[SEGMENTS + 1];
		float[] widths = new float[SEGMENTS + 1];
		float[] alphas = new float[SEGMENTS + 1];

		double radiusBase = Math.max(0.42D, playerWidth * 0.95D);
		double arc = Math.PI * 1.55D;
		double orbit = time * 0.055D + trailPhase;

		for (int i = 0; i <= SEGMENTS; i++) {
			float t = i / (float) SEGMENTS;
			double angle = orbit + arc * t;
			double radius = radiusBase + Math.sin(t * Math.PI) * 0.08D;
			double x = center.x + Math.cos(angle) * radius;
			double z = center.z + Math.sin(angle) * radius;
			double y = center.y + 0.30D + 0.62D * t + Math.sin(time * 0.07D + trailPhase + t * Math.PI * 2.0D) * 0.035D;

			points[i] = new Vec3(x, y, z);
			widths[i] = BASE_WIDTH * widthAlongTrail(t) * fade;
			alphas[i] = BASE_ALPHA * alphaAlongTrail(t) * fade;
		}

		for (int i = 0; i <= SEGMENTS; i++) {
			Vec3 previous = points[Math.max(0, i - 1)];
			Vec3 next = points[Math.min(SEGMENTS, i + 1)];
			Vec3 tangent = next.subtract(previous);
			if (tangent.lengthSqr() < 0.00001D) {
				sides[i] = new Vec3(1.0D, 0.0D, 0.0D);
				continue;
			}

			Vec3 toCamera = cameraPosition.subtract(points[i]);
			Vec3 side = tangent.cross(toCamera).normalize();
			sides[i] = side.lengthSqr() < 0.00001D ? new Vec3(1.0D, 0.0D, 0.0D) : side;
		}

		for (int i = 0; i < SEGMENTS; i++) {
			Vec3 start = points[i];
			Vec3 end = points[i + 1];
			if (end.subtract(start).lengthSqr() < 0.00001D)
				continue;

			Vec3 s0 = start.add(sides[i].scale(widths[i]));
			Vec3 s1 = start.subtract(sides[i].scale(widths[i]));
			Vec3 e0 = end.add(sides[i + 1].scale(widths[i + 1]));
			Vec3 e1 = end.subtract(sides[i + 1].scale(widths[i + 1]));

			vertex(poseStack, consumer, s0, alphas[i], red, green, blue);
			vertex(poseStack, consumer, e0, alphas[i + 1], red, green, blue);
			vertex(poseStack, consumer, e1, alphas[i + 1], red, green, blue);
			vertex(poseStack, consumer, s1, alphas[i], red, green, blue);
		}
	}

	private static float widthAlongTrail(float t) {
		return 0.10F + 0.90F * (float) Math.sin(Math.PI * t);
	}

	private static float alphaAlongTrail(float t) {
		float middle = (float) Math.sin(Math.PI * t);
		float tipFade = Mth.clamp((1.0F - t) / 0.22F, 0.0F, 1.0F);
		float rootFade = Mth.clamp(t / 0.14F, 0.0F, 1.0F);
		return rootFade * tipFade * (0.18F + 0.82F * middle);
	}

	private static float smoothstep(float edge0, float edge1, float x) {
		if (edge0 == edge1)
			return x < edge0 ? 0.0F : 1.0F;

		x = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
		return x * x * (3.0F - 2.0F * x);
	}

	private static void vertex(PoseStack poseStack, VertexConsumer consumer, Vec3 position, float alpha, int red, int green, int blue) {
		consumer.addVertex(poseStack.last().pose(), (float) position.x, (float) position.y, (float) position.z)
				.setColor(red, green, blue, Mth.clamp((int) (alpha * 255.0F), 0, 255));
	}
}
