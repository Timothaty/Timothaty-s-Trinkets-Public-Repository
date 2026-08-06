package net.timothaty.timothatystrinkets.mechanics.fire;

import net.minecraft.world.entity.player.Player;

public final class FireSphereSweepState {
	private FireSphereSweepState() {
	}

	public static void markNextSweepFiery(Player player) {
		CustomSweepVisualState.markFiery(player);
	}

	public static boolean consumeFierySweep(Player player) {
		return CustomSweepVisualState.consume(player) == CustomSweepVisualState.Visual.FIERY;
	}
}
