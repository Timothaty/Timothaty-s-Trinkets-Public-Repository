package net.timothaty.timothatystrinkets.mechanics.pillagers_coin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.npc.Villager;

import java.util.UUID;

public final class PillagersCoinVillagerFearData {
	private static final String FEAR_ENTRIES_KEY = "timothatys_trinkets_pillagers_coin_fears";
	private static final String PLAYER_KEY = "player";
	private static final String COST_KEY = "forgiveness_cost";

	private PillagersCoinVillagerFearData() {
	}

	public static boolean fears(Villager villager, UUID playerId) {
		return findEntry(villager, playerId) != null;
	}

	public static int getForgivenessCost(Villager villager, UUID playerId) {
		CompoundTag entry = findEntry(villager, playerId);
		return entry == null ? 0 : Math.max(1, Math.min(4, entry.getInt(COST_KEY)));
	}

	public static int addFear(Villager villager, UUID playerId) {
		int existingCost = getForgivenessCost(villager, playerId);
		if (existingCost > 0)
			return existingCost;

		ListTag entries = getEntries(villager);
		CompoundTag entry = new CompoundTag();
		entry.putUUID(PLAYER_KEY, playerId);
		int cost = 1 + villager.getRandom().nextInt(4);
		entry.putInt(COST_KEY, cost);
		entries.add(entry);
		villager.getPersistentData().put(FEAR_ENTRIES_KEY, entries);
		return cost;
	}

	public static boolean forgive(Villager villager, UUID playerId) {
		ListTag entries = getEntries(villager);
		for (int i = 0; i < entries.size(); i++) {
			CompoundTag entry = entries.getCompound(i);
			if (entry.hasUUID(PLAYER_KEY) && entry.getUUID(PLAYER_KEY).equals(playerId)) {
				entries.remove(i);
				villager.getPersistentData().put(FEAR_ENTRIES_KEY, entries);
				return true;
			}
		}
		return false;
	}

	private static CompoundTag findEntry(Villager villager, UUID playerId) {
		if (villager == null || playerId == null)
			return null;
		ListTag entries = getEntries(villager);
		for (int i = 0; i < entries.size(); i++) {
			CompoundTag entry = entries.getCompound(i);
			if (entry.hasUUID(PLAYER_KEY) && entry.getUUID(PLAYER_KEY).equals(playerId))
				return entry;
		}
		return null;
	}

	private static ListTag getEntries(Villager villager) {
		return villager.getPersistentData().getList(FEAR_ENTRIES_KEY, Tag.TAG_COMPOUND);
	}
}
