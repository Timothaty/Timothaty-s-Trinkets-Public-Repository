package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.cherubims_wisdom;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumBead;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumHelper;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumState;

import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class CherubimsWisdomStateEvents {
	private CherubimsWisdomStateEvents() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onPlayerTick(PlayerTickEvent.Pre event) {
		if (!(event.getEntity() instanceof ServerPlayer player)
				|| !player.hasEffect(TimothatysTrinketsModMobEffects.CHERUBIMS_WISDOM))
			return;
		validateSource(player);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onCurioChanged(CurioChangeEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			HolyRosariumState.markDirty(player);
			HolyRosariumState.refreshNow(player);
			validateSource(player);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onEffectAdded(MobEffectEvent.Added event) {
		if (event.getEntity() instanceof ServerPlayer player)
			validateSource(player);
	}

	private static void validateSource(ServerPlayer player) {
		if (!player.hasEffect(TimothatysTrinketsModMobEffects.CHERUBIMS_WISDOM))
			return;

		// Comparable Rosarium abilities are dispelled when their cached source becomes invalid.
		if (!HolyRosariumHelper.hasActiveCombination(
				player,
				HolyRosariumBead.RESURRECTION,
				HolyRosariumBead.SACRAMENT
			)) {
			player.removeEffect(TimothatysTrinketsModMobEffects.CHERUBIMS_WISDOM);
		}
	}
}
