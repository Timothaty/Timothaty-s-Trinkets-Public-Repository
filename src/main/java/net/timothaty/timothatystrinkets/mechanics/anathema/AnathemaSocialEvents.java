package net.timothaty.timothatystrinkets.mechanics.anathema;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class AnathemaSocialEvents {
	private AnathemaSocialEvents() {
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onVillagerInteract(PlayerInteractEvent.EntityInteract event) {
		blockTradeIfNeeded(event, event.getEntity(), event.getTarget());
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onVillagerInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
		blockTradeIfNeeded(event, event.getEntity(), event.getTarget());
	}

	private static void blockTradeIfNeeded(PlayerInteractEvent event, Player player, Entity target) {
		if (!(target instanceof Villager villager) || player.isSecondaryUseActive() || isIndulgencyUse(event, player, villager))
			return;

		int level = AnathemaHelper.getLevel(player);
		if (level <= 0 || level == 1 && isLevelOneAllowedProfession(villager.getVillagerData().getProfession()))
			return;

		if (!player.level().isClientSide()) {
			villager.setUnhappyCounter(40);
			villager.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
		}

		if (event instanceof PlayerInteractEvent.EntityInteract entityInteract) {
			entityInteract.setCanceled(true);
			entityInteract.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide()));
		} else if (event instanceof PlayerInteractEvent.EntityInteractSpecific specific) {
			specific.setCanceled(true);
			specific.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide()));
		}
	}

	private static boolean isIndulgencyUse(PlayerInteractEvent event, Player player, Villager villager) {
		return event.getHand() == InteractionHand.MAIN_HAND
			&& player.isCrouching()
			&& villager.getVillagerData().getProfession() == VillagerProfession.CLERIC
			&& event.getItemStack().is(TimothatysTrinketsModItems.INDULGENCY.get());
	}

	private static boolean isLevelOneAllowedProfession(VillagerProfession profession) {
		return profession == VillagerProfession.LEATHERWORKER
			|| profession == VillagerProfession.WEAPONSMITH
			|| profession == VillagerProfession.ARMORER;
	}
}
