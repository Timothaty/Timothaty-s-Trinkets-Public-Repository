package net.timothaty.timothatystrinkets.mechanics.pillagers_coin;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;

public final class PillagersCoinData {
	public static final int EXTORTION_DURATION_TICKS = 100;
	public static final double EXTORTION_MAX_DISTANCE = 5.0D;
	public static final double EXTORTION_MAX_DISTANCE_SQR = 25.0D;
	public static final int OUT_OF_RANGE_GRACE_TICKS = 10;
	public static final int DROP_INTERVAL_TICKS = 5;
	public static final int SWEAT_INTERVAL_TICKS = 10;
	public static final int RETURN_TO_SLEEP_SETTLE_TICKS = 20;
	public static final int MAX_STOCK_HALF_UNITS = 30;
	public static final int STOCK_COST_PER_DROP_HALF_UNITS = 2;
	public static final long STOCK_REGEN_INTERVAL_TICKS = 24_000L;
	public static final int UNWITNESSED_COOLDOWN_TICKS = 30 * 20;
	public static final int WITNESSED_COOLDOWN_TICKS = 60 * 20;
	public static final double UNWITNESSED_BASE_CHANCE = 0.60D;
	public static final double WITNESSED_BASE_CHANCE = 0.05D;
	public static final double BLOODSTAINED_BONUS = 0.15D;
	public static final double WEAPON_BONUS = 0.10D;
	public static final double BLOODSTAINED_WEAPON_BONUS = 0.15D;
	public static final double MIN_SUCCESS_CHANCE = 0.05D;
	public static final double MAX_SUCCESS_CHANCE = 0.85D;
	public static final double RAID_DROP_CHANCE = 0.05D;
	public static final ResourceKey<LootTable> EXTORTION_LOOT_TABLE = ResourceKey.create(
		Registries.LOOT_TABLE,
		ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "gameplay/pillagers_coin_extortion")
	);
	public static final ResourceKey<LootTable> CLERIC_EXTORTION_LOOT_TABLE = ResourceKey.create(
		Registries.LOOT_TABLE,
		ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "gameplay/pillagers_coin_cleric_extortion")
	);

	private PillagersCoinData() {
	}
}
