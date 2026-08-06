package net.timothaty.timothatystrinkets.mechanics.blight;

public final class BlightConfig {
	private BlightConfig() {
	}

	public static final int AURA_CACHE_TICKS = 15;
	public static final int AURA_HEIGHT = 4;
	public static final double AURA_FOOTPRINT_INSET = 1.0E-4D;

	public static final int SPREAD_MIN_DELAY_TICKS = 800;
	public static final int SPREAD_MAX_DELAY_TICKS = 1200;
	public static final int SPREAD_RETRY_MIN_DELAY_TICKS = 40;
	public static final int SPREAD_RETRY_MAX_DELAY_TICKS = 120;
	public static final int SPREAD_WAKE_DELAY_TICKS = 1;
	public static final float SPREAD_CHANCE = 1.0F;
	public static final int SPREAD_MAX_PER_CHUNK_PER_TICK = 2;
	public static final int SPREAD_MAX_GLOBAL_PER_TICK = 200;
	public static final int SPREAD_MAX_GLOBAL_ATTEMPTS_PER_TICK = 256;
	public static final int SPREAD_MAX_DEPTH_BELOW_ORIGIN = 3;

	public static final int PUTREFACTION_MIN_RADIUS = 3;
	public static final int PUTREFACTION_MAX_RADIUS = 5;
	public static final int PUTREFACTION_BLOCKS_PER_TICK = 4;

	public static final int GROUND_PUTREFACTION_CHECK_INTERVAL_TICKS = 60;
	public static final int GROUND_PUTREFACTION_DURATION_TICKS = 30 * 60 * 20;
	public static final float GROUND_PUTREFACTION_CHANCE = 0.02F;
	public static final int GROUND_AURA_TICK_INTERVAL_TICKS = 15;
	public static final int GROUND_UNDEAD_REGEN_INTERVAL_TICKS = 20;
	public static final float GROUND_UNDEAD_REGEN_PER_SECOND = 1.0F;

	public static final double GROUND_UNDEAD_ARMOR_BONUS = 4.0D;
	public static final double GROUND_UNDEAD_ATTACK_DAMAGE_BONUS = 2.0D;
	public static final double GROUND_UNDEAD_MAX_HEALTH_BONUS = 10.0D;
	public static final double GROUND_UNDEAD_MOVEMENT_SPEED_BONUS = 0.05D;
	public static final double GROUND_LIVING_MOVEMENT_SPEED_PENALTY = -0.10D;

	public static final int SPAWN_BOOST_BLIGHT_RADIUS = 7;
	public static final int SPAWN_BOOST_MAX_CLUSTER_SIZE = 28;
	public static final int SPAWN_BOOST_COUNT_CAP = 200;
	public static final int SPAWN_BOOST_DEBUG_GLOW_TICKS = 120;
	public static final long SPAWN_BOOST_MARKER_TTL_TICKS = 200L;
	public static final long SPAWN_BOOST_CLEANUP_INTERVAL_TICKS = 20L;

	public static final int SUN_SHELTER_HORIZONTAL_SEARCH_RANGE = 12;
	public static final int SUN_SHELTER_VERTICAL_SEARCH_RANGE = 5;
	public static final int SUN_SHELTER_MAX_PATH_CHECKS = 4;
	public static final int SUN_SHELTER_CACHE_TICKS = 40;

}
