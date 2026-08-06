package net.timothaty.timothatystrinkets.mechanics.bloodstained;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class BloodstainedData {
	public static final int DEFAULT_DURATION_TICKS = 120 * 20;
	public static final int WASH_CHECK_INTERVAL_TICKS = 40;
	public static final int WATER_DURATION_REDUCTION = 200;
	public static final int RAIN_DURATION_REDUCTION = 100;
	public static final float FINAL_FADE_FRACTION = 0.35F;
	public static final TagKey<Item> BLOODSTAINING_WEAPONS = TagKey.create(
		Registries.ITEM,
		ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "bloodstaining_weapons")
	);

	private BloodstainedData() {
	}
}
