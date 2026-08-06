package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.gorge;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

public final class GorgeData {
	public static final int DURATION_TICKS = 30 * 20;
	public static final int DELIVERY_INTERVAL_TICKS = 20;
	public static final int COOLDOWN_TICKS = 50 * 20;
	public static final int PENALTY_DURATION_TICKS = 5 * 20;
	public static final int IMMEDIATE_HUNGER = 4;
	public static final int DIGESTIVE_SURGE_COOLDOWN_TICKS = 40;
	public static final float DIGESTIVE_SURGE_HEALING = 2.0F;
	public static final float MAX_TARGET_HEALTH = 16.0F;
	public static final double MAX_TARGET_REACH = 2.0D;
	public static final double MAX_TARGET_REACH_SQR = MAX_TARGET_REACH * MAX_TARGET_REACH;
	public static final double RAY_TRACE_DISTANCE = 4.0D;

	private GorgeData() {
	}

	public static Restoration calculateRestoration(float targetCurrentHealth) {
		int totalHealing = Math.max(0, Math.round(targetCurrentHealth));
		int totalHunger = Math.max(0, Math.round(targetCurrentHealth * 0.75F));
		return new Restoration(totalHealing, totalHunger);
	}

	public static void addHungerWithoutSaturation(Player player, int amount) {
		if (player == null || amount <= 0)
			return;

		FoodData foodData = player.getFoodData();
		foodData.setFoodLevel(Mth.clamp(foodData.getFoodLevel() + amount, 0, 20));
	}

	public record Restoration(int totalHealing, int totalHunger) {
	}
}
