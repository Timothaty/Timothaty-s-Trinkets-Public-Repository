package net.timothaty.timothatystrinkets.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public final class BeatificPalliumRenderTypes extends RenderType {
	private static final Map<ResourceLocation, RenderType> TRANSLUCENT_COLOR_ONLY = new ConcurrentHashMap<>();

	private BeatificPalliumRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
			boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
		super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
	}

	public static RenderType translucentColorOnly(ResourceLocation texture) {
		return TRANSLUCENT_COLOR_ONLY.computeIfAbsent(texture, BeatificPalliumRenderTypes::createTranslucentColorOnly);
	}

	private static RenderType createTranslucentColorOnly(ResourceLocation texture) {
		CompositeState state = CompositeState.builder()
				.setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
				.setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
				.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
				.setCullState(NO_CULL)
				.setLightmapState(LIGHTMAP)
				.setOverlayState(OVERLAY)
				.setDepthTestState(LEQUAL_DEPTH_TEST)
				.setWriteMaskState(COLOR_WRITE)
				.createCompositeState(true);

		return create(
				"timothatys_trinkets_beatific_pallium_" + texture.toDebugFileName(),
				DefaultVertexFormat.NEW_ENTITY,
				VertexFormat.Mode.QUADS,
				1536,
				true,
				true,
				state
		);
	}
}
