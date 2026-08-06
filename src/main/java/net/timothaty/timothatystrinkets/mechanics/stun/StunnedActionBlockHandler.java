package net.timothaty.timothatystrinkets.mechanics.stun;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.active_ability.PlayerActionLockHelper;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class StunnedActionBlockHandler {
	private StunnedActionBlockHandler() {
	}


	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onAttack(AttackEntityEvent event) {
		Player player = event.getEntity();
		if (shouldBlock(player)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
		Player player = event.getEntity();
		if (shouldBlock(player)) {
			event.setCancellationResult(InteractionResult.FAIL);
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		Player player = event.getEntity();
		if (shouldBlock(player)) {
			event.setCancellationResult(InteractionResult.FAIL);
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		Player player = event.getEntity();
		if (shouldBlock(player)) {
			event.setCancellationResult(InteractionResult.FAIL);
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		Player player = event.getEntity();
		if (shouldBlock(player)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onBreakBlock(BlockEvent.BreakEvent event) {
		Player player = event.getPlayer();
		if (shouldBlock(player)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
		Player player = event.getEntity();
		if (shouldBlock(player)) {
			event.setNewSpeed(0.0F);
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
		if (event.getEntity() instanceof Player player && shouldBlock(player)) {
			event.setCanceled(true);
		}
	}

	private static boolean shouldBlock(Player player) {
		return PlayerActionLockHelper.isActionBlocked(player);
	}
}
