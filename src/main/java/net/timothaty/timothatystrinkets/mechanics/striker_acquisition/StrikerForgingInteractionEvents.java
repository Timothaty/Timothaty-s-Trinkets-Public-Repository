package net.timothaty.timothatystrinkets.mechanics.striker_acquisition;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class StrikerForgingInteractionEvents {
	private static final Map<Villager, Long> LAST_REFUSAL_TICK = new WeakHashMap<>();

	private StrikerForgingInteractionEvents() {
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onVillagerInteract(PlayerInteractEvent.EntityInteract event) {
		blockForgingTrade(event, event.getEntity(), event.getTarget());
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onVillagerInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
		blockForgingTrade(event, event.getEntity(), event.getTarget());
	}

	private static void blockForgingTrade(PlayerInteractEvent event, Player player, Entity target) {
		if (!(target instanceof Villager villager)
				|| player.isSecondaryUseActive()
				|| StrikerCommissionData.getStage(villager) != StrikerCommissionStage.FORGING)
			return;

		if (!player.level().isClientSide())
			playRefusalOnce(villager);

		InteractionResult result = InteractionResult.sidedSuccess(player.level().isClientSide());
		if (event instanceof PlayerInteractEvent.EntityInteract entityInteract) {
			entityInteract.setCanceled(true);
			entityInteract.setCancellationResult(result);
		} else if (event instanceof PlayerInteractEvent.EntityInteractSpecific specific) {
			specific.setCanceled(true);
			specific.setCancellationResult(result);
		}
	}

	private static void playRefusalOnce(Villager villager) {
		long now = villager.level().getGameTime();
		Long previousTick = LAST_REFUSAL_TICK.put(villager, now);
		if (previousTick != null && now - previousTick <= 2L)
			return;

		villager.setUnhappyCounter(40);
		villager.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
	}
}
