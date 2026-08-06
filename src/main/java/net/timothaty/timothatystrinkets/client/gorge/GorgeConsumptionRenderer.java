package net.timothaty.timothatystrinkets.client.gorge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.network.GorgeConsumptionVisualMessage;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(
		modid = TimothatysTrinketsMod.MODID,
		value = Dist.CLIENT
)
public final class GorgeConsumptionRenderer {
	private static final int MIN_FRAGMENT_COUNT = 10;
	private static final int MEDIUM_FRAGMENT_COUNT = 12;
	private static final int MAX_FRAGMENT_COUNT = 14;
	private static final float MIN_FRAGMENT_SIZE = 0.12F;
	private static final float MAX_FRAGMENT_SIZE = 0.25F;
	private static final int MIN_LIFETIME_TICKS = 14;
	private static final int MAX_LIFETIME_TICKS = 18;
	private static final double MIN_BURST_DISTANCE = 0.20D;
	private static final double MAX_BURST_DISTANCE = 0.60D;

	private static final Map<ResourceLocation, List<Effect>> EFFECTS_BY_TEXTURE =
			new LinkedHashMap<>();
	private static ClientLevel trackedLevel;

	private GorgeConsumptionRenderer() {
	}

	public static void start(
			GorgeConsumptionVisualMessage message,
			ResourceLocation texture,
			List<GorgeTextureRegionCache.TextureRegion> regions
	) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null || texture == null || regions.isEmpty())
			return;

		Entity consumer = level.getEntity(message.consumerEntityId());
		if (consumer == null)
			return;
		if (trackedLevel != level) {
			clear();
			trackedLevel = level;
		}

		RandomSource random = RandomSource.create(message.seed());
		Vec3 targetCenter = new Vec3(
				message.centerX(),
				message.centerY(),
				message.centerZ()
		);
		List<Fragment> fragments = createFragments(
				message,
				targetCenter,
				regions,
				random
		);
		if (fragments.isEmpty())
			return;

		Effect effect = new Effect(
				message.consumerEntityId(),
				fragments,
				chestPosition(consumer, 1.0F)
		);
		EFFECTS_BY_TEXTURE.computeIfAbsent(
				texture,
				unused -> new ArrayList<>()
		).add(effect);
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			clear();
			return;
		}
		if (trackedLevel != level) {
			clear();
			trackedLevel = level;
			return;
		}

		Iterator<Map.Entry<ResourceLocation, List<Effect>>> textureIterator =
				EFFECTS_BY_TEXTURE.entrySet().iterator();
		while (textureIterator.hasNext()) {
			List<Effect> effects = textureIterator.next().getValue();
			effects.removeIf(Effect::tickAndIsFinished);
			if (effects.isEmpty())
				textureIterator.remove();
		}
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
				|| EFFECTS_BY_TEXTURE.isEmpty()) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null || trackedLevel != level) {
			clear();
			return;
		}

		float partialTick = event.getPartialTick()
				.getGameTimeDeltaPartialTick(false);
		Vec3 cameraPosition = event.getCamera().getPosition();
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource =
				minecraft.renderBuffers().bufferSource();

		poseStack.pushPose();
		poseStack.translate(
				-cameraPosition.x,
				-cameraPosition.y,
				-cameraPosition.z
		);
		for (Map.Entry<ResourceLocation, List<Effect>> entry
				: EFFECTS_BY_TEXTURE.entrySet()) {
			RenderType renderType = RenderType.entityTranslucent(entry.getKey());
			VertexConsumer consumer = bufferSource.getBuffer(renderType);
			for (Effect effect : entry.getValue()) {
				renderEffect(
						minecraft,
						level,
						poseStack,
						consumer,
						effect,
						partialTick
				);
			}
			bufferSource.endBatch(renderType);
		}
		poseStack.popPose();
	}

	private static List<Fragment> createFragments(
			GorgeConsumptionVisualMessage message,
			Vec3 center,
			List<GorgeTextureRegionCache.TextureRegion> regions,
			RandomSource random
	) {
		double width = Math.max(0.0D, message.maxX() - message.minX());
		double height = Math.max(0.0D, message.maxY() - message.minY());
		double depth = Math.max(0.0D, message.maxZ() - message.minZ());
		double longestSide = Math.max(width, Math.max(height, depth));
		int fragmentCount = longestSide < 0.9D
				? MIN_FRAGMENT_COUNT
				: longestSide < 1.5D
						? MEDIUM_FRAGMENT_COUNT
						: MAX_FRAGMENT_COUNT;

		List<Fragment> fragments = new ArrayList<>(fragmentCount);
		for (int index = 0; index < fragmentCount; index++) {
			Vec3 start = new Vec3(
					randomBetween(random, message.minX(), message.maxX()),
					randomBetween(random, message.minY(), message.maxY()),
					randomBetween(random, message.minZ(), message.maxZ())
			);
			Vec3 outward = start.subtract(center);
			outward = new Vec3(
					outward.x,
					outward.y * 0.28D + randomBetween(random, -0.04D, 0.12D),
					outward.z
			);
			if (outward.lengthSqr() < 1.0E-5D) {
				double angle = random.nextDouble() * Math.PI * 2.0D;
				outward = new Vec3(
						Math.cos(angle),
						randomBetween(random, 0.05D, 0.25D),
						Math.sin(angle)
				);
			}
			outward = outward.normalize();

			double burstDistance = randomBetween(
					random,
					MIN_BURST_DISTANCE,
					MAX_BURST_DISTANCE
			);
			Vec3 burstEnd = start.add(outward.scale(burstDistance))
					.add(0.0D, randomBetween(random, 0.10D, 0.28D), 0.0D);
			int lifetime = MIN_LIFETIME_TICKS
					+ random.nextInt(MAX_LIFETIME_TICKS - MIN_LIFETIME_TICKS + 1);
			float size = randomBetween(
					random,
					MIN_FRAGMENT_SIZE,
					MAX_FRAGMENT_SIZE
			);
			float fullTurn = (float) (Math.PI * 2.0D);
			GorgeTextureRegionCache.TextureRegion region =
					regions.get(random.nextInt(regions.size()));
			fragments.add(new Fragment(
					start,
					burstEnd,
					randomBetween(random, 0.08D, 0.24D),
					randomBetween(random, 0.15F, 0.25F),
					size,
					lifetime,
					random.nextFloat() * fullTurn,
					random.nextFloat() * fullTurn,
					random.nextFloat() * fullTurn,
					randomAngularVelocity(random),
					randomAngularVelocity(random),
					randomAngularVelocity(random),
					region
			));
		}
		return fragments;
	}

	private static void renderEffect(
			Minecraft minecraft,
			ClientLevel level,
			PoseStack poseStack,
			VertexConsumer vertexConsumer,
			Effect effect,
			float partialTick
	) {
		Vec3 destination = effect.destination(level, partialTick);
		boolean localFirstPerson = minecraft.player != null
				&& minecraft.player.getId() == effect.consumerEntityId
				&& minecraft.options.getCameraType() == CameraType.FIRST_PERSON;
		float age = effect.ageTicks + partialTick;
		for (Fragment fragment : effect.fragments) {
			float progress = Mth.clamp(
					age / fragment.lifetimeTicks,
					0.0F,
					1.0F
			);
			if (progress >= 1.0F)
				continue;

			float alpha = smoothstep(0.0F, 0.10F, progress)
					* (1.0F - smoothstep(0.70F, 1.0F, progress));
			float sizeScale = 1.0F
					- smoothstep(0.60F, 1.0F, progress);
			if (localFirstPerson) {
				float safetyFade = 1.0F
						- smoothstep(0.68F, 0.94F, progress);
				alpha *= safetyFade;
				sizeScale *= safetyFade;
			}
			if (alpha <= 0.002F || sizeScale <= 0.002F)
				continue;

			Vec3 position = fragmentPosition(fragment, destination, progress);
			int packedLight = LevelRenderer.getLightColor(
					level,
					BlockPos.containing(position)
			);
			float endTint = smoothstep(0.70F, 1.0F, progress);
			int red = Mth.clamp(
					Math.round(255.0F * (1.0F - 0.18F * endTint)),
					0,
					255
			);
			int green = Mth.clamp(
					Math.round(255.0F * (1.0F - 0.55F * endTint)),
					0,
					255
			);
			int blue = Mth.clamp(
					Math.round(255.0F * (1.0F - 0.52F * endTint)),
					0,
					255
			);
			renderFragment(
					poseStack,
					vertexConsumer,
					fragment,
					position,
					age,
					fragment.size * sizeScale,
					red,
					green,
					blue,
					Mth.clamp(Math.round(alpha * 255.0F), 0, 255),
					packedLight
			);
		}
	}

	private static void renderFragment(
			PoseStack poseStack,
			VertexConsumer consumer,
			Fragment fragment,
			Vec3 position,
			float age,
			float size,
			int red,
			int green,
			int blue,
			int alpha,
			int packedLight
	) {
		poseStack.pushPose();
		poseStack.translate(position.x, position.y, position.z);
		poseStack.mulPose(
				Axis.YP.rotation(fragment.initialYaw + fragment.yawVelocity * age)
		);
		poseStack.mulPose(
				Axis.XP.rotation(
						fragment.initialPitch + fragment.pitchVelocity * age
				)
		);
		poseStack.mulPose(
				Axis.ZP.rotation(
						fragment.initialRoll + fragment.rollVelocity * age
				)
		);

		float halfSize = size * 0.5F;
		GorgeTextureRegionCache.TextureRegion uv = fragment.region;
		PoseStack.Pose pose = poseStack.last();
		vertex(
				consumer,
				pose,
				-halfSize,
				-halfSize,
				uv.u0(),
				uv.v1(),
				red,
				green,
				blue,
				alpha,
				packedLight
		);
		vertex(
				consumer,
				pose,
				halfSize,
				-halfSize,
				uv.u1(),
				uv.v1(),
				red,
				green,
				blue,
				alpha,
				packedLight
		);
		vertex(
				consumer,
				pose,
				halfSize,
				halfSize,
				uv.u1(),
				uv.v0(),
				red,
				green,
				blue,
				alpha,
				packedLight
		);
		vertex(
				consumer,
				pose,
				-halfSize,
				halfSize,
				uv.u0(),
				uv.v0(),
				red,
				green,
				blue,
				alpha,
				packedLight
		);
		poseStack.popPose();
	}

	private static void vertex(
			VertexConsumer consumer,
			PoseStack.Pose pose,
			float x,
			float y,
			float u,
			float v,
			int red,
			int green,
			int blue,
			int alpha,
			int packedLight
	) {
		consumer.addVertex(pose, x, y, 0.0F)
				.setColor(red, green, blue, alpha)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(packedLight)
				.setNormal(pose, 0.0F, 0.0F, 1.0F);
	}

	private static Vec3 fragmentPosition(
			Fragment fragment,
			Vec3 destination,
			float progress
	) {
		if (progress <= fragment.burstFraction) {
			float burstProgress = smoothstep(
					0.0F,
					fragment.burstFraction,
					progress
			);
			return fragment.start.lerp(fragment.burstEnd, burstProgress);
		}

		float pullProgress = smoothstep(
				fragment.burstFraction,
				1.0F,
				progress
		);
		Vec3 control = fragment.burstEnd
				.add(destination.subtract(fragment.burstEnd).scale(0.34D))
				.add(0.0D, fragment.curveLift, 0.0D);
		return quadraticBezier(
				fragment.burstEnd,
				control,
				destination,
				pullProgress
		);
	}

	private static Vec3 quadraticBezier(
			Vec3 start,
			Vec3 control,
			Vec3 end,
			float progress
	) {
		double inverse = 1.0D - progress;
		return start.scale(inverse * inverse)
				.add(control.scale(2.0D * inverse * progress))
				.add(end.scale(progress * progress));
	}

	private static Vec3 chestPosition(Entity entity, float partialTick) {
		return new Vec3(
				Mth.lerp(partialTick, entity.xo, entity.getX()),
				Mth.lerp(partialTick, entity.yo, entity.getY())
						+ entity.getBbHeight() * 0.62D,
				Mth.lerp(partialTick, entity.zo, entity.getZ())
		);
	}

	private static float randomAngularVelocity(RandomSource random) {
		float magnitude = randomBetween(random, 0.07F, 0.18F);
		return random.nextBoolean() ? magnitude : -magnitude;
	}

	private static float randomBetween(
			RandomSource random,
			float minimum,
			float maximum
	) {
		return minimum + random.nextFloat() * (maximum - minimum);
	}

	private static double randomBetween(
			RandomSource random,
			double minimum,
			double maximum
	) {
		return minimum + random.nextDouble() * Math.max(0.0D, maximum - minimum);
	}

	private static float smoothstep(float edge0, float edge1, float value) {
		if (edge0 >= edge1)
			return value < edge0 ? 0.0F : 1.0F;
		float clamped = Mth.clamp(
				(value - edge0) / (edge1 - edge0),
				0.0F,
				1.0F
		);
		return clamped * clamped * (3.0F - 2.0F * clamped);
	}

	private static void clear() {
		EFFECTS_BY_TEXTURE.clear();
		trackedLevel = null;
	}

	private static final class Effect {
		private final int consumerEntityId;
		private final List<Fragment> fragments;
		private final int maximumLifetime;
		private Vec3 lastDestination;
		private int ageTicks;

		private Effect(
				int consumerEntityId,
				List<Fragment> fragments,
				Vec3 initialDestination
		) {
			this.consumerEntityId = consumerEntityId;
			this.fragments = fragments;
			this.maximumLifetime = fragments.stream()
					.mapToInt(fragment -> fragment.lifetimeTicks)
					.max()
					.orElse(1);
			this.lastDestination = initialDestination;
		}

		private boolean tickAndIsFinished() {
			ageTicks++;
			return ageTicks >= maximumLifetime;
		}

		private Vec3 destination(ClientLevel level, float partialTick) {
			Entity consumer = level.getEntity(consumerEntityId);
			if (consumer != null)
				lastDestination = chestPosition(consumer, partialTick);
			return lastDestination;
		}
	}

	private record Fragment(
			Vec3 start,
			Vec3 burstEnd,
			double curveLift,
			float burstFraction,
			float size,
			int lifetimeTicks,
			float initialYaw,
			float initialPitch,
			float initialRoll,
			float yawVelocity,
			float pitchVelocity,
			float rollVelocity,
			GorgeTextureRegionCache.TextureRegion region
	) {
	}
}
