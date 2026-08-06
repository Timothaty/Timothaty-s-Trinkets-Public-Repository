package net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility;

public enum HumilityDeedType {
	REPAIR_NATURAL_GOLEM,
	CURE_VILLAGER,
	CREATE_VILLAGE_GOLEM,
	TRADE_WITH_VILLAGER,
	DEFEND_VILLAGE,
	SLAY_RAIDER_OUTSIDE_RAID;

	public int bit() {
		return 1 << ordinal();
	}
}
