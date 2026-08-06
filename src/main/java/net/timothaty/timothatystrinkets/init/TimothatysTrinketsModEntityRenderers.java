package net.timothaty.timothatystrinkets.init;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.timothaty.timothatystrinkets.client.renderer.*;
import net.minecraft.client.renderer.entity.NoopRenderer;

@EventBusSubscriber(Dist.CLIENT)
public class TimothatysTrinketsModEntityRenderers {
	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(TimothatysTrinketsModEntities.VFX_INDULGENCY_BLESSING.get(), VFXIndulgencyBlessingRenderer::new);
		event.registerEntityRenderer(TimothatysTrinketsModEntities.NECROMANCER.get(), NecromancerRenderer::new);
		event.registerEntityRenderer(TimothatysTrinketsModEntities.DEBTLORD.get(), DebtlordRenderer::new);
		event.registerEntityRenderer(TimothatysTrinketsModEntities.DEBTLORD_GROUND_DEBRIS.get(), DebtlordGroundDebrisRenderer::new);
		event.registerEntityRenderer(TimothatysTrinketsModEntities.UNDEAD_KNIGHT.get(), UndeadKnightRenderer::new);
		event.registerEntityRenderer(TimothatysTrinketsModEntities.SOUL_ORB.get(), SoulOrbRenderer::new);
		event.registerEntityRenderer(TimothatysTrinketsModEntities.TARGET_AREA.get(), TargetAreaRenderer::new);
		event.registerEntityRenderer(TimothatysTrinketsModEntities.BEATIFIC_PALLIUM.get(), BeatificPalliumRenderer::new);
		event.registerEntityRenderer(TimothatysTrinketsModEntities.CLEANSING_RITUAL_CONTROLLER.get(), NoopRenderer::new);
		event.registerEntityRenderer(TimothatysTrinketsModEntities.CLEANSING_DUST_MANIFESTATION.get(), CleansingDustManifestationRenderer::new);
	}
}
