package net.timothaty.timothatystrinkets.client;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;

import net.timothaty.timothatystrinkets.block.DebtlordsHeadBlock;
import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public class DebtlordHeadSkullClient {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "textures/entities/debtlord.png");

	@SubscribeEvent
	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(DebtlordHeadSkullModel.LAYER_LOCATION, DebtlordHeadSkullModel::createBodyLayer);
	}

	@SubscribeEvent
	public static void createSkullModels(EntityRenderersEvent.CreateSkullModels event) {
		SkullBlockRenderer.SKIN_BY_TYPE.put(DebtlordsHeadBlock.DEBTLORD_TYPE, TEXTURE);
		event.registerSkullModel(DebtlordsHeadBlock.DEBTLORD_TYPE, new DebtlordHeadSkullModel(event.getEntityModelSet().bakeLayer(DebtlordHeadSkullModel.LAYER_LOCATION)));
	}
}
