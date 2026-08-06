package net.timothaty.timothatystrinkets.client.gorge;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.network.GorgeConsumptionVisualMessage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

import java.util.List;

public final class GorgeConsumptionClient {
	private static final double DOT_RED = 75.0D / 255.0D;
	private static final double DOT_GREEN = 3.0D / 255.0D;
	private static final double DOT_BLUE = 13.0D / 255.0D;

	private GorgeConsumptionClient() {
	}

	public static void handle(GorgeConsumptionVisualMessage message) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null)
			return;

		GorgeAnimationState.start(
				message.consumerEntityId(),
				message.centerX(),
				message.centerZ()
		);
		GorgeFirstPersonAnimation.start(message.consumerEntityId());
		spawnDots(level, message);
		Entity target = level.getEntity(message.targetEntityId());
		Entity consumer = level.getEntity(message.consumerEntityId());
		if (target == null || consumer == null)
			return;

		ResourceLocation texture = textureSnapshot(
				minecraft.getEntityRenderDispatcher(),
				target
		);
		if (texture == null)
			return;

		List<GorgeTextureRegionCache.TextureRegion> regions =
				GorgeTextureRegionCache.regions(
						minecraft.getResourceManager(),
						texture
				);
		if (regions.isEmpty())
			return;

		GorgeConsumptionRenderer.start(message, texture, regions);
	}

	private static void spawnDots(
			ClientLevel level,
			GorgeConsumptionVisualMessage message
	) {
		RandomSource random = RandomSource.create(
				message.seed() ^ 0xBB67AE8584CAA73BL
		);
		int count = 5 + random.nextInt(4);
		for (int index = 0; index < count; index++) {
			double x = randomBetween(random, message.minX(), message.maxX());
			double y = randomBetween(random, message.minY(), message.maxY());
			double z = randomBetween(random, message.minZ(), message.maxZ());
			level.addParticle(
					TimothatysTrinketsModParticleTypes.DOT.get(),
					x,
					y,
					z,
					DOT_RED,
					DOT_GREEN,
					DOT_BLUE
			);
		}
	}

	private static double randomBetween(
			RandomSource random,
			double minimum,
			double maximum
	) {
		return minimum + random.nextDouble() * Math.max(0.0D, maximum - minimum);
	}

	private static <T extends Entity> ResourceLocation textureSnapshot(
			EntityRenderDispatcher dispatcher,
			T entity
	) {
		try {
			EntityRenderer<? super T> renderer = dispatcher.getRenderer(entity);
			return renderer.getTextureLocation(entity);
		} catch (RuntimeException exception) {
			TimothatysTrinketsMod.LOGGER.debug(
					"Could not snapshot Gorge target texture for {}",
					entity.getType(),
					exception
			);
			return null;
		}
	}
}
