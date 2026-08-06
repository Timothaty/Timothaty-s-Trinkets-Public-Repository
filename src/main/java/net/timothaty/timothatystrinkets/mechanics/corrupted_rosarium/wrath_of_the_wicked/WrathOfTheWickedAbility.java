package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.wrath_of_the_wicked;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumCombination;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumData;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumHelper;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumState;
import net.timothaty.timothatystrinkets.mechanics.flaming_ember.FlamingEmberData;
import net.timothaty.timothatystrinkets.util.TimothatysCuriosHelper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.SlotResult;

public final class WrathOfTheWickedAbility {
	private WrathOfTheWickedAbility() {
	}

	public static boolean tryActivate(ServerPlayer player) {
		SlotResult activeRosarium;
		if (player == null
				|| !player.isAlive()
				|| player.isDeadOrDying()
				|| player.isRemoved()
				|| player.isSpectator()
				|| WrathOfTheWickedState.isActive(player)
				|| player.getCooldowns().isOnCooldown(TimothatysTrinketsModItems.CORRUPTED_ROSARY.get())) {
			return false;
		}
		activeRosarium = CorruptedRosariumHelper.findActiveRosariumResult(player).orElse(null);
		if (activeRosarium == null
				|| !CorruptedRosariumCombination.WRATH_OF_THE_WICKED.matches(
						CorruptedRosariumData.getKnownBeadMask(activeRosarium.stack())
				)) {
			return false;
		}

		ItemStack ember = TimothatysCuriosHelper.findCurio(
				player,
				TimothatysTrinketsModItems.FLAMING_EMBER.get()
		);
		boolean hasFlamingEmberHeat = FlamingEmberData.getHeat(ember)
				>= WrathOfTheWickedData.FLAMING_EMBER_HEAT_COST;
		boolean venomSphereSynergy = TimothatysCuriosHelper.isActiveExclusiveSphere(
				player,
				TimothatysTrinketsModItems.VENOM_SPHERE.get()
		);

		if (!WrathOfTheWickedState.start(
				player,
				CorruptedRosariumState.getRevision(player),
				activeRosarium.slotContext(),
				venomSphereSynergy
		)) {
			return false;
		}

		if (hasFlamingEmberHeat
				&& FlamingEmberData.consumeHeat(
						ember,
						WrathOfTheWickedData.FLAMING_EMBER_HEAT_COST
				)) {
			WrathOfTheWickedState.enableFlamingEmberSynergy(player);
		}

		player.getCooldowns().addCooldown(
				TimothatysTrinketsModItems.CORRUPTED_ROSARY.get(),
				WrathOfTheWickedData.COOLDOWN_TICKS
		);
		return true;
	}
}
