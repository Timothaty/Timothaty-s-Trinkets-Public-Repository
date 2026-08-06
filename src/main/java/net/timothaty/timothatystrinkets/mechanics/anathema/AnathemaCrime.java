package net.timothaty.timothatystrinkets.mechanics.anathema;

public enum AnathemaCrime {
	VILLAGER_MURDER(true),
	IRON_GOLEM_MURDER(true),
	THEFT(true),
	WORKSTATION_DESTRUCTION(true),
	EXTORTION(true),
	ARSON(false);

	private final boolean requiresLineOfSight;

	AnathemaCrime(boolean requiresLineOfSight) {
		this.requiresLineOfSight = requiresLineOfSight;
	}

	public boolean requiresLineOfSight() {
		return requiresLineOfSight;
	}
}
