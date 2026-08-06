package net.timothaty.timothatystrinkets.mechanics.indulgency;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.advancement.TimothatysTrinketsCriteriaTriggers;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaHelper;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestCeremonyService;
import net.timothaty.timothatystrinkets.mechanics.pillagers_coin.PillagersCoinExtortionManager;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class IndulgencyClericInteractionHandler {
	private static final int COOLDOWN_TICKS = 60;

	private IndulgencyClericInteractionHandler() {
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
	public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
		handleEvent(event, event.getLevel(), event.getEntity(), event.getTarget(), event.getItemStack());
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
	public static void onRightClickEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
		handleEvent(event, event.getLevel(), event.getEntity(), event.getTarget(), event.getItemStack());
	}


	private static void handleEvent(PlayerInteractEvent event, Level level, Player player, Entity target, ItemStack usedStack) {
		if (event.getHand() != InteractionHand.MAIN_HAND || !isIndulgencyInteraction(player, target, usedStack))
			return;

		if (event instanceof PlayerInteractEvent.EntityInteract entityInteract) {
			entityInteract.setCanceled(true);
			entityInteract.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
		} else if (event instanceof PlayerInteractEvent.EntityInteractSpecific specific) {
			specific.setCanceled(true);
			specific.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
		}

		if (level.isClientSide())
			return;
		if (target instanceof Villager villager && PillagersCoinExtortionManager.hasSession(villager)) {
			player.displayClientMessage(Component.translatable("message.timothatys_trinkets.cleric_quest.cleric_busy"), true);
			return;
		}

		Item indulgency = TimothatysTrinketsModItems.INDULGENCY.get();
		if (player.getCooldowns().isOnCooldown(indulgency))
			return;

		int currentLevel = AnathemaHelper.getLevel(player);
		if (currentLevel <= 0)
			return;

		int emeraldCost = currentLevel == 1 ? 0 : currentLevel + 1;
		if (!player.getAbilities().instabuild && countEmeralds(player) < emeraldCost) {
			player.displayClientMessage(Component.translatable("message.timothatys_trinkets.indulgency_missing_emeralds", emeraldCost), true);
			return;
		}

		if (!AnathemaHelper.reduceOneLevel(player))
			return;

		if (!player.getAbilities().instabuild) {
			removeEmeralds(player, emeraldCost);
			usedStack.shrink(1);
		}

		ClericQuestCeremonyService.playStandalone((ServerLevel) level, (Villager) target, player);
		player.getCooldowns().addCooldown(indulgency, COOLDOWN_TICKS);
		if (player instanceof ServerPlayer serverPlayer)
			TimothatysTrinketsCriteriaTriggers.triggerGiveIndulgencyToCleric(serverPlayer);
	}

	private static boolean isIndulgencyInteraction(Player player, Entity target, ItemStack stack) {
		return player != null
			&& player.isCrouching()
			&& target instanceof Villager villager
			&& villager.getVillagerData().getProfession() == VillagerProfession.CLERIC
			&& stack != null
			&& stack.is(TimothatysTrinketsModItems.INDULGENCY.get());
	}

	private static int countEmeralds(Player player) {
		int count = 0;
		for (ItemStack stack : player.getInventory().items) {
			if (stack.is(Items.EMERALD))
				count += stack.getCount();
		}
		return count;
	}

	private static void removeEmeralds(Player player, int amount) {
		int remaining = amount;
		for (ItemStack stack : player.getInventory().items) {
			if (remaining <= 0)
				break;
			if (!stack.is(Items.EMERALD))
				continue;

			int removed = Math.min(remaining, stack.getCount());
			stack.shrink(removed);
			remaining -= removed;
		}
		player.getInventory().setChanged();
	}
}
