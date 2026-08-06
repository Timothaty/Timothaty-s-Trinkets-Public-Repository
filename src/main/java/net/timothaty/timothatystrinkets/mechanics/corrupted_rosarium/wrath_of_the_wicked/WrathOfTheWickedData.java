package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.wrath_of_the_wicked;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;

public final class WrathOfTheWickedData {
	public static final int DURATION_TICKS = 70;
	public static final int COOLDOWN_TICKS = 80 * 20;
	public static final int DECELERATION_START_TICK = 15;
	public static final int ANCHOR_START_TICK = 20;
	public static final int ANCHOR_END_TICK = 60;
	public static final int PULSE_SERVER_START_TICK = 19;
	public static final int LASER_STAGE_COUNT = 14;
	public static final int LASER_STAGE_INTERVAL_TICKS = 3;
	public static final int WEAKNESS_TICKS = 10 * 20;
	public static final int CONTROL_TICKS = 20;
	public static final int CORROSIVE_TOXICITY_TICKS = 20 * 20;
	public static final int FIRE_TICKS = 5 * 20;
	public static final int FIRE_WAVE_SPREAD_TICKS = 18;
	public static final int PULSE_SLOWNESS_TICKS = 3 * 20;

	public static final double RADIUS = 6.0D;
	public static final double RADIUS_SQR = RADIUS * RADIUS;
	public static final double PULSE_VERTICAL_TOLERANCE = 0.55D;
	public static final float PULSE_VISUAL_START_TICK = 18.4F;
	public static final float PULSE_DURATION_TICKS = 10.0F;
	public static final double FLAMING_EMBER_HEAT_COST = 60.0D;
	public static final double MOVEMENT_SPEED_MULTIPLIER = -0.85D;
	public static final double KNOCKBACK_RESISTANCE_BONUS = 1.0D;
	public static final float REPEAT_LASER_DAMAGE = 0.5F;
	public static final float FIRE_WAVE_DAMAGE = 1.0F;

	private static final double FIRE_WAVE_MIN_RADIUS = 0.35D;
	private static final int[] FIRE_WAVE_TICKS = {20, 35, 50};

	public static final ResourceLocation MOVEMENT_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
			TimothatysTrinketsMod.MODID, "wrath_of_the_wicked_movement_speed"
	);
	public static final ResourceLocation KNOCKBACK_RESISTANCE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
			TimothatysTrinketsMod.MODID, "wrath_of_the_wicked_knockback_resistance"
	);

	public static final TagKey<EntityType<?>> BOSSES = TagKey.create(
			Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath("c", "bosses")
	);

	private WrathOfTheWickedData() {
	}

	public static float rotationPhase(float elapsedTicks) {
		float linear = Mth.clamp(
				(elapsedTicks - ANCHOR_START_TICK) / (float) (ANCHOR_END_TICK - ANCHOR_START_TICK),
				0.0F,
				1.0F
		);
		return smoothstep(linear);
	}

	public static float smoothstep(float value) {
		float clamped = Mth.clamp(value, 0.0F, 1.0F);
		return clamped * clamped * (3.0F - 2.0F * clamped);
	}

	public static float pulseProgress(float elapsedTicks) {
		return Mth.clamp(
				(elapsedTicks - PULSE_VISUAL_START_TICK) / PULSE_DURATION_TICKS,
				0.0F,
				1.0F
		);
	}

	public static double pulseRadius(float elapsedTicks) {
		float progress = pulseProgress(elapsedTicks);
		float inverse = 1.0F - progress;
		float eased = 1.0F - inverse * inverse * inverse;
		return RADIUS * eased;
	}

	public static float laserSweepPhase(int stage) {
		return rotationPhase(
				ANCHOR_START_TICK + stage * LASER_STAGE_INTERVAL_TICKS
		);
	}

	public static int fireWaveCount() {
		return FIRE_WAVE_TICKS.length;
	}

	public static int fireWaveTick(int index) {
		return FIRE_WAVE_TICKS[index];
	}

	public static double fireWaveRadius(float ageTicks) {
		float progress = Mth.clamp(
				ageTicks / FIRE_WAVE_SPREAD_TICKS,
				0.0F,
				1.0F
		);
		float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
		return Mth.lerp(eased, FIRE_WAVE_MIN_RADIUS, RADIUS);
	}
}
