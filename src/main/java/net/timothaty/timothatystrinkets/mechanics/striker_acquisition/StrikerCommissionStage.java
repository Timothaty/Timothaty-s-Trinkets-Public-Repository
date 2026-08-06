package net.timothaty.timothatystrinkets.mechanics.striker_acquisition;

public enum StrikerCommissionStage {
	NONE,
	WAITING_FOR_RAID_VICTORY,
	FORGING_PENDING,
	WALKING_TO_WORKSTATION,
	FORGING,
	DELIVERY_PENDING,
	DELIVERING;

	public boolean isActive() {
		return this != NONE;
	}

	public static StrikerCommissionStage fromStoredName(String name) {
		if (name == null || name.isBlank())
			return NONE;
		try {
			return valueOf(name);
		} catch (IllegalArgumentException ignored) {
			return NONE;
		}
	}
}
