package net.timothaty.timothatystrinkets.client;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.entity.DebtlordEntity;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.Tags;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class DebtlordInventoryDebugClient {
	private static final boolean DEBUG_ENABLED = false;
	private static final double WATCH_RADIUS = 64.0D;
	private static final int REAPPEAR_WINDOW_TICKS = 40;
	private static ItemStack previousSelectedStack = ItemStack.EMPTY;
	private static int previousSelectedSlot = -1;
	private static PendingDisappearance pendingDisappearance;

	private DebtlordInventoryDebugClient() {
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		if (!DEBUG_ENABLED)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null) {
			reset();
			return;
		}

		DebtlordEntity nearbyBoss = minecraft.level.getEntitiesOfClass(
			DebtlordEntity.class,
			player.getBoundingBox().inflate(WATCH_RADIUS),
			DebtlordEntity::isAlive
		).stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
		if (nearbyBoss == null) {
			reset();
			return;
		}

		int selectedSlot = player.getInventory().selected;
		ItemStack selectedStack = player.getInventory().getItem(selectedSlot);
		checkPendingReappearance(player, selectedSlot, selectedStack);

		if (previousSelectedSlot == selectedSlot
			&& !previousSelectedStack.isEmpty()
			&& isTrackedWeaponOrTool(previousSelectedStack)
			&& selectedStack.isEmpty()) {
			int relocatedSlot = findMatchingInventorySlot(player, previousSelectedStack, selectedSlot);
			String itemName = previousSelectedStack.getHoverName().getString();
			String location = relocatedSlot >= 0 ? "найден в слоте " + relocatedSlot : "в других слотах не найден";
			player.displayClientMessage(Component.literal(
				"[Debtlord inventory debug] возможное исчезновение: " + itemName
					+ ", слот " + selectedSlot
					+ ", clientTick " + player.tickCount
					+ ", " + location
					+ ", дистанция до босса " + String.format(java.util.Locale.ROOT, "%.1f", player.distanceTo(nearbyBoss))
			), false);
			if (relocatedSlot < 0)
				pendingDisappearance = new PendingDisappearance(previousSelectedStack.copy(), selectedSlot, player.tickCount);
		}

		previousSelectedSlot = selectedSlot;
		previousSelectedStack = selectedStack.copy();
	}

	private static void checkPendingReappearance(LocalPlayer player, int selectedSlot, ItemStack selectedStack) {
		if (pendingDisappearance == null)
			return;
		if (player.tickCount - pendingDisappearance.tick > REAPPEAR_WINDOW_TICKS) {
			pendingDisappearance = null;
			return;
		}
		if (selectedSlot == pendingDisappearance.slot
			&& !selectedStack.isEmpty()
			&& ItemStack.isSameItem(selectedStack, pendingDisappearance.stack)) {
			player.displayClientMessage(Component.literal(
				"[Debtlord inventory debug] подтверждено фантомное исчезновение: "
					+ selectedStack.getHoverName().getString()
					+ " вернулся в слот " + selectedSlot
					+ " через " + (player.tickCount - pendingDisappearance.tick) + " тиков"
			), false);
			pendingDisappearance = null;
		}
	}

	private static int findMatchingInventorySlot(LocalPlayer player, ItemStack expected, int excludedSlot) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			if (slot == excludedSlot)
				continue;
			ItemStack candidate = player.getInventory().getItem(slot);
			if (!candidate.isEmpty() && ItemStack.isSameItem(candidate, expected))
				return slot;
		}
		return -1;
	}

	private static boolean isTrackedWeaponOrTool(ItemStack stack) {
		return stack.is(Tags.Items.MELEE_WEAPON_TOOLS)
			|| stack.is(Tags.Items.RANGED_WEAPON_TOOLS)
			|| stack.is(Tags.Items.MINING_TOOL_TOOLS);
	}

	private static void reset() {
		previousSelectedStack = ItemStack.EMPTY;
		previousSelectedSlot = -1;
		pendingDisappearance = null;
	}

	private record PendingDisappearance(ItemStack stack, int slot, int tick) {
	}
}
