package net.timothaty.timothatystrinkets.mechanics.active_ability;

import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris.HubrisActivationState;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.wrath_of_the_wicked.WrathOfTheWickedState;
import net.timothaty.timothatystrinkets.mechanics.debtlord.DebtlordTelekineticHold;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

import net.minecraft.world.entity.player.Player;

public final class PlayerActionLockHelper {
	private PlayerActionLockHelper() {
	}

	public static boolean isActionBlocked(Player player) {
		return player != null && (TimothatysTrinketsStunHelper.isStunned(player)
				|| HubrisActivationState.isCasting(player)
				|| WrathOfTheWickedState.isActive(player)
				|| DebtlordTelekineticHold.isHeld(player));
	}
}
