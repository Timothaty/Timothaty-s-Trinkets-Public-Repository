package net.timothaty.timothatystrinkets.mechanics.pillagers_coin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;

public final class PillagersCoinVillagerStockData {
	private static final String STOCK_KEY = "timothatys_trinkets_pillagers_coin_stock_half_units";
	private static final String UPDATE_TIME_KEY = "timothatys_trinkets_pillagers_coin_stock_update_time";

	private PillagersCoinVillagerStockData() {
	}

	public static int getAvailableWholeStock(ServerLevel level, Villager villager) {
		return getStockHalfUnits(level, villager) / PillagersCoinData.STOCK_COST_PER_DROP_HALF_UNITS;
	}

	public static int getStockHalfUnits(ServerLevel level, Villager villager) {
		CompoundTag data = villager.getPersistentData();
		long now = level.getGameTime();
		if (!data.contains(STOCK_KEY, Tag.TAG_INT)) {
			int initialWholeStock = 1 + villager.getRandom().nextInt(
				PillagersCoinData.MAX_STOCK_HALF_UNITS / PillagersCoinData.STOCK_COST_PER_DROP_HALF_UNITS
			);
			int initialHalfUnits = initialWholeStock * PillagersCoinData.STOCK_COST_PER_DROP_HALF_UNITS;
			data.putInt(STOCK_KEY, initialHalfUnits);
			data.putLong(UPDATE_TIME_KEY, now);
			return initialHalfUnits;
		}

		int halfUnits = Math.max(0, Math.min(PillagersCoinData.MAX_STOCK_HALF_UNITS, data.getInt(STOCK_KEY)));
		if (!data.contains(UPDATE_TIME_KEY, Tag.TAG_LONG)) {
			data.putInt(STOCK_KEY, halfUnits);
			data.putLong(UPDATE_TIME_KEY, now);
			return halfUnits;
		}
		long lastUpdate = data.getLong(UPDATE_TIME_KEY);
		if (lastUpdate < 0L || lastUpdate > now) {
			data.putInt(STOCK_KEY, halfUnits);
			data.putLong(UPDATE_TIME_KEY, now);
			return halfUnits;
		}

		long elapsedPeriods = (now - lastUpdate) / PillagersCoinData.STOCK_REGEN_INTERVAL_TICKS;
		if (elapsedPeriods > 0L) {
			int restored = (int) Math.min(elapsedPeriods, PillagersCoinData.MAX_STOCK_HALF_UNITS);
			halfUnits = Math.min(PillagersCoinData.MAX_STOCK_HALF_UNITS, halfUnits + restored);
			lastUpdate += elapsedPeriods * PillagersCoinData.STOCK_REGEN_INTERVAL_TICKS;
		}
		data.putInt(STOCK_KEY, halfUnits);
		data.putLong(UPDATE_TIME_KEY, lastUpdate);
		return halfUnits;
	}

	public static boolean tryConsumeOneStock(ServerLevel level, Villager villager) {
		int halfUnits = getStockHalfUnits(level, villager);
		if (halfUnits < PillagersCoinData.STOCK_COST_PER_DROP_HALF_UNITS)
			return false;
		villager.getPersistentData().putInt(STOCK_KEY, halfUnits - PillagersCoinData.STOCK_COST_PER_DROP_HALF_UNITS);
		return true;
	}
}
