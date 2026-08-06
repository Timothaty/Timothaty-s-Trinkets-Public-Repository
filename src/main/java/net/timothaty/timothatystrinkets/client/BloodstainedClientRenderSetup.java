package net.timothaty.timothatystrinkets.client;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.render.layer.BloodstainedPlayerLayer;

import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class BloodstainedClientRenderSetup {
	private static final ResourceLocation WIDE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
		TimothatysTrinketsMod.MODID,
		"textures/entities/overlays/bloodstained_overlay_wide.png"
	);
	private static final ResourceLocation SLIM_TEXTURE = ResourceLocation.fromNamespaceAndPath(
		TimothatysTrinketsMod.MODID,
		"textures/entities/overlays/bloodstained_overlay_slim.png"
	);

	private BloodstainedClientRenderSetup() {
	}

	@SubscribeEvent
	public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
		addLayer(event.getSkin(PlayerSkin.Model.WIDE), WIDE_TEXTURE);
		addLayer(event.getSkin(PlayerSkin.Model.SLIM), SLIM_TEXTURE);
	}

	private static void addLayer(PlayerRenderer renderer, ResourceLocation texture) {
		if (renderer != null)
			renderer.addLayer(new BloodstainedPlayerLayer(renderer, texture));
	}
}
