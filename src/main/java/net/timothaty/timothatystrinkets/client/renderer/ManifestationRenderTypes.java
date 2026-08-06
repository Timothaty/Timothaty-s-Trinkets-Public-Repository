package net.timothaty.timothatystrinkets.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

@OnlyIn(Dist.CLIENT)
public final class ManifestationRenderTypes {
	private static final RenderType FLAT_WEDGE = RenderType.create(
			"timothatys_trinkets_manifestation_flat_wedge",
			DefaultVertexFormat.POSITION_COLOR,
			VertexFormat.Mode.QUADS,
			1536,
			false,
			false,
			RenderType.CompositeState.builder()
					.setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
					.setTextureState(RenderStateShard.NO_TEXTURE)
					// LIGHTNING_TRANSPARENCY is SRC_ALPHA + ONE in the 1.21.1 mappings.
					.setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
					.setCullState(RenderStateShard.NO_CULL)
					.setLightmapState(RenderStateShard.NO_LIGHTMAP)
					.setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
					.setWriteMaskState(RenderStateShard.COLOR_WRITE)
					.setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
					.createCompositeState(false));

	private ManifestationRenderTypes() {
	}

	public static RenderType flatWedge() {
		return FLAT_WEDGE;
	}
}
