package net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet;

public enum DuelistGuardDirection {
	NONE,
	CENTER,
	LEFT,
	RIGHT,
	BACK,
	BROKEN;

	public int networkId() {
		return ordinal();
	}

	public boolean canBeHeldByPlayer() {
		return this == CENTER || this == LEFT || this == RIGHT;
	}

	public boolean isSide() {
		return this == LEFT || this == RIGHT;
	}

	public static DuelistGuardDirection fromNetworkId(int id) {
		DuelistGuardDirection[] values = values();
		if (id < 0 || id >= values.length)
			return NONE;
		return values[id];
	}
}
