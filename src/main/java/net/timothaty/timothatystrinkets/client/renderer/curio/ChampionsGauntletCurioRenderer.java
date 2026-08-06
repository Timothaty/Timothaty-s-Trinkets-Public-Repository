package net.timothaty.timothatystrinkets.client.renderer.curio;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.model.curio.ChampionsGauntletModel;
import net.timothaty.timothatystrinkets.util.CuriosHandsSlotHelper;

@OnlyIn(Dist.CLIENT)
public final class ChampionsGauntletCurioRenderer implements ICurioRenderer {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/curio/champions_gauntlet.png");

	private final ChampionsGauntletModel<LivingEntity> wideModel;
	private final ChampionsGauntletModel<LivingEntity> slimModel;

	public ChampionsGauntletCurioRenderer() {
		this.wideModel = new ChampionsGauntletModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ChampionsGauntletModel.WIDE_LAYER_LOCATION));
		this.slimModel = new ChampionsGauntletModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ChampionsGauntletModel.SLIM_LAYER_LOCATION));
	}

	@Override
	public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent,
			MultiBufferSource bufferSource, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
		if (!(renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel)) {
			return;
		}

		LivingEntity entity = slotContext.entity();
		if (entity == null) {
			return;
		}

		ChampionsGauntletModel<LivingEntity> model = this.getModel(entity);
		HumanoidArm arm = CuriosHandsSlotHelper.physicalArmForSlot(entity, slotContext.index());
		model.copyArmPoseFrom(humanoidModel);

		RenderType renderType = RenderType.armorCutoutNoCull(TEXTURE);
		VertexConsumer consumer = ItemRenderer.getArmorFoilBuffer(bufferSource, renderType, stack.hasFoil());

		poseStack.pushPose();
		try {
			model.renderArm(arm, poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
		} finally {
			poseStack.popPose();
		}
	}

	private ChampionsGauntletModel<LivingEntity> getModel(LivingEntity entity) {
		return isSlimPlayer(entity) ? this.slimModel : this.wideModel;
	}

	private static boolean isSlimPlayer(LivingEntity entity) {
		return entity instanceof AbstractClientPlayer player && player.getSkin().model() == PlayerSkin.Model.SLIM;
	}
}
