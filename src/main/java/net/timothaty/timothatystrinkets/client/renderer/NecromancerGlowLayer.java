package net.timothaty.timothatystrinkets.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.model.ModelNecromancerModel;
import net.timothaty.timothatystrinkets.entity.NecromancerEntity;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class NecromancerGlowLayer extends RenderLayer<NecromancerEntity, ModelNecromancerModel<NecromancerEntity>> {
	private static final ResourceLocation GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/entities/necromancer_mob_glow.png");
	private static final int GLOW_COLOR = 0x80FFFFFF;

	public NecromancerGlowLayer(RenderLayerParent<NecromancerEntity, ModelNecromancerModel<NecromancerEntity>> parent) {
		super(parent);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, NecromancerEntity entity,
			float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
		if (entity.isInvisible())
			return;

		VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.eyes(GLOW_TEXTURE));
		this.getParentModel().renderToBuffer(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, GLOW_COLOR);
	}
}
