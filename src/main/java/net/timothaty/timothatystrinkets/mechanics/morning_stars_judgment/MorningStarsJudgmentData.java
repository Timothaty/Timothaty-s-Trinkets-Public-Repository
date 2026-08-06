package net.timothaty.timothatystrinkets.mechanics.morning_stars_judgment;

import net.minecraft.util.Mth;

public final class MorningStarsJudgmentData {
	public static final float MIN_RADIUS = 0.35F;
	public static final float MAX_RADIUS = 5.0F;
	public static final double VERTICAL_SEARCH_RADIUS = 3.0D;
	public static final float DAMAGE_MULTIPLIER = 0.30F;
	public static final int LIFETIME_TICKS = 28;
	public static final double KNOCKBACK_RADIUS = 2.0D;
	public static final double KNOCKBACK_HORIZONTAL_STRENGTH = 0.75D;
	public static final double KNOCKBACK_VERTICAL_IMPULSE = 0.18D;
	public static final int COOLDOWN_TICKS = 6 * 20;
	public static final double VISUAL_Y_OFFSET = 0.01D;

	private MorningStarsJudgmentData() {
	}

	public static float radiusAtAge(int age) {
		float progress = Mth.clamp(
				(float) age / (float) LIFETIME_TICKS,
				0.0F,
				1.0F
		);
		float eased = 1.0F
				- (1.0F - progress) * (1.0F - progress);
		return Mth.lerp(eased, MIN_RADIUS, MAX_RADIUS);
	}
}
