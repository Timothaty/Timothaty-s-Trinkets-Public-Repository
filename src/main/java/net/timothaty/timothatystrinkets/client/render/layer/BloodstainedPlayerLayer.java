package net.timothaty.timothatystrinkets.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.bloodstained.BloodstainedData;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;

public final class BloodstainedPlayerLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	private final ResourceLocation texture;

	public BloodstainedPlayerLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ResourceLocation texture) {
		super(parent);
		this.texture = texture;
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
		MobEffectInstance effect = player.getEffect(TimothatysTrinketsModMobEffects.BLOODSTAINED);
		if (effect == null || player.isInvisible())
			return;

		int fadeTicks = Math.round(BloodstainedData.DEFAULT_DURATION_TICKS * BloodstainedData.FINAL_FADE_FRACTION);
		float alpha = effect.getDuration() > fadeTicks ? 1.0F : effect.getDuration() / (float) fadeTicks;
		int color = Mth.clamp(Math.round(alpha * 255.0F), 0, 255) << 24 | 0xFFFFFF;
		VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(this.texture));
		this.getParentModel().renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, color);
	}
}
