package net.timothaty.timothatystrinkets.client.angels_shroud;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.angels_shroud.AngelsShroudData;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class AngelsShroudEntityOverlay {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
			TimothatysTrinketsMod.MODID,
			"textures/entities/overlays/angels_shroud_overlay.png"
	);

	private AngelsShroudEntityOverlay() {
	}

	@SubscribeEvent
	public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
		addLayer(event.getSkin(PlayerSkin.Model.WIDE));
		addLayer(event.getSkin(PlayerSkin.Model.SLIM));
	}

	private static void addLayer(PlayerRenderer renderer) {
		if (renderer != null)
			renderer.addLayer(new EnergySwirlLayer(renderer));
	}

	private static final class EnergySwirlLayer
			extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
		private EnergySwirlLayer(PlayerRenderer renderer) {
			super(renderer);
		}

		@Override
		public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
				AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks,
				float ageInTicks, float netHeadYaw, float headPitch) {
			MobEffectInstance effect = player.getEffect(TimothatysTrinketsModMobEffects.ANGELS_SHROUD);
			if (effect == null || player.isInvisible())
				return;

			float time = player.tickCount + partialTicks;
			float u = time * 0.0040F % 1.0F;
			float v = time * 0.0025F % 1.0F;
			VertexConsumer consumer = buffer.getBuffer(RenderType.energySwirl(TEXTURE, u, v));
			this.getParentModel().renderToBuffer(
					poseStack,
					consumer,
					packedLight,
					OverlayTexture.NO_OVERLAY,
					overlayColor(effect, partialTicks)
			);
		}

		private static int overlayColor(MobEffectInstance effect, float partialTicks) {
			float remaining = Math.max(0.0F, effect.getDuration() - partialTicks);
			float fade = Mth.clamp(
					remaining / AngelsShroudData.VISUAL_FADE_OUT_TICKS,
					0.0F,
					1.0F
			);
			int alpha = Mth.clamp(Math.round(210.0F * fade), 0, 210);
			return alpha << 24 | AngelsShroudData.GOLD_RGB;
		}
	}
}
