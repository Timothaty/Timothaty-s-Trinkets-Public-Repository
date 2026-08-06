package net.timothaty.timothatystrinkets.mechanics.cleric_quests;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility.HumilityQuestService;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility.HumilityStage;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.sacrament.SacramentOfferingType;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.sacrament.SacramentQuestService;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.sacrament.SacramentStage;
import net.timothaty.timothatystrinkets.mechanics.pillagers_coin.PillagersCoinExtortionManager;
import net.timothaty.timothatystrinkets.mechanics.pillagers_coin.PillagersCoinVillagerFearData;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class ClericQuestInteractionHandler {
	private ClericQuestInteractionHandler() {
	}

	@SubscribeEvent(priority = EventPriority.HIGH, receiveCanceled = true)
	public static void onVillagerInteract(PlayerInteractEvent.EntityInteract event) {
		if (event.isCanceled()
				|| event.getHand() != InteractionHand.MAIN_HAND
				|| !(event.getEntity() instanceof ServerPlayer player)
				|| !(event.getTarget() instanceof Villager cleric)
				|| !(player.level() instanceof ServerLevel level)
				|| !isEligibleCleric(cleric))
			return;

		ClericQuestSavedData data = ClericQuestSavedData.get(level);
		ClericQuestProgress progress = data.get(player.getUUID());
		boolean boundHere = progress != null && cleric.getUUID().equals(progress.activeClericId())
				&& level.dimension().equals(progress.activeClericDimension());

		if (PillagersCoinExtortionManager.hasSession(cleric)) {
			cancel(event, player);
			player.displayClientMessage(Component.translatable("message.timothatys_trinkets.cleric_quest.cleric_busy"), true);
			return;
		}
		if (PillagersCoinVillagerFearData.fears(cleric, player.getUUID())) {
			cancel(event, player);
			return;
		}
		if (ClericQuestRuntimeManager.isClericBusy(cleric.getUUID())) {
			cancel(event, player);
			player.displayClientMessage(Component.translatable("message.timothatys_trinkets.cleric_quest.cleric_busy"), true);
			return;
		}

		if (progress != null && boundHere && handleActiveQuest(event, level, cleric, player, progress))
			return;

		ItemStack stack = event.getItemStack();
		if (player.isCrouching() && stack.is(TimothatysTrinketsModItems.WOODEN_BEAD.get())
				&& (progress == null || (!progress.humilityCompleted() && progress.humilityStage() == HumilityStage.NONE))) {
			if (HumilityQuestService.begin(player, cleric)) {
				consumeOne(player, stack);
				cancel(event, player);
			}
			return;
		}

		SacramentOfferingType offering = player.isCrouching() ? SacramentOfferingType.fromStack(stack) : null;
		if (offering != null) {
			progress = data.get(player.getUUID());
			if (progress == null || !progress.humilityCompleted()) {
				cancel(event, player);
				ClericQuestDialogue.show(cleric, player, "dialogue.timothatys_trinkets.cleric.requires_humility");
				return;
			}
			if (progress.sacramentCompleted()) {
				cancel(event, player);
				ClericQuestDialogue.show(cleric, player, "dialogue.timothatys_trinkets.cleric.already_completed");
				return;
			}
			if (progress.sacramentStage() == SacramentStage.NONE && SacramentQuestService.offer(level, cleric, player, offering)) {
				consumeOne(player, stack);
				cancel(event, player);
				return;
			}
		}

		if (boundHere)
			refuseTrade(event, player, cleric);
	}

	private static boolean handleActiveQuest(PlayerInteractEvent.EntityInteract event, ServerLevel level, Villager cleric, ServerPlayer player, ClericQuestProgress progress) {
		if (progress.humilityStage() == HumilityStage.REWARD_READY) {
			HumilityQuestService.beginRewardCeremony(level, cleric, player);
			cancel(event, player);
			return true;
		}

		switch (progress.sacramentStage()) {
			case OFFERINGS -> {
				if (SacramentQuestService.hasAllOfferings(progress)) {
					SacramentQuestService.retryOfferingsCeremony(level, cleric, player);
					cancel(event, player);
					return true;
				}
				SacramentOfferingType offering = player.isCrouching() ? SacramentOfferingType.fromStack(event.getItemStack()) : null;
				if (offering != null) {
					boolean accepted = SacramentQuestService.offer(level, cleric, player, offering);
					if (accepted)
						consumeOne(player, event.getItemStack());
					cancel(event, player);
					return true;
				}
			}
			case HUNT_ACTIVE -> {
			}
			case HUNT_RETURN -> {
				SacramentQuestService.beginHuntReturnCeremony(level, cleric, player);
				cancel(event, player);
				return true;
			}
			case RESTART_REQUIRED -> {
				SacramentQuestService.beginRestartCeremony(level, cleric, player);
				cancel(event, player);
				return true;
			}
			case FAST_RETURN -> {
				SacramentQuestService.beginRewardCeremony(level, cleric, player);
				cancel(event, player);
				return true;
			}
			default -> {
			}
		}
		return false;
	}

	private static boolean isEligibleCleric(Villager villager) {
		return villager.isAlive()
			&& !villager.isBaby()
			&& villager.getVillagerData().getProfession() == VillagerProfession.CLERIC
			&& villager.getVillagerData().getLevel() >= 3;
	}

	private static void consumeOne(ServerPlayer player, ItemStack stack) {
		if (!player.getAbilities().instabuild)
			stack.shrink(1);
	}

	private static void refuseTrade(PlayerInteractEvent.EntityInteract event, ServerPlayer player, Villager cleric) {
		cleric.setUnhappyCounter(40);
		cleric.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
		cancel(event, player);
	}

	private static void cancel(PlayerInteractEvent.EntityInteract event, ServerPlayer player) {
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide()));
	}
}
