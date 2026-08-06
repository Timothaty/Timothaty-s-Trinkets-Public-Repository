package net.timothaty.timothatystrinkets.mechanics.flaming_ember;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class FlamingEmberData {
	public static final double MAX_HEAT = 200.0D;
	public static final double HEAT_PER_DIVISION = 20.0D;
	public static final int DIVISIONS = 10;

	public static final int PASSIVE_TICK_INTERVAL = 20;
	public static final int HEAT_SOURCE_CHECK_INTERVAL_TICKS = 20 * 5;
	public static final int HEAT_SOURCE_RADIUS = 7;
	public static final int MAX_HEAT_SOURCES = 2;

	public static final double HEAT_SOURCE_HEAT_PER_SECOND = 0.25D;
	public static final double HOT_BIOME_HEAT_PER_SECOND = 3.0D;
	public static final double COLD_BIOME_HEAT_PER_SECOND = -2.0D;
	public static final double SUN_HEAT_PER_SECOND = 0.25D;

	public static final double HEAT_MOB_DAMAGE_MIN = 3.0D;
	public static final double HEAT_MOB_DAMAGE_MAX = 6.0D;
	public static final double FIRE_DAMAGE_MIN = 0.0D;
	public static final double FIRE_DAMAGE_MAX = 3.0D;
	public static final double LAVA_DAMAGE_MIN = 4.0D;
	public static final double LAVA_DAMAGE_MAX = 8.0D;

	public static final int OVERHEAT_WINDOW_TICKS = 20 * 2;
	public static final double OVERHEAT_HEAT_THRESHOLD = 15.0D;
	public static final int OVERHEAT_COOLDOWN_TICKS = 20 * 7;

	public static final int IMPULSE_DAMAGE_WINDOW_TICKS = 20 * 3;
	public static final float IMPULSE_DAMAGE_THRESHOLD = 5.0F;
	public static final double IMPULSE_HEAT_COST = 50.0D;
	public static final int IMPULSE_COOLDOWN_TICKS = 20 * 3;
	public static final double IMPULSE_RADIUS = 5.0D;
	public static final double IMPULSE_VERTICAL_RANGE = 2.25D;
	public static final int IMPULSE_SPREAD_TICKS = 14;
	public static final float IMPULSE_MAGIC_DAMAGE = 2.0F;
	public static final int IMPULSE_FIRE_TICKS = 20 * 15;
	public static final double IMPULSE_KNOCKBACK_STRENGTH = 1.05D;
	public static final double IMPULSE_KNOCKBACK_UPWARD = 0.22D;

	public static final String NBT_HEAT = "tt_flaming_ember_heat";
	public static final String NBT_HEAT_SOURCE_CHECK_TICK = "tt_flaming_ember_heat_source_check_tick";
	public static final String NBT_HEAT_SOURCE_COUNT = "tt_flaming_ember_heat_source_count";
	public static final String NBT_DAMAGE_WINDOW_START_TICK = "tt_flaming_ember_damage_window_start_tick";
	public static final String NBT_DAMAGE_WINDOW_HEAT = "tt_flaming_ember_damage_window_heat";
	public static final String NBT_IMPULSE_DAMAGE_WINDOW_START_TICK = "tt_flaming_ember_impulse_damage_window_start_tick";
	public static final String NBT_IMPULSE_DAMAGE_WINDOW_AMOUNT = "tt_flaming_ember_impulse_damage_window_amount";

	private FlamingEmberData() {
	}

	public static double getHeat(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return 0.0D;

		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		return clampHeat(tag.getDouble(NBT_HEAT));
	}

	public static void setHeat(ItemStack stack, double heat) {
		if (stack == null || stack.isEmpty())
			return;

		double clamped = clampHeat(heat);
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		if (clamped <= 0.0D) {
			tag.remove(NBT_HEAT);
		} else {
			tag.putDouble(NBT_HEAT, clamped);
		}

		if (tag.isEmpty()) {
			stack.remove(DataComponents.CUSTOM_DATA);
		} else {
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		}
	}

	public static boolean addHeat(ItemStack stack, double amount) {
		if (stack == null || stack.isEmpty() || amount == 0.0D)
			return false;

		double before = getHeat(stack);
		double after = clampHeat(before + amount);
		if (after == before)
			return false;

		setHeat(stack, after);
		return true;
	}

	public static boolean consumeHeat(ItemStack stack, double amount) {
		if (stack == null || stack.isEmpty())
			return false;
		if (amount <= 0.0D)
			return true;

		double currentHeat = getHeat(stack);
		if (currentHeat < amount)
			return false;

		setHeat(stack, currentHeat - amount);
		return true;
	}

	public static float getHeatProgress(ItemStack stack) {
		return (float) Mth.clamp(getHeat(stack) / MAX_HEAT, 0.0D, 1.0D);
	}

	public static double clampHeat(double heat) {
		if (Double.isNaN(heat))
			return 0.0D;
		return Mth.clamp(heat, 0.0D, MAX_HEAT);
	}
}
