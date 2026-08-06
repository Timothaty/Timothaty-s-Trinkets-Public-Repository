package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.salt_of_the_earth;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public final class SaltOfTheEarth {
	public static final double MAX_HEALTH_BONUS = 4.0D;
	public static final float HEALTH_PER_DAMAGE_STEP = 2.0F;
	public static final float HOLY_DAMAGE_BONUS_PER_STEP = 0.03F;
	public static final float MAX_HOLY_DAMAGE_BONUS = 0.35F;

	private SaltOfTheEarth() {
	}

	public static float modifyHolyDamage(Player player, float originalDamage) {
		if (player == null || originalDamage <= 0.0F)
			return originalDamage;

		int healthSteps = Mth.floor(Math.max(0.0F, player.getHealth()) / HEALTH_PER_DAMAGE_STEP);
		float bonus = Math.min(MAX_HOLY_DAMAGE_BONUS, healthSteps * HOLY_DAMAGE_BONUS_PER_STEP);
		return originalDamage * (1.0F + bonus);
	}
}
