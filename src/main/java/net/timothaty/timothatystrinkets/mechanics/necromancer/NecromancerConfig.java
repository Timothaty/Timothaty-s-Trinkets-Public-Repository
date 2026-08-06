package net.timothaty.timothatystrinkets.mechanics.necromancer;

public final class NecromancerConfig {
	public static final int UNHOLY_AURA_INTERVAL_TICKS = 30;
	public static final int UNHOLY_AURA_EFFECT_TICKS = 90;
	public static final int UNHOLY_AURA_REFRESH_THRESHOLD_TICKS = 45;
	public static final double UNHOLY_AURA_RADIUS = 10.0D;
	public static final double UNHOLY_AURA_RADIUS_SQR = UNHOLY_AURA_RADIUS * UNHOLY_AURA_RADIUS;
	public static final int UNHOLY_AURA_MODIFIER_UPDATE_INTERVAL_TICKS = 20;

	public static final int SUMMONED_MINION_CAP = 8;
	public static final int SUMMONED_MINION_COUNT_CACHE_TICKS = 5;
	public static final double SUMMONED_MINION_SCAN_RANGE = 64.0D;
	public static final int SUMMON_RITUAL_PARTICLE_INTERVAL_TICKS = 4;

	public static final int FRIENDLY_FIRE_TARGET_CLEANUP_INTERVAL_TICKS = 20;

	public static final int VILLAGE_STRUCTURE_SEARCH_RADIUS_CHUNKS = 32;
	public static final int VILLAGE_HORIZONTAL_RADIUS = 96;
	public static final int VILLAGE_VERTICAL_RADIUS = 32;
	public static final int VILLAGE_MAX_APPROACH_VERTICAL_DIFFERENCE = 16;
	public static final int VILLAGE_STRUCTURE_SEARCH_INTERVAL_TICKS = 20 * 25;
	public static final int VILLAGE_STRUCTURE_SEARCH_RANDOM_EXTRA_TICKS = 20 * 5;
	public static final int VILLAGE_POI_REFRESH_INTERVAL_TICKS = 20 * 12;
	public static final int VILLAGE_POI_REFRESH_RANDOM_EXTRA_TICKS = 80;
	public static final int VILLAGE_VILLAGER_SCAN_INTERVAL_TICKS = 20 * 5;
	public static final int VILLAGE_VILLAGER_SCAN_RANDOM_EXTRA_TICKS = 20;
	public static final int VILLAGE_DEPLETED_CONFIRMATION_SCANS = 6;
	public static final int MAX_DEPLETED_VILLAGES_REMEMBERED = 8;
	public static final double SAME_VILLAGE_CENTER_DISTANCE_SQR = 64.0D * 64.0D;
	public static final double NATURAL_SPAWN_EXCLUSION_RADIUS = 32.0D;
	public static final double NATURAL_SPAWN_EXCLUSION_RADIUS_SQR = NATURAL_SPAWN_EXCLUSION_RADIUS * NATURAL_SPAWN_EXCLUSION_RADIUS;

	private NecromancerConfig() {
	}
}
