package net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility;

public enum HumilityStage {
	NONE,
	DEEDS_ACTIVE,
	REWARD_READY,
	COMPLETED;

	public static HumilityStage byName(String name) {
		try {
			return valueOf(name);
		} catch (IllegalArgumentException | NullPointerException ignored) {
			return NONE;
		}
	}
}
