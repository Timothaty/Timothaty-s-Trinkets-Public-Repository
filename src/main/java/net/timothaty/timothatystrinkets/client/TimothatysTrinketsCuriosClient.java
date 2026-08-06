package net.timothaty.timothatystrinkets.client;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumTooltip;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumTooltip;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public class TimothatysTrinketsCuriosClient {
	@SubscribeEvent
	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(
				OrbitingOrbCurioRenderer.VoidSphereModel.LAYER_LOCATION,
				OrbitingOrbCurioRenderer.VoidSphereModel::createBodyLayer
		);
	}

	@SubscribeEvent
	public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
		event.register(HolyRosariumTooltip.class, HolyRosariumClientTooltip::new);
		event.register(CorruptedRosariumTooltip.class, CorruptedRosariumClientTooltip::new);
	}
}
