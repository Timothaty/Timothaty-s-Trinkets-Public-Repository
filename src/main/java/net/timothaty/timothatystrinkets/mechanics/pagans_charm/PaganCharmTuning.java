package net.timothaty.timothatystrinkets.mechanics.pagans_charm;

public final class PaganCharmTuning {
	public static final int MAX_CHARGE = 500;
	public static final int CREATIVE_TAB_CHARGE = 1;
	public static final int BAR_COLOR = 0x008000;
	public static final int EMPTY_CHARGE_TEXT_COLOR = 0x006400;
	public static final int FULL_CHARGE_TEXT_COLOR = 0x32CD32;

	public static final int IDLE_TICKS_TO_START = 10 * 20;
	public static final int MEDITATE_TICKS = 35;
	public static final int LOOP_KEEP_ALIVE_TICKS = 2 * 20;
	public static final int CHARGE_START_TICKS = 10 * 20;
	public static final int BONUS_CACHE_INTERVAL_TICKS = 20;
	public static final int BENIGN_SWING_GRACE_TICKS = 10;

	public static final double BASE_CHARGE_PER_SECOND = 0.5D;
	public static final double CAMPFIRE_BONUS_PER_SECOND = 0.5D;
	public static final double OTHER_MEDITATOR_BONUS_PER_SECOND = 0.5D;
	public static final double FISHING_BONUS_PER_SECOND = 1.0D;
	public static final double UNIQUE_BIOME_BONUS_PER_SECOND = 1.0D;

	public static final int CAMPFIRE_RADIUS = 3;
	public static final double OTHER_MEDITATOR_RADIUS = 6.0D;
	public static final double OTHER_MEDITATOR_RADIUS_SQR = OTHER_MEDITATOR_RADIUS * OTHER_MEDITATOR_RADIUS;
	public static final int FISHING_WATER_VOLUME_RADIUS = 2;
	public static final int FISHING_WATER_VOLUME_MIN_Y_OFFSET = -1;
	public static final int FISHING_WATER_VOLUME_MAX_Y_OFFSET = 0;
	public static final int FISHING_MIN_WATER_BLOCKS = 12;

	public static final int SPIRIT_TRAIL_COUNT = 3;
	public static final int SPIRIT_TRAIL_SEGMENTS = 18;
	public static final double SPIRIT_TRAIL_RENDER_DISTANCE_SQR = 64.0D * 64.0D;
	public static final float SPIRIT_TRAIL_BASE_WIDTH = 0.022F;
	public static final float SPIRIT_TRAIL_BASE_ALPHA = 0.20F;
	public static final int SPIRIT_TRAIL_DEFAULT_RED = 255;
	public static final int SPIRIT_TRAIL_DEFAULT_GREEN = 255;
	public static final int SPIRIT_TRAIL_DEFAULT_BLUE = 255;
	public static final int SPIRIT_TRAIL_NETHER_RED = 92;
	public static final int SPIRIT_TRAIL_NETHER_GREEN = 222;
	public static final int SPIRIT_TRAIL_NETHER_BLUE = 255;

	public static final int FERTILIZER_RADIUS = 2;
	public static final int FERTILIZER_VERTICAL_RADIUS = 1;
	public static final float FERTILIZER_PROC_CHANCE = 0.12F;
	public static final int FERTILIZER_PROC_CHARGE_COST = 10;
	public static final int FERTILIZER_BIOME_ENERGY_PARTICLES = 10;
	public static final int FERTILIZER_RUNE_PARTICLES = 1;

	private PaganCharmTuning() {
	}
}
