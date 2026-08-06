package net.timothaty.timothatystrinkets.mechanics.holy_rosarium;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class HolyRosariumEvents {
	private static final float HEALING_MULTIPLIER = 1.40F;

	private HolyRosariumEvents() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingHeal(LivingHealEvent event) {
		if (!(event.getEntity() instanceof Player player) || event.getAmount() <= 0.0F)
			return;
		if (!HolyRosariumHelper.hasActiveCombination(player, HolyRosariumBead.PENANCE, HolyRosariumBead.SACRAMENT))
			return;

		event.setAmount(event.getAmount() * HEALING_MULTIPLIER);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
		cancelUnholyRelicUse(event);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		cancelUnholyRelicUse(event);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
	public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
		cancelUnholyRelicUse(event);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
	public static void onRightClickEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
		cancelUnholyRelicUse(event);
	}

	private static void cancelUnholyRelicUse(PlayerInteractEvent event) {
		Player player = event.getEntity();
		if (!HolyRosariumHelper.isUnholyRelicSuppressed(player, event.getItemStack()))
			return;
		cancelInteraction(event, InteractionResult.FAIL);
	}

	private static void cancelInteraction(PlayerInteractEvent event, InteractionResult result) {
		if (event instanceof PlayerInteractEvent.RightClickItem rightClickItem) {
			rightClickItem.setCanceled(true);
			rightClickItem.setCancellationResult(result);
		} else if (event instanceof PlayerInteractEvent.RightClickBlock rightClickBlock) {
			rightClickBlock.setCanceled(true);
			rightClickBlock.setCancellationResult(result);
		} else if (event instanceof PlayerInteractEvent.EntityInteract entityInteract) {
			entityInteract.setCanceled(true);
			entityInteract.setCancellationResult(result);
		} else if (event instanceof PlayerInteractEvent.EntityInteractSpecific specific) {
			specific.setCanceled(true);
			specific.setCancellationResult(result);
		}
	}

}
