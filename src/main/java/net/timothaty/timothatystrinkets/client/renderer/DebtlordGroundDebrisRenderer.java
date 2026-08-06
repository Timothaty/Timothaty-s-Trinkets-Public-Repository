package net.timothaty.timothatystrinkets.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

import net.timothaty.timothatystrinkets.entity.DebtlordGroundDebrisEntity;

public class DebtlordGroundDebrisRenderer extends EntityRenderer<DebtlordGroundDebrisEntity> {
	private final BlockRenderDispatcher blockRenderer;

	public DebtlordGroundDebrisRenderer(EntityRendererProvider.Context context) {
		super(context);
		blockRenderer = Minecraft.getInstance().getBlockRenderer();
		shadowRadius = 0.0F;
	}

	@Override
	public void render(DebtlordGroundDebrisEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		if (!entity.isDebrisVisible(partialTick))
			return;

		poseStack.pushPose();
		poseStack.translate(-0.5D, -1.0D + entity.getRenderYOffset(partialTick), -0.5D);
		blockRenderer.renderSingleBlock(entity.getDebrisBlockState(), poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
		poseStack.popPose();
	}

	@Override
	public ResourceLocation getTextureLocation(DebtlordGroundDebrisEntity entity) {
		return TextureAtlas.LOCATION_BLOCKS;
	}
}
