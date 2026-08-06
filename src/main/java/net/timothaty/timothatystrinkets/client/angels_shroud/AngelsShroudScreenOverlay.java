package net.timothaty.timothatystrinkets.client.angels_shroud;

import com.mojang.blaze3d.systems.RenderSystem;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.angels_shroud.AngelsShroudData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class AngelsShroudScreenOverlay {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
			TimothatysTrinketsMod.MODID,
			"textures/screens/screen_overlays/angelic_shroud_screen_overlay.png"
	);
	private static final int TEXTURE_WIDTH = 100;
	private static final int TEXTURE_HEIGHT = 58;
	private static final float MAX_ALPHA = 0.58F;

	private AngelsShroudScreenOverlay() {
	}

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui
				|| minecraft.player.isSpectator()) {
			return;
		}

		MobEffectInstance effect = minecraft.player.getEffect(TimothatysTrinketsModMobEffects.ANGELS_SHROUD);
		if (effect == null)
			return;

		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		float remaining = Math.max(0.0F, effect.getDuration() - partialTick);
		float elapsed = Math.max(0.0F, AngelsShroudData.DURATION_TICKS - remaining);
		float fadeIn = Mth.clamp(elapsed / AngelsShroudData.SCREEN_FADE_IN_TICKS, 0.0F, 1.0F);
		float fadeOut = Mth.clamp(remaining / AngelsShroudData.SCREEN_FADE_OUT_TICKS, 0.0F, 1.0F);
		float alpha = MAX_ALPHA * fadeIn * fadeOut;
		if (alpha <= 0.002F)
			return;

		GuiGraphics gui = event.getGuiGraphics();
		int width = minecraft.getWindow().getGuiScaledWidth();
		int height = minecraft.getWindow().getGuiScaledHeight();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		gui.setColor(1.0F, 1.0F, 1.0F, alpha);
		gui.blit(
				TEXTURE,
				0,
				0,
				width,
				height,
				0.0F,
				0.0F,
				TEXTURE_WIDTH,
				TEXTURE_HEIGHT,
				TEXTURE_WIDTH,
				TEXTURE_HEIGHT
		);
		gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
	}
}
