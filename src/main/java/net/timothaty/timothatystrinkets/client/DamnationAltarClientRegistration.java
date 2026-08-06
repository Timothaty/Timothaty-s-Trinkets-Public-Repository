package net.timothaty.timothatystrinkets.client;

import net.timothaty.timothatystrinkets.client.renderer.DamnationAltarBlockEntityRenderer;
import net.timothaty.timothatystrinkets.client.renderer.DormantSphereBlockEntityRenderer;
import net.timothaty.timothatystrinkets.client.renderer.EchoSphereBlockEntityRenderer;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModBlockEntities;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(Dist.CLIENT)
public final class DamnationAltarClientRegistration {
	private DamnationAltarClientRegistration() {
	}

	@SubscribeEvent
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(TimothatysTrinketsModBlockEntities.DAMNATION_ALTAR.get(), DamnationAltarBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(TimothatysTrinketsModBlockEntities.DORMANT_SPHERE.get(), DormantSphereBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(TimothatysTrinketsModBlockEntities.ECHO_SPHERE.get(), EchoSphereBlockEntityRenderer::new);
	}
}
