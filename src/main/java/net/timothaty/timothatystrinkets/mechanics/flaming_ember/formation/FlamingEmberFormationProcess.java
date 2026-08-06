package net.timothaty.timothatystrinkets.mechanics.flaming_ember.formation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.neoforged.neoforge.network.PacketDistributor;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.mechanics.flaming_ember.FlamingEmberData;
import net.timothaty.timothatystrinkets.network.FlamingEmberFormationVisualMessage;

import java.util.UUID;

public final class FlamingEmberFormationProcess {
	private FlamingEmberFormationProcess() {
	}

	public static void tick(ServerPlayer player) {
		FlamingEmberFormationState.Snapshot state = FlamingEmberFormationState.get(player);
		if (!state.active()) {
			tryStart(player);
			return;
		}

		continueProcess(player, state);
	}

	public static void handleToss(ServerPlayer player, ItemStack tossedStack) {
		FlamingEmberFormationState.Snapshot state = FlamingEmberFormationState.get(player);
		if (state.active() && FlamingEmberFormationData.matchesToken(tossedStack, state.token())) {
			reset(player, tossedStack);
		} else if (FlamingEmberFormationData.hasFormationData(tossedStack)) {
			syncVisualState(player, FlamingEmberFormationData.getToken(tossedStack), 0, false);
			FlamingEmberFormationData.clear(tossedStack);
		}
	}

	public static void reset(ServerPlayer player) {
		reset(player, ItemStack.EMPTY);
	}

	public static void clearAll(ServerPlayer player) {
		FlamingEmberFormationState.Snapshot state = FlamingEmberFormationState.get(player);
		if (state.active())
			syncVisualState(player, state.token(), state.progress(), false);
		FlamingEmberFormationState.clear(player);
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			FlamingEmberFormationData.clear(player.getInventory().getItem(slot));
		}
	}

	private static void tryStart(ServerPlayer player) {
		long currentGameTime = player.level().getGameTime();
		if (Math.floorMod(currentGameTime + player.getId(), FlamingEmberFormationData.ENVIRONMENT_CHECK_INTERVAL_TICKS) != 0L)
			return;
		if (!FlamingEmberFormationEnvironment.isPlayerEligible(player))
			return;

		InteractionHand hand = findStartingHand(player);
		if (hand == null || !FlamingEmberFormationEnvironment.hasSuitableLavaLake(player))
			return;

		ItemStack charcoal = player.getItemInHand(hand);
		String token = UUID.randomUUID().toString();
		FlamingEmberFormationData.writeToken(charcoal, token);
		FlamingEmberFormationState.start(player, token, hand, currentGameTime);
		syncVisualState(player, token, 0, true);
	}

	private static void continueProcess(ServerPlayer player, FlamingEmberFormationState.Snapshot state) {
		if (!hasValidIdentity(player, state) || !FlamingEmberFormationEnvironment.isPlayerEligible(player)) {
			reset(player);
			return;
		}

		long currentGameTime = player.level().getGameTime();
		if (currentGameTime - state.lastEnvironmentCheckTick() >= FlamingEmberFormationData.ENVIRONMENT_CHECK_INTERVAL_TICKS) {
			boolean environmentValid = FlamingEmberFormationEnvironment.hasSuitableLavaLake(player);
			FlamingEmberFormationState.updateEnvironment(player, environmentValid, currentGameTime);
			state = FlamingEmberFormationState.get(player);
		}

		if (!state.environmentValid()) {
			if (currentGameTime - state.lastValidEnvironmentTick() > FlamingEmberFormationData.ENVIRONMENT_GRACE_TICKS) {
				reset(player);
			} else {
				syncVisualStateIfNeeded(player, state.token(), state.progress(), currentGameTime);
			}
			return;
		}

		int progress = FlamingEmberFormationState.advanceProgress(player);
		if (isFireDamageTick(progress)) {
			player.hurt(player.damageSources().inFire(), 1.0F);
			FlamingEmberFormationState.Snapshot afterDamage = FlamingEmberFormationState.get(player);
			if (!afterDamage.active() || !hasValidIdentity(player, afterDamage) || !player.isAlive())
				return;
			state = afterDamage;
		}

		if (progress >= FlamingEmberFormationData.DURATION_TICKS) {
			complete(player);
			return;
		}

		syncVisualStateIfNeeded(player, state.token(), progress, currentGameTime);
	}

	private static void complete(ServerPlayer player) {
		FlamingEmberFormationState.Snapshot state = FlamingEmberFormationState.get(player);
		if (!state.active() || state.progress() < FlamingEmberFormationData.DURATION_TICKS || !hasValidIdentity(player, state)) {
			reset(player);
			return;
		}

		ItemStack charcoal = player.getItemInHand(state.hand());
		if (player.getRandom().nextDouble() < FlamingEmberFormationData.SUCCESS_CHANCE) {
			completeSuccessfully(player, state.hand(), charcoal);
		} else {
			completeWithFailure(player, state.hand(), charcoal);
		}
	}

	private static void completeSuccessfully(ServerPlayer player, InteractionHand hand, ItemStack charcoal) {
		FlamingEmberFormationState.Snapshot state = FlamingEmberFormationState.get(player);
		syncVisualState(player, state.token(), state.progress(), false);
		FlamingEmberFormationData.clear(charcoal);
		ItemStack result = new ItemStack(TimothatysTrinketsModItems.FLAMING_EMBER.get());
		FlamingEmberData.setHeat(result, 40.0D);
		player.setItemInHand(hand, result);
		FlamingEmberFormationState.clear(player);
		FlamingEmberFormationEffects.playSuccess(player, hand);
	}

	private static void completeWithFailure(ServerPlayer player, InteractionHand hand, ItemStack charcoal) {
		FlamingEmberFormationState.Snapshot state = FlamingEmberFormationState.get(player);
		syncVisualState(player, state.token(), state.progress(), false);
		ItemStack particleStack = new ItemStack(Items.CHARCOAL);
		FlamingEmberFormationData.clear(charcoal);
		player.setItemInHand(hand, ItemStack.EMPTY);
		FlamingEmberFormationState.clear(player);
		FlamingEmberFormationEffects.playFailure(player, hand, particleStack);
	}

	private static void reset(ServerPlayer player, ItemStack outsideInventory) {
		FlamingEmberFormationState.Snapshot state = FlamingEmberFormationState.get(player);
		String token = state.token();
		if (token == null || token.isEmpty()) {
			clearAll(player);
			return;
		}

		syncVisualState(player, token, state.progress(), false);
		if (FlamingEmberFormationData.matchesToken(outsideInventory, token))
			FlamingEmberFormationData.clear(outsideInventory);
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (FlamingEmberFormationData.matchesToken(stack, token))
				FlamingEmberFormationData.clear(stack);
		}
		FlamingEmberFormationState.clear(player);
	}

	private static boolean hasValidIdentity(ServerPlayer player, FlamingEmberFormationState.Snapshot state) {
		if (!state.active() || state.hand() == null || state.token() == null || state.token().isEmpty())
			return false;
		ItemStack stack = player.getItemInHand(state.hand());
		return isSingleCharcoal(stack) && FlamingEmberFormationData.matchesToken(stack, state.token());
	}

	private static InteractionHand findStartingHand(ServerPlayer player) {
		if (isSingleCharcoal(player.getMainHandItem()))
			return InteractionHand.MAIN_HAND;
		if (isSingleCharcoal(player.getOffhandItem()))
			return InteractionHand.OFF_HAND;
		return null;
	}

	private static boolean isSingleCharcoal(ItemStack stack) {
		return stack.is(Items.CHARCOAL) && stack.getCount() == 1;
	}

	private static boolean isFireDamageTick(int progress) {
		return progress == 340 || progress == 360 || progress == 380;
	}

	private static void syncVisualStateIfNeeded(ServerPlayer player, String token, int progress, long currentGameTime) {
		if (Math.floorMod(currentGameTime, FlamingEmberFormationData.VISUAL_UPDATE_INTERVAL_TICKS) == 0L) {
			syncVisualState(player, token, progress, true);
		}
	}

	private static void syncVisualState(ServerPlayer player, String token, int progress, boolean active) {
		if (token == null || token.isEmpty())
			return;
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
				new FlamingEmberFormationVisualMessage(token, progress, active));
	}
}
