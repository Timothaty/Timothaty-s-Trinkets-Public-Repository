package net.timothaty.timothatystrinkets.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.timothaty.timothatystrinkets.block.DamnationAltarBlock;
import net.timothaty.timothatystrinkets.block.entity.DamnationAltarBlockEntity;
import net.timothaty.timothatystrinkets.mechanics.damnation_altar.DamnationAltarSlot;
import net.timothaty.timothatystrinkets.mechanics.damnation_altar.DamnationAltarSlotLayout;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class DamnationAltarBlockEntityRenderer implements BlockEntityRenderer<DamnationAltarBlockEntity> {
	private static final float OUTER_ITEM_SCALE = 0.50F;
	private static final float CENTER_ITEM_SCALE = 0.58F;
	private static final float OFFER_ITEM_SCALE = 1.0F;
	private static final float OUTER_ITEM_X_ROTATION = 90.0F;
	private static final float CENTER_ROTATION_RADIANS_PER_TICK = 1.0F / 20.0F;
	private static final double OFFER_RENDER_Y = 1.55D;

	private final ItemRenderer itemRenderer;

	public DamnationAltarBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
		itemRenderer = context.getItemRenderer();
	}

	@Override
	public void render(DamnationAltarBlockEntity altar, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
		Direction facing = altar.getBlockState().getValue(DamnationAltarBlock.FACING);
		for (DamnationAltarSlot slot : DamnationAltarSlot.OUTER_SLOTS) {
			renderOuterItem(altar, slot, altar.getStack(slot), facing, poseStack, buffer, packedLight);
		}
		if (!altar.hasExternalOffer()) {
			renderCenterItem(
					altar,
					altar.getStack(DamnationAltarSlot.CENTER),
					facing,
					partialTick,
					poseStack,
					buffer,
					packedLight
			);
		}
		renderExternalOffer(altar, altar.getExternalOffer(), partialTick, poseStack, buffer, packedLight);
	}

	private void renderOuterItem(DamnationAltarBlockEntity altar, DamnationAltarSlot slot, ItemStack stack, Direction facing,
			PoseStack poseStack, MultiBufferSource itemBuffer, int packedLight) {
		if (stack.isEmpty() || altar.getLevel() == null) return;
		DamnationAltarSlotLayout.LocalPoint position = DamnationAltarSlotLayout.getWorldPosition(facing, slot);
		poseStack.pushPose();
		poseStack.translate(position.x(), DamnationAltarSlotLayout.OUTER_ITEM_Y, position.z());
		poseStack.mulPose(Axis.XP.rotationDegrees(OUTER_ITEM_X_ROTATION));
		poseStack.scale(OUTER_ITEM_SCALE, OUTER_ITEM_SCALE, OUTER_ITEM_SCALE);
		itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, poseStack, itemBuffer, altar.getLevel(), slot.index());
		poseStack.popPose();
	}

	private void renderCenterItem(DamnationAltarBlockEntity altar, ItemStack stack, Direction facing, float partialTick,
			PoseStack poseStack, MultiBufferSource itemBuffer, int packedLight) {
		if (stack.isEmpty() || altar.getLevel() == null) return;
		DamnationAltarSlotLayout.LocalPoint position = DamnationAltarSlotLayout.getWorldPosition(facing, DamnationAltarSlot.CENTER);
		poseStack.pushPose();
		poseStack.translate(position.x(), altar.getCenterRenderY(partialTick), position.z());
		float age = altar.getLevel().getGameTime() + partialTick;
		poseStack.mulPose(Axis.YP.rotation(age * CENTER_ROTATION_RADIANS_PER_TICK));
		poseStack.scale(CENTER_ITEM_SCALE, CENTER_ITEM_SCALE, CENTER_ITEM_SCALE);
		itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, poseStack, itemBuffer, altar.getLevel(), DamnationAltarSlot.CENTER.index());
		poseStack.popPose();
	}

	private void renderExternalOffer(DamnationAltarBlockEntity altar, ItemStack stack, float partialTick,
			PoseStack poseStack, MultiBufferSource itemBuffer, int packedLight) {
		if (stack.isEmpty() || altar.getLevel() == null) return;
		poseStack.pushPose();
		poseStack.translate(0.5D, OFFER_RENDER_Y, 0.5D);
		float age = altar.getLevel().getGameTime() + partialTick;
		float phase = (altar.getBlockPos().hashCode() & 1023) * ((float) Math.PI * 2.0F / 1024.0F);
		poseStack.mulPose(Axis.YP.rotation(age * CENTER_ROTATION_RADIANS_PER_TICK + phase));
		poseStack.scale(OFFER_ITEM_SCALE, OFFER_ITEM_SCALE, OFFER_ITEM_SCALE);
		itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY,
				poseStack, itemBuffer, altar.getLevel(), 31);
		poseStack.popPose();
	}
}
