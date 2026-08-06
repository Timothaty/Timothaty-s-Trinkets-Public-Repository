package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.cherubims_wisdom;

import net.minecraft.util.Mth;

public final class CherubimsWisdomData {
	public static final int DURATION_TICKS = 300;
	public static final int COOLDOWN_TICKS = 500;
	public static final float MELEE_MULTIPLIER = 1.25F;
	public static final float RANGED_MULTIPLIER = 1.25F;
	public static final float HOLY_MULTIPLIER = 1.15F;
	public static final double HOLY_PARTICLE_R = 1.0D;
	public static final double HOLY_PARTICLE_G = 0.84D;
	public static final double HOLY_PARTICLE_B = 0.28D;
	public static final String EMPOWERED_PROJECTILE_TAG =
			"timothatys_trinkets:cherubims_wisdom_empowered";
	public static final String INSUFFICIENT_XP_MESSAGE =
			"message.timothatys_trinkets.cherubims_wisdom.insufficient_experience";

	private CherubimsWisdomData() {
	}

	public static int calculateXpCost(int experienceLevel) {
		return Mth.clamp(40 + Math.max(0, experienceLevel) * 2, 40, 100);
	}
}
