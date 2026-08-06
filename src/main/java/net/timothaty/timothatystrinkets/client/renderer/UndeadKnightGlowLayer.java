package net.timothaty.timothatystrinkets.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.entity.UndeadKnightEntity;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class UndeadKnightGlowLayer extends RenderLayer<UndeadKnightEntity, UndeadKnightRenderer.UndeadKnightArmedModel> {
	private static final ResourceLocation GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/entities/undead_knight_glow.png");
	private static final int GLOW_COLOR = 0x80FFFFFF;

	public UndeadKnightGlowLayer(RenderLayerParent<UndeadKnightEntity, UndeadKnightRenderer.UndeadKnightArmedModel> parent) {
		super(parent);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, UndeadKnightEntity entity,
			float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
		if (entity.isInvisible())
			return;

		VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.eyes(GLOW_TEXTURE));
		this.getParentModel().renderToBuffer(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, GLOW_COLOR);
	}
}
