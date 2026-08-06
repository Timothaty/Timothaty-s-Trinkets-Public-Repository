package net.timothaty.timothatystrinkets.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.model.curio.ChampionsGauntletModel;
import net.timothaty.timothatystrinkets.client.model.curio.DuelistsGauntletModel;
import net.timothaty.timothatystrinkets.client.model.curio.UndeadKnightsArmletModel;
import net.timothaty.timothatystrinkets.client.renderer.curio.ChampionsGauntletCurioRenderer;
import net.timothaty.timothatystrinkets.client.renderer.curio.DuelistsGauntletCurioRenderer;
import net.timothaty.timothatystrinkets.client.renderer.curio.HandCurioVisualRegistry;
import net.timothaty.timothatystrinkets.client.renderer.curio.UndeadKnightsArmletCurioRenderer;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public final class CurioClientRenderSetup {
	private CurioClientRenderSetup() {
	}

	@SubscribeEvent
	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(ChampionsGauntletModel.WIDE_LAYER_LOCATION, ChampionsGauntletModel::createWideBodyLayer);
		event.registerLayerDefinition(ChampionsGauntletModel.SLIM_LAYER_LOCATION, ChampionsGauntletModel::createSlimBodyLayer);
		event.registerLayerDefinition(DuelistsGauntletModel.WIDE_LAYER_LOCATION, DuelistsGauntletModel::createWideBodyLayer);
		event.registerLayerDefinition(DuelistsGauntletModel.SLIM_LAYER_LOCATION, DuelistsGauntletModel::createSlimBodyLayer);
		event.registerLayerDefinition(UndeadKnightsArmletModel.WIDE_LAYER_LOCATION, UndeadKnightsArmletModel::createWideBodyLayer);
		event.registerLayerDefinition(UndeadKnightsArmletModel.SLIM_LAYER_LOCATION, UndeadKnightsArmletModel::createSlimBodyLayer);
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		if (!ModList.get().isLoaded("curios")) {
			return;
		}

		event.enqueueWork(() -> {
			HandCurioVisualRegistry.bootstrap();
			CuriosClientRegistration.registerRenderers();
		});
	}

	private static final class CuriosClientRegistration {
		private CuriosClientRegistration() {
		}

		private static void registerRenderers() {
			CuriosRendererRegistry.register(TimothatysTrinketsModItems.CHAMPIONS_GAUNTLET.get(), ChampionsGauntletCurioRenderer::new);
			CuriosRendererRegistry.register(TimothatysTrinketsModItems.DUELISTS_GAUNTLET.get(), DuelistsGauntletCurioRenderer::new);
			CuriosRendererRegistry.register(TimothatysTrinketsModItems.UNDEAD_KNIGHTS_ARMLET.get(), UndeadKnightsArmletCurioRenderer::new);
		}
	}
}
