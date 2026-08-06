package net.timothaty.timothatystrinkets.mechanics.undead_knights_armlet;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsEquipState;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsSunPenaltyHelper;

import net.minecraft.resources.ResourceLocation;

public final class UndeadKnightsArmletData {
	public static final ResourceLocation ARMLET_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "undead_knights_armlet");
	public static final String EQUIP_STATE_KEY = TimothatysTrinketsEquipState.UNDEAD_KNIGHTS_ARMLET;

	public static final ResourceLocation ARMOR_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "undead_knights_armlet_armor");
	public static final ResourceLocation SOUL_HUNGER_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "undead_knights_armlet_soul_hunger_speed");
	public static final ResourceLocation SOUL_HUNGER_ATTACK_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "undead_knights_armlet_soul_hunger_attack_speed");

	public static final double ARMOR_BONUS = 1.0D;
	public static final double SOUL_HUNGER_SPEED_BONUS = 0.02D;
	public static final double SOUL_HUNGER_ATTACK_SPEED_PENALTY = -0.16D;

	public static final float FULL_ATTACK_STRENGTH = 0.9F;
	public static final float BASE_MAGIC_PROC_CHANCE = 0.0666F;
	public static final float SOUL_HUNGER_MAGIC_PROC_CHANCE_BONUS = 0.31F;
	public static final float BASE_MAGIC_DAMAGE_MULTIPLIER = 0.27F;
	public static final float SOUL_HUNGER_MAGIC_DAMAGE_BONUS = 0.11F;

	public static final TimothatysTrinketsSunPenaltyHelper.Settings SUN_PENALTY =
			new TimothatysTrinketsSunPenaltyHelper.Settings(20, 60, 1, 40);

	public static final int SOUL_HUNGER_DURATION_TICKS = 20 * 10;
	public static final int SOUL_HUNGER_COOLDOWN_TICKS = 20 * 40;
	public static final int SOUL_HUNGER_WEAKNESS_TICKS = 20 * 6;
	public static final int SOUL_HUNGER_WEAKNESS_AMPLIFIER = 0;

	public static final float SOUL_HUNGER_FOOD_COST = 4.0F;
	public static final float SOUL_HUNGER_HEALTH_COST = 4.0F;
	public static final float SOUL_HUNGER_MIN_HEALTH_AFTER_COST = 1.0F;
	public static final float SOUL_HUNGER_MOB_KILL_PENDING_HEAL = 1.0F;
	public static final float SOUL_HUNGER_PLAYER_KILL_PENDING_HEAL_MIN = 1.0F;
	public static final float SOUL_HUNGER_PLAYER_KILL_PENDING_HEAL_MAX = 2.0F;

	public static final String NBT_SOUL_HUNGER_ACTIVE = "tt_undead_knights_armlet_soul_hunger_active";
	public static final String NBT_PENDING_HEAL = "tt_undead_knights_armlet_pending_heal";

	private UndeadKnightsArmletData() {
	}
}
