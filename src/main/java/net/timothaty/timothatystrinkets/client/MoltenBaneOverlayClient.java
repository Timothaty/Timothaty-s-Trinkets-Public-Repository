package net.timothaty.timothatystrinkets.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class MoltenBaneOverlayClient {
	private static final ResourceLocation EMBER_OVERLAY = ResourceLocation.fromNamespaceAndPath(
			TimothatysTrinketsMod.MODID,
			"textures/entities/overlays/ember_overlay.png"
	);
	private MoltenBaneOverlayClient() {
	}

	public static void activateVisual(int entityId, int durationTicks) {
	}

	@SubscribeEvent
	public static void addLayers(EntityRenderersEvent.AddLayers event) {
		for (EntityType<?> entityType : event.getEntityTypes()) {
			tryAddLayer(event.getRenderer(entityType));
		}

		for (var skin : event.getSkins()) {
			tryAddLayer(event.getSkin(skin));
		}
	}

	private static void tryAddLayer(EntityRenderer<?> renderer) {
		if (renderer instanceof LivingEntityRenderer<?, ?> livingRenderer) {
			addMoltenBaneLayer(livingRenderer);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static <T extends LivingEntity, M extends EntityModel<T>> void addMoltenBaneLayer(LivingEntityRenderer<?, ?> renderer) {
		LivingEntityRenderer<T, M> typedRenderer = (LivingEntityRenderer<T, M>) renderer;
		typedRenderer.addLayer(new MoltenBaneEnergySwirlLayer<>(typedRenderer));
	}

	private static final class MoltenBaneEnergySwirlLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
		private MoltenBaneEnergySwirlLayer(LivingEntityRenderer<T, M> renderer) {
			super(renderer);
		}

		@Override
		public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
			MobEffectInstance effect = entity.getEffect(TimothatysTrinketsModMobEffects.MOLTEN_BANE);
			if (effect == null || entity.isInvisible())
				return;

			float f = entity.tickCount + partialTicks;
			M model = this.getParentModel();
			model.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks);
			this.getParentModel().copyPropertiesTo(model);

			VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.energySwirl(EMBER_OVERLAY, f * 0.01F % 1.0F, f * 0.01F % 1.0F));
			model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, getOverlayColor(effect));
		}

		private static int getOverlayColor(MobEffectInstance effect) {
			int alpha = 255;
			if (effect != null && effect.getDuration() < 10) {
				alpha = (int) (255.0F * (effect.getDuration() / 10.0F));
			}

			alpha = Mth.clamp(alpha, 0, 255);
			return (alpha << 24) | 0xFF6A00;
		}
	}
}
