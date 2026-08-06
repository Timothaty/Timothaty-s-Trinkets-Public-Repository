package net.timothaty.timothatystrinkets.client.debtlord;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public final class DebtlordHoldClientState {
	private static boolean active;

	private DebtlordHoldClientState() {
	}

	public static boolean isActive() {
		return active;
	}

	public static void setActive(boolean active) {
		DebtlordHoldClientState.active = active;
		Minecraft minecraft = Minecraft.getInstance();
		if (active && (minecraft.screen instanceof InventoryScreen || minecraft.screen instanceof CreativeModeInventoryScreen))
			minecraft.setScreen(null);
	}

	public static void clear() {
		active = false;
	}
}
