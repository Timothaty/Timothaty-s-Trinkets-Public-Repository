package net.timothaty.timothatystrinkets.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import net.timothaty.timothatystrinkets.client.model.Modelindulgency_vfx;
import net.timothaty.timothatystrinkets.entity.VFXIndulgencyBlessingEntity;

public class VFXIndulgencyBlessingRenderer extends MobRenderer<VFXIndulgencyBlessingEntity, Modelindulgency_vfx<VFXIndulgencyBlessingEntity>> {

	private static final int FRAMES = 5;

	private static final float CYCLE_TICKS = 10.0F;

	public VFXIndulgencyBlessingRenderer(EntityRendererProvider.Context context) {
		super(context, new Modelindulgency_vfx<VFXIndulgencyBlessingEntity>(context.bakeLayer(Modelindulgency_vfx.LAYER_LOCATION)), 0.0f);
	}

	@Override
	public ResourceLocation getTextureLocation(VFXIndulgencyBlessingEntity entity) {
		return ResourceLocation.parse("timothatys_trinkets:textures/entities/indulgency_vfx_animated_1.png");
	}

	private static ResourceLocation getAnimatedTexture(VFXIndulgencyBlessingEntity entity, float partialTicks) {
		float ageTicks = entity.tickCount + partialTicks;

		int steps = (int) Math.floor((ageTicks * FRAMES) / CYCLE_TICKS);

		int frameIndex = Math.floorMod(steps, FRAMES);

		int frameNumber = frameIndex + 1;

		return ResourceLocation.parse("timothatys_trinkets:textures/entities/indulgency_vfx_animated_" + frameNumber + ".png");
	}

	@Override
	public void render(VFXIndulgencyBlessingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		poseStack.pushPose();

		float ageInTicks = entity.tickCount + partialTicks;

		this.setupRotations(entity, poseStack, ageInTicks, entityYaw, partialTicks, 1.0F);

		poseStack.scale(-1.0F, -1.0F, 1.0F);

		poseStack.translate(0.0D, -1.501D, 0.0D);

		poseStack.translate(0.0D, 0.002D, 0.0D);

		this.model.prepareMobModel(entity, 0.0F, 0.0F, partialTicks);
		this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

		float alpha = computeAlpha(entity, partialTicks);
		int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
		int packedColor = (a << 24) | 0xFFFFFF;

		int light = 15728880;
		int overlay = getOverlayCoords(entity, 0.0F);

		ResourceLocation tex = getAnimatedTexture(entity, partialTicks);

		RenderType rt = RenderType.entityTranslucentCull(tex);
		VertexConsumer vc = buffer.getBuffer(rt);
		this.model.renderToBuffer(poseStack, vc, light, overlay, packedColor);

		poseStack.popPose();
	}

	private static float computeAlpha(VFXIndulgencyBlessingEntity entity, float partialTicks) {
		float age = entity.tickCount + partialTicks;
		float life = (float) VFXIndulgencyBlessingEntity.LIFETIME_TICKS;
		float t = (life <= 0.0F) ? 0.0F : Mth.clamp(age / life, 0.0F, 1.0F);

		if (t <= 0.15F) {
			return Mth.clamp(t / 0.15F, 0.0F, 1.0F);
		}
		if (t <= 0.60F) {
			return 1.0F;
		}
		return Mth.clamp(1.0F - ((t - 0.60F) / 0.40F), 0.0F, 1.0F);
	}
}