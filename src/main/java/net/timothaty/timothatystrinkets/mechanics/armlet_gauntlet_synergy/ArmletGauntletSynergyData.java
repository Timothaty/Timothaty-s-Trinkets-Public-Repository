package net.timothaty.timothatystrinkets.mechanics.armlet_gauntlet_synergy;

import net.minecraft.resources.ResourceLocation;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

public final class ArmletGauntletSynergyData {
	public static final double MAX_RANGE = 6.0D;
	public static final double MAX_RANGE_SQR = MAX_RANGE * MAX_RANGE;
	public static final double ACQUIRE_CONE_DEGREES = 12.0D;
	public static final double RETAIN_CONE_DEGREES = 25.0D;
	public static final double ACQUIRE_MIN_DOT = Math.cos(Math.toRadians(ACQUIRE_CONE_DEGREES));
	public static final double RETAIN_MIN_DOT = Math.cos(Math.toRadians(RETAIN_CONE_DEGREES));

	public static final int MAX_SOUL_EMPOWER_LEVEL = 10;
	public static final int SOUL_EMPOWER_DURATION_TICKS = 20 * 60 * 10;
	public static final double ARMOR_PER_LEVEL = 0.25D;
	public static final double MOVEMENT_SPEED_PER_LEVEL = -0.0025D;
	public static final float MAGIC_DAMAGE_MULTIPLIER_PER_LEVEL = 0.015F;
	public static final int HIGH_COST_MIN_LEVEL = 6;
	public static final float HIGH_LEVEL_EXTRA_HEALTH_COST = 4.0F;

	public static final ResourceLocation ARMOR_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "soul_empower_armor");
	public static final ResourceLocation MOVEMENT_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "soul_empower_movement_speed");

	private ArmletGauntletSynergyData() {
	}
}
