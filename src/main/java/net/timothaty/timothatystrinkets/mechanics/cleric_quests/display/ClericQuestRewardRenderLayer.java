package net.timothaty.timothatystrinkets.mechanics.cleric_quests.display;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class ClericQuestRewardRenderLayer extends RenderLayer<Villager, VillagerModel<Villager>> {
	private final ItemInHandRenderer itemRenderer;
	private final ItemStack humilityStack;
	private final ItemStack sacramentStack;

	private ClericQuestRewardRenderLayer(RenderLayerParent<Villager, VillagerModel<Villager>> parent, ItemInHandRenderer itemRenderer) {
		super(parent);
		this.itemRenderer = itemRenderer;
		this.humilityStack = new ItemStack(TimothatysTrinketsModItems.BEAD_OF_HUMILITY.get());
		this.sacramentStack = new ItemStack(TimothatysTrinketsModItems.BEAD_OF_THE_SACRAMENT.get());
	}

	@SubscribeEvent
	public static void addLayer(EntityRenderersEvent.AddLayers event) {
		VillagerRenderer renderer = event.getRenderer(EntityType.VILLAGER);
		if (renderer != null)
			renderer.addLayer(new ClericQuestRewardRenderLayer(renderer, event.getContext().getItemInHandRenderer()));
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Villager villager, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
		if (!(villager instanceof ClericQuestRewardDisplayState state))
			return;
		ItemStack stack = switch (state.timothatys_trinkets$getClericQuestRewardDisplay()) {
			case ClericQuestRewardDisplayState.HUMILITY -> humilityStack;
			case ClericQuestRewardDisplayState.SACRAMENT -> sacramentStack;
			default -> ItemStack.EMPTY;
		};
		if (stack.isEmpty())
			return;
		poseStack.pushPose();
		poseStack.translate(0.0F, 0.4F, -0.4F);
		poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
		itemRenderer.renderItem(villager, stack, ItemDisplayContext.GROUND, false, poseStack, buffer, packedLight);
		poseStack.popPose();
	}
}
