package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.confession;

public final class ConfessionData {
	public static final String HEAL_COOLDOWN_UNTIL_TAG =
			"timothatys_trinkets:confession_heal_cooldown_until";
	public static final int HEAL_COOLDOWN_TICKS = 2_400;
	public static final int FALLBACK_CHECK_INTERVAL_TICKS = 40;
	public static final int RESERVATION_TIMEOUT_TICKS = 200;
	public static final double CLERIC_SEARCH_RADIUS = 6.0D;
	public static final double CLERIC_SEARCH_RADIUS_SQR = CLERIC_SEARCH_RADIUS * CLERIC_SEARCH_RADIUS;
	public static final double MAX_RESERVATION_DISTANCE_SQR = 10.0D * 10.0D;
	public static final double HEAL_DISTANCE_SQR = 3.0D * 3.0D;
	public static final double WALK_SPEED = 0.60D;
	public static final float CANDIDATE_HEALTH_RATIO = 0.50F;
	public static final float EMERGENCY_HEALTH_RATIO = 0.25F;
	public static final float CLERIC_SAFETY_HEALTH_RATIO = 0.25F;
	public static final float HEAL_BASE = 2.0F;
	public static final float HEAL_MAX_HEALTH_RATIO = 0.20F;
	public static final float CLERIC_TRADE_DISCOUNT = 0.25F;
	public static final double TRADING_CLERIC_SEARCH_RADIUS = 8.0D;

	private ConfessionData() {
	}
}
