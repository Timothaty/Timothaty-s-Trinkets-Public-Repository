package net.timothaty.timothatystrinkets.client.renderer.curio;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.model.curio.UndeadKnightsArmletModel;
import net.timothaty.timothatystrinkets.mechanics.armlet_gauntlet_synergy.ArmletGauntletSynergyState;
import net.timothaty.timothatystrinkets.util.CuriosHandsSlotHelper;

@OnlyIn(Dist.CLIENT)
public final class UndeadKnightsArmletCurioRenderer implements ICurioRenderer {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/curio/undead_knights_armlet.png");
	private static final ResourceLocation RIVETS_TEXTURE = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/curio/undead_knights_armlet_rivets.png");

	private final UndeadKnightsArmletModel<LivingEntity> wideModel;
	private final UndeadKnightsArmletModel<LivingEntity> slimModel;

	public UndeadKnightsArmletCurioRenderer() {
		this.wideModel = new UndeadKnightsArmletModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(UndeadKnightsArmletModel.WIDE_LAYER_LOCATION));
		this.slimModel = new UndeadKnightsArmletModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(UndeadKnightsArmletModel.SLIM_LAYER_LOCATION));
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

		HumanoidArm arm = CuriosHandsSlotHelper.physicalArmForSlot(entity, slotContext.index());
		UndeadKnightsArmletModel<LivingEntity> model = this.getModel(entity);
		model.copyArmPoseFrom(humanoidModel);

		boolean useRivets = entity instanceof net.minecraft.world.entity.player.Player player
				&& ArmletGauntletSynergyState.isPhysicalSynergyOnArm(player, arm);
		ResourceLocation texture = useRivets ? RIVETS_TEXTURE : TEXTURE;
		RenderType renderType = RenderType.armorCutoutNoCull(texture);
		VertexConsumer consumer = ItemRenderer.getArmorFoilBuffer(bufferSource, renderType, stack.hasFoil());

		poseStack.pushPose();
		try {
			model.renderArm(arm, poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
		} finally {
			poseStack.popPose();
		}
	}

	private UndeadKnightsArmletModel<LivingEntity> getModel(LivingEntity entity) {
		return isSlimPlayer(entity) ? this.slimModel : this.wideModel;
	}

	private static boolean isSlimPlayer(LivingEntity entity) {
		return entity instanceof AbstractClientPlayer player && player.getSkin().model() == PlayerSkin.Model.SLIM;
	}
}
