package net.timothaty.timothatystrinkets.mechanics.cleric_quests.sacrament;

public enum SacramentStage {
	NONE,
	OFFERINGS,
	HUNT_ACTIVE,
	HUNT_RETURN,
	FAST_ACTIVE,
	FAST_RETURN,
	RESTART_REQUIRED,
	COMPLETED;

	public static SacramentStage byName(String name) {
		try {
			return valueOf(name);
		} catch (IllegalArgumentException | NullPointerException ignored) {
			return NONE;
		}
	}
}
