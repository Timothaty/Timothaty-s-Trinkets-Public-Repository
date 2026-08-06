package net.timothaty.timothatystrinkets.mechanics.active_ability;

import net.minecraft.world.entity.player.Player;

public final class ActiveAbilityUseGuard {
	private ActiveAbilityUseGuard() {
	}

	public static boolean isBlocked(Player player) {
		return player == null
				|| !player.isAlive()
				|| player.isDeadOrDying()
				|| player.isRemoved()
				|| player.isSpectator()
				|| PlayerActionLockHelper.isActionBlocked(player)
				|| ActiveAbilityCastLock.isLocked(player);
	}
}
