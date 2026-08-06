package net.timothaty.timothatystrinkets.util;

import net.minecraft.world.entity.player.Player;

@Deprecated(forRemoval = false)
public final class FireSphereSweepState {
	private FireSphereSweepState() {
	}

	public static void markNextSweepFiery(Player player) {
		net.timothaty.timothatystrinkets.mechanics.fire.FireSphereSweepState.markNextSweepFiery(player);
	}

	public static boolean consumeFierySweep(Player player) {
		return net.timothaty.timothatystrinkets.mechanics.fire.FireSphereSweepState.consumeFierySweep(player);
	}
}
