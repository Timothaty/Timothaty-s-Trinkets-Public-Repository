package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris;

public enum HubrisEndReason {
	EXPIRED(true),
	DEPLETED(true),
	DISPELLED(true),
	DEATH(false),
	LOGOUT(false),
	DIMENSION_CHANGE(false),
	CLONE(false),
	SERVER_STOP(false),
	INVALID(false);

	private final boolean appliesSlowness;

	HubrisEndReason(boolean appliesSlowness) {
		this.appliesSlowness = appliesSlowness;
	}

	public boolean appliesSlowness() {
		return appliesSlowness;
	}
}
