package net.timothaty.timothatystrinkets.mechanics.active_ability;

import net.timothaty.timothatystrinkets.mechanics.undead_knights_armlet.UndeadKnightsArmletEvents;
import net.timothaty.timothatystrinkets.mechanics.champions_gauntlet.ChampionsGauntletEvents;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumActivationResult;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumActiveAbilityRouter;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumActivationResult;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumActiveAbilityRouter;
import net.timothaty.timothatystrinkets.mechanics.vampiric_fangs.VampiricFangsEvents;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsEquipState;
import net.timothaty.timothatystrinkets.util.VampiricFangsCurios;

import net.minecraft.world.entity.player.Player;

public final class TimothatysTrinketsActiveAbilityHandler {
	private TimothatysTrinketsActiveAbilityHandler() {
	}

	public static boolean activate(Player player) {
		if (player == null || player.level().isClientSide() || ActiveAbilityUseGuard.isBlocked(player))
			return false;

		boolean alternate = player.isShiftKeyDown();
		return alternate ? activateAlternate(player) : activatePrimary(player);
	}

	private static boolean activatePrimary(Player player) {
		HolyRosariumActivationResult holyResult = HolyRosariumActiveAbilityRouter.tryActivate(player);
		if (holyResult != HolyRosariumActivationResult.NOT_APPLICABLE)
			return true;

		CorruptedRosariumActivationResult corruptedResult =
				CorruptedRosariumActiveAbilityRouter.tryActivate(player);
		if (corruptedResult != CorruptedRosariumActivationResult.NOT_APPLICABLE) {
			return true;
		}

		if (TimothatysTrinketsEquipState.has(player, TimothatysTrinketsEquipState.CHAMPIONS_GAUNTLET)) {
			ChampionsGauntletEvents.activateSoulAbsorption(player);
			return true;
		}
		if (TimothatysTrinketsEquipState.has(player, TimothatysTrinketsEquipState.UNDEAD_KNIGHTS_ARMLET)) {
			UndeadKnightsArmletEvents.activateSoulHunger(player);
			return true;
		}
		if (VampiricFangsCurios.hasEquippedFangs(player)) {
			VampiricFangsEvents.activateInsatiable(player);
			return true;
		}
		return false;
	}

	private static boolean activateAlternate(Player player) {
		if (VampiricFangsCurios.hasEquippedFangs(player)) {
			VampiricFangsEvents.activateInsatiable(player);
			return true;
		}
		if (TimothatysTrinketsEquipState.has(player, TimothatysTrinketsEquipState.CHAMPIONS_GAUNTLET)) {
			ChampionsGauntletEvents.activateSoulAbsorption(player);
			return true;
		}
		if (TimothatysTrinketsEquipState.has(player, TimothatysTrinketsEquipState.UNDEAD_KNIGHTS_ARMLET)) {
			UndeadKnightsArmletEvents.activateSoulHunger(player);
			return true;
		}
		return false;
	}
}
