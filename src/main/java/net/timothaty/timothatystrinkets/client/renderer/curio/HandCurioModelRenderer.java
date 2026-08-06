package net.timothaty.timothatystrinkets.client.renderer.curio;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.timothaty.timothatystrinkets.client.model.curio.HandCurioArmModel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.IdentityHashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class HandCurioModelRenderer {
	public static final HandCurioModelRenderer INSTANCE = new HandCurioModelRenderer();

	private final Map<HandCurioVisualDefinition, ModelPair> models = new IdentityHashMap<>();

	private HandCurioModelRenderer() {
	}

	public void render(
			AbstractClientPlayer player,
			HumanoidArm arm,
			HumanoidModel<?> sourceModel,
			ResolvedHandCurioVisuals visuals,
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			int light
	) {
		visuals.primaryGauntlet().ifPresent(visual ->
				renderVisual(player, arm, sourceModel, visual, poseStack, bufferSource, light));
		for (ResolvedHandCurioVisuals.ResolvedVisual accessory : visuals.accessories())
			renderVisual(player, arm, sourceModel, accessory, poseStack, bufferSource, light);
	}

	private void renderVisual(
			AbstractClientPlayer player,
			HumanoidArm arm,
			HumanoidModel<?> sourceModel,
			ResolvedHandCurioVisuals.ResolvedVisual visual,
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			int light
	) {
		HandCurioVisualDefinition definition = visual.definition();
		HandCurioArmModel model = getModels(definition).select(isSlimPlayer(player));
		model.copyArmPoseFrom(sourceModel);

		ResourceLocation texture = definition.textureResolver().resolve(
				player,
				arm,
				visual.renderedStack()
		);
		VertexConsumer consumer = ItemRenderer.getArmorFoilBuffer(
				bufferSource,
				RenderType.armorCutoutNoCull(texture),
				visual.renderedStack().hasFoil()
		);

		poseStack.pushPose();
		try {
			model.renderArm(arm, poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
		} finally {
			poseStack.popPose();
		}
	}

	private ModelPair getModels(HandCurioVisualDefinition definition) {
		return this.models.computeIfAbsent(definition, key -> new ModelPair(
				key.modelFactory().apply(Minecraft.getInstance().getEntityModels().bakeLayer(key.wideModelLayer())),
				key.modelFactory().apply(Minecraft.getInstance().getEntityModels().bakeLayer(key.slimModelLayer()))
		));
	}

	private static boolean isSlimPlayer(AbstractClientPlayer player) {
		return player.getSkin().model() == PlayerSkin.Model.SLIM;
	}

	private record ModelPair(HandCurioArmModel wide, HandCurioArmModel slim) {
		private HandCurioArmModel select(boolean slimPlayer) {
			return slimPlayer ? this.slim : this.wide;
		}
	}
}
