package net.timothaty.timothatystrinkets.client;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public class ResonanceCageRenderHideClient {
	private static final int MODEL_HIDE_DELAY_TICKS = 3;

	@SubscribeEvent
	public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
		if (shouldHide(event.getEntity())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
		LivingEntity entity = event.getEntity();
		if (entity instanceof Player) {
			return;
		}

		if (shouldHide(entity)) {
			event.setCanceled(true);
		}
	}

	private static boolean shouldHide(LivingEntity entity) {
		if (entity == null || !entity.hasEffect(TimothatysTrinketsModMobEffects.RESONANCE_CAGE)) {
			return false;
		}

		var effect = entity.getEffect(TimothatysTrinketsModMobEffects.RESONANCE_CAGE);
		if (effect == null) {
			return false;
		}

		return effect.getDuration() <= Math.max(1, EchoSphereCageDuration.CLIENT_TOTAL_DURATION - MODEL_HIDE_DELAY_TICKS);
	}

	private static final class EchoSphereCageDuration {
		private static final int CLIENT_TOTAL_DURATION = 50;
	}
}
