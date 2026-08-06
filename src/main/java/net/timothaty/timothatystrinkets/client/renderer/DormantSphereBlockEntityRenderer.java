package net.timothaty.timothatystrinkets.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.timothaty.timothatystrinkets.block.entity.DormantSphereBlockEntity;
import net.timothaty.timothatystrinkets.client.model.ModelEchoSphere;
import net.timothaty.timothatystrinkets.client.model.animations.EchoSphereAnimations;

import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import org.joml.Vector3f;

public final class DormantSphereBlockEntityRenderer implements BlockEntityRenderer<DormantSphereBlockEntity> {
	private static final ResourceLocation CORE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
			"timothatys_trinkets",
			"textures/block/dormant_sphere_cube.png"
	);
	private static final ResourceLocation OUTLINE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
			"timothatys_trinkets",
			"textures/block/outline_dormant_sphere.png"
	);
	private static final long LOOP_TICKS = 10L * 20L;
	private static final Vector3f ANIMATION_VECTOR_CACHE = new Vector3f();

	private final ModelEchoSphere model;

	public DormantSphereBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
		this.model = new ModelEchoSphere(context.bakeLayer(ModelEchoSphere.LAYER_LOCATION));
	}

	@Override
	public void render(DormantSphereBlockEntity sphere, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
			int packedLight, int packedOverlay) {
		if (sphere.getLevel() == null) {
			return;
		}

		model.root().getAllParts().forEach(part -> part.resetPose());
		double phaseTicks = (sphere.getLevel().getGameTime() % LOOP_TICKS) + partialTick;
		long animationTimeMillis = (long) (phaseTicks * 50.0D);
		KeyframeAnimations.animate(model, EchoSphereAnimations.LOOP, animationTimeMillis, 1.0F, ANIMATION_VECTOR_CACHE);

		poseStack.pushPose();
		poseStack.translate(0.5D, 1.5D, 0.5D);
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		VertexConsumer coreConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(CORE_TEXTURE));
		model.renderCoreToBuffer(poseStack, coreConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, sphere.getCoreColor());

		VertexConsumer outlineConsumer = buffer.getBuffer(RenderType.entityCutout(OUTLINE_TEXTURE));
		model.renderOutlineToBuffer(poseStack, outlineConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, sphere.getOutlineColor());
		poseStack.popPose();
	}
}
