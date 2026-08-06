package net.timothaty.timothatystrinkets.mechanics.champions_gauntlet;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsEquipState;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsSunPenaltyHelper;

import net.minecraft.resources.ResourceLocation;

public final class ChampionsGauntletData {
	public static final ResourceLocation GAUNTLET_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "champions_gauntlet");
	public static final String EQUIP_STATE_KEY = TimothatysTrinketsEquipState.CHAMPIONS_GAUNTLET;

	public static final ResourceLocation ARMOR_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "champions_gauntlet_armor");
	public static final ResourceLocation MAX_HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "champions_gauntlet_max_health");
	public static final ResourceLocation SOUL_ABSORPTION_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "champions_gauntlet_soul_absorption_speed");

	public static final double ARMOR_BONUS = 1.0D;
	public static final double MAX_HEALTH_BONUS = 2.0D;
	public static final double NIGHT_MAX_HEALTH_BONUS = 1.0D;
	public static final double SOUL_ABSORPTION_SPEED_BONUS = 0.07D;

	public static final float FULL_ATTACK_STRENGTH = 0.9F;
	public static final float BASE_MAGIC_PROC_CHANCE = 0.24F;
	public static final float SOUL_ABSORPTION_MAGIC_PROC_CHANCE_BONUS = 0.26F;
	public static final float BASE_MAGIC_DAMAGE_MULTIPLIER = 0.44F;
	public static final float SOUL_ABSORPTION_MAGIC_DAMAGE_BONUS = 0.18F;

	public static final TimothatysTrinketsSunPenaltyHelper.Settings SUN_PENALTY =
			new TimothatysTrinketsSunPenaltyHelper.Settings(20, 60, 1, 40);

	public static final int SOUL_ABSORPTION_DURATION_TICKS = 20 * 10;
	public static final int SOUL_ABSORPTION_COOLDOWN_TICKS = 20 * 40;
	public static final int SOUL_ABSORPTION_WEAKNESS_TICKS = 20 * 6;
	public static final int SOUL_ABSORPTION_WEAKNESS_AMPLIFIER = 0;

	public static final float SOUL_ABSORPTION_FOOD_COST = 4.0F;
	public static final float SOUL_ABSORPTION_HEALTH_COST = 4.0F;
	public static final float SOUL_ABSORPTION_NO_KILL_HEALTH_COST = 1.0F;
	public static final float SOUL_ABSORPTION_MIN_HEALTH_AFTER_COST = 1.0F;
	public static final float SOUL_ABSORPTION_MOB_KILL_PENDING_HEAL = 1.0F;
	public static final float SOUL_ABSORPTION_PLAYER_KILL_PENDING_HEAL = 2.0F;
	public static final double SOUL_ABSORPTION_FAMES_MULTIPLIER = 5.0D;

	public static final double VOID_MARKED_DEATH_STUN_RADIUS = 2.0D;
	public static final int VOID_MARKED_DEATH_STUN_TICKS = 20;

	public static final String NBT_SOUL_ABSORPTION_ACTIVE = "tt_champions_gauntlet_soul_absorption_active";
	public static final String NBT_PENDING_HEAL = "tt_champions_gauntlet_pending_heal";
	public static final String NBT_KILL_COUNT = "tt_champions_gauntlet_kill_count";

	private ChampionsGauntletData() {
	}
}
