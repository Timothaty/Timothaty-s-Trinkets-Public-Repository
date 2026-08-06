package net.timothaty.timothatystrinkets.init;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.timothaty.timothatystrinkets.client.model.Modelundeadknight;
import net.timothaty.timothatystrinkets.client.model.Modelindulgency_vfx;
import net.timothaty.timothatystrinkets.client.model.ModelNecromancerModel;
import net.timothaty.timothatystrinkets.client.model.ModelDebtlordHead;
import net.timothaty.timothatystrinkets.client.model.ModelDebtlord;
import net.timothaty.timothatystrinkets.client.model.ModelEchoSphere;
import net.timothaty.timothatystrinkets.client.model.ModelBeatificPallium;

@EventBusSubscriber(Dist.CLIENT)
public class TimothatysTrinketsModModels {
	@SubscribeEvent
	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(Modelindulgency_vfx.LAYER_LOCATION, Modelindulgency_vfx::createBodyLayer);
		event.registerLayerDefinition(Modelundeadknight.LAYER_LOCATION, Modelundeadknight::createBodyLayer);
		event.registerLayerDefinition(ModelNecromancerModel.LAYER_LOCATION, ModelNecromancerModel::createBodyLayer);
		event.registerLayerDefinition(ModelDebtlordHead.LAYER_LOCATION, ModelDebtlordHead::createBodyLayer);
		event.registerLayerDefinition(ModelDebtlord.LAYER_LOCATION, ModelDebtlord::createBodyLayer);
		event.registerLayerDefinition(ModelEchoSphere.LAYER_LOCATION, ModelEchoSphere::createBodyLayer);
		event.registerLayerDefinition(ModelBeatificPallium.LAYER_LOCATION, ModelBeatificPallium::createBodyLayer);
	}
}
