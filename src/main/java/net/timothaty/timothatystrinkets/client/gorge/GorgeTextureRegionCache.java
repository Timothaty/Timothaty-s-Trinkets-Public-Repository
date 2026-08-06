package net.timothaty.timothatystrinkets.client.gorge;

import com.mojang.blaze3d.platform.NativeImage;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.FastColor;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(
		modid = TimothatysTrinketsMod.MODID,
		value = Dist.CLIENT
)
public final class GorgeTextureRegionCache {
	private static final int[] REGION_SIZES = {4, 6, 8};
	private static final int ALPHA_THRESHOLD = 8;
	private static final float REQUIRED_OPAQUE_FRACTION = 0.50F;
	private static final Map<ResourceLocation, List<TextureRegion>> CACHE =
			new HashMap<>();

	private GorgeTextureRegionCache() {
	}

	public static List<TextureRegion> regions(
			ResourceManager resourceManager,
			ResourceLocation texture
	) {
		if (resourceManager == null || texture == null)
			return List.of();
		return CACHE.computeIfAbsent(
				texture,
				key -> scanTexture(resourceManager, key)
		);
	}

	public static void clear() {
		CACHE.clear();
	}

	@SubscribeEvent
	public static void registerReloadListener(
			RegisterClientReloadListenersEvent event
	) {
		event.registerReloadListener(
				(ResourceManagerReloadListener) resourceManager -> clear()
		);
	}

	private static List<TextureRegion> scanTexture(
			ResourceManager resourceManager,
			ResourceLocation texture
	) {
		Optional<Resource> resource = resourceManager.getResource(texture);
		if (resource.isEmpty())
			return List.of();

		try (InputStream stream = resource.get().open();
				NativeImage image = NativeImage.read(stream)) {
			List<TextureRegion> regions = new ArrayList<>();
			for (int regionSize : REGION_SIZES) {
				collectRegions(image, regionSize, regions);
			}
			return List.copyOf(regions);
		} catch (IOException | RuntimeException exception) {
			TimothatysTrinketsMod.LOGGER.debug(
					"Could not alpha-scan Gorge texture {}",
					texture,
					exception
			);
			return fallbackRegions();
		}
	}

	private static void collectRegions(
			NativeImage image,
			int regionSize,
			List<TextureRegion> regions
	) {
		int width = image.getWidth();
		int height = image.getHeight();
		if (regionSize > width || regionSize > height)
			return;

		int requiredOpaque = (int) Math.ceil(
				regionSize * regionSize * REQUIRED_OPAQUE_FRACTION
		);
		for (int y = 0; y + regionSize <= height; y += regionSize) {
			for (int x = 0; x + regionSize <= width; x += regionSize) {
				if (opaquePixelCount(image, x, y, regionSize) < requiredOpaque)
					continue;
				regions.add(new TextureRegion(
						x / (float) width,
						y / (float) height,
						(x + regionSize) / (float) width,
						(y + regionSize) / (float) height
				));
			}
		}
	}

	private static int opaquePixelCount(
			NativeImage image,
			int startX,
			int startY,
			int regionSize
	) {
		int opaque = 0;
		for (int y = startY; y < startY + regionSize; y++) {
			for (int x = startX; x < startX + regionSize; x++) {
				if (FastColor.ABGR32.alpha(image.getPixelRGBA(x, y))
						> ALPHA_THRESHOLD) {
					opaque++;
				}
			}
		}
		return opaque;
	}

	private static List<TextureRegion> fallbackRegions() {
		List<TextureRegion> regions = new ArrayList<>(16);
		for (int y = 0; y < 4; y++) {
			for (int x = 0; x < 4; x++) {
				regions.add(new TextureRegion(
						x / 4.0F,
						y / 4.0F,
						(x + 1) / 4.0F,
						(y + 1) / 4.0F
				));
			}
		}
		return List.copyOf(regions);
	}

	public record TextureRegion(float u0, float v0, float u1, float v1) {
	}
}
