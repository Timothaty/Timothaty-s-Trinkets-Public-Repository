package net.timothaty.timothatystrinkets.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class TimothatysTrinketsEquipState {
	public static final String DRUMS_OF_HASTE = "ttr_equipped_drums_of_haste";
	public static final String CHAMPIONS_GAUNTLET = "ttr_equipped_champions_gauntlet";
	public static final String UNDEAD_KNIGHTS_ARMLET = "ttr_equipped_undead_knights_armlet";
	public static final String VAMPIRIC_FANGS = "ttr_equipped_vampiric_fangs";
	public static final String ECHO_SPHERE = "ttr_equipped_echo_sphere";
	public static final String BELT_OF_OUTCAST = "ttr_equipped_belt_of_outcast";
	public static final String STRIKER_OF_THE_MORNING_STAR = "ttr_equipped_striker_of_the_morning_star";

	private TimothatysTrinketsEquipState() {
	}

	public static boolean has(Player player, String key) {
		return player != null && player.getPersistentData().getBoolean(key);
	}

	public static void set(Player player, String key, boolean equipped) {
		if (player == null || key == null || key.isEmpty())
			return;

		if (equipped) {
			player.getPersistentData().putBoolean(key, true);
		} else {
			player.getPersistentData().remove(key);
		}
	}

	public static boolean syncFromCurios(Player player, String key, ResourceLocation itemId) {
		if (player == null)
			return false;

		boolean actual = TimothatysCuriosHelper.hasCurio(player, itemId);
		boolean cached = has(player, key);
		if (actual != cached) {
			set(player, key, actual);
			return true;
		}
		return false;
	}
}
