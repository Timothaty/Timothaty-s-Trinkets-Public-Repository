package net.timothaty.timothatystrinkets.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class VampiricFangsData {
	private VampiricFangsData() {}

	public static final String FAMES_KEY = "tt_fames_sanguinis";

	public static final double MAX_FAMES = 200.0D;

	public static double getFames(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return 0.0D;
		CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		CompoundTag tag = data.copyTag();
		return clamp(tag.getDouble(FAMES_KEY));
	}

	public static void setFames(ItemStack stack, double value) {
		if (stack == null || stack.isEmpty())
			return;
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		tag.putDouble(FAMES_KEY, clamp(value));
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}

	public static void addFames(ItemStack stack, double amount) {
		if (amount <= 0.0D)
			return;
		setFames(stack, getFames(stack) + amount);
	}

	public static String format(double value) {
		return String.format(java.util.Locale.ROOT, "%.1f", clamp(value));
	}

	private static double clamp(double value) {
		if (Double.isNaN(value) || value < 0.0D)
			return 0.0D;
		return Math.min(value, MAX_FAMES);
	}
}
