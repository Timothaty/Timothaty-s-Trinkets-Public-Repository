package net.timothaty.timothatystrinkets.mechanics.pagans_charm;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.item.PagansCharmItem;
import net.timothaty.timothatystrinkets.util.TimothatysCuriosHelper;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PaganCharmCharge {
	public static final double BASE_CHARGE_PER_SECOND = PaganCharmTuning.BASE_CHARGE_PER_SECOND;
	public static final double CAMPFIRE_BONUS_PER_SECOND = PaganCharmTuning.CAMPFIRE_BONUS_PER_SECOND;
	public static final double OTHER_MEDITATOR_BONUS_PER_SECOND = PaganCharmTuning.OTHER_MEDITATOR_BONUS_PER_SECOND;
	public static final double FISHING_BONUS_PER_SECOND = PaganCharmTuning.FISHING_BONUS_PER_SECOND;
	public static final double UNIQUE_BIOME_BONUS_PER_SECOND = PaganCharmTuning.UNIQUE_BIOME_BONUS_PER_SECOND;

	private PaganCharmCharge() {
	}

	public static ItemStack findEquippedCharm(Player player) {
		return TimothatysCuriosHelper.findCurio(player, TimothatysTrinketsModItems.PAGANS_CHARM.get());
	}

	public static boolean hasEquippedCharm(Player player) {
		return !findEquippedCharm(player).isEmpty();
	}

	public static boolean hasEquippedCharmForMeditation(Player player) {
		return !findEquippedCharmForMeditation(player).isEmpty();
	}

	public static double getPotentialChargePerSecond(Player player) {
		return getPotentialChargeBreakdown(player).total();
	}

	public static PaganCharmBonuses.ChargeBreakdown getPotentialChargeBreakdown(Player player) {
		if (player == null)
			return PaganCharmBonuses.ChargeBreakdown.EMPTY;
		if (!PaganCharmMeditationRules.canMeditateInCurrentBiome(player) || PaganCharmMeditationRules.isInForbiddenFluid(player))
			return PaganCharmBonuses.ChargeBreakdown.EMPTY;

		return PaganCharmBonuses.getChargeBreakdown(player);
	}

	public static void tickCharge(Player player, PaganCharmMeditationPlayerState state) {
		if (state.timothatys_trinkets$getPaganCharmMeditationActiveTicks(player.tickCount) < PaganCharmMeditationPlayerState.CHARGE_START_TICKS)
			return;

		ItemStack charm = findEquippedCharmForMeditation(player, state);
		if (charm.isEmpty())
			return;

		if (PagansCharmItem.getCharge(charm) >= PagansCharmItem.getMaxCharge(charm)) {
			state.timothatys_trinkets$setPaganCharmChargeRemainder(0.0D);
			return;
		}

		double chargePerSecond = getPotentialChargePerSecond(player);
		if (chargePerSecond <= 0.0D) {
			state.timothatys_trinkets$setPaganCharmChargeRemainder(0.0D);
			return;
		}

		double chargePerTick = chargePerSecond / 20.0D;
		double accumulated = state.timothatys_trinkets$getPaganCharmChargeRemainder() + chargePerTick;
		int wholeCharge = (int) Math.floor(accumulated);
		if (wholeCharge > 0) {
			if (PagansCharmItem.addCharge(charm, wholeCharge)) {
				accumulated -= wholeCharge;
			} else {
				accumulated = 0.0D;
			}
		}

		state.timothatys_trinkets$setPaganCharmChargeRemainder(accumulated);
	}

	private static ItemStack findEquippedCharmForMeditation(Player player) {
		if (player instanceof PaganCharmMeditationPlayerState state)
			return findEquippedCharmForMeditation(player, state);
		return findEquippedCharm(player);
	}

	private static ItemStack findEquippedCharmForMeditation(
			Player player,
			PaganCharmMeditationPlayerState state
	) {
		int cachedAt = state.timothatys_trinkets$getPaganCharmEquipmentCacheTick();
		ItemStack cachedCharm = state.timothatys_trinkets$getPaganCharmCachedEquippedStack();
		if (cachedAt != Integer.MIN_VALUE && player.tickCount >= cachedAt) {
			int cacheAge = player.tickCount - cachedAt;
			if (cacheAge == 0)
				return cachedCharm;
		}

		ItemStack charm = findEquippedCharm(player);
		state.timothatys_trinkets$setPaganCharmEquipmentCache(player.tickCount, charm);
		return charm;
	}
}
