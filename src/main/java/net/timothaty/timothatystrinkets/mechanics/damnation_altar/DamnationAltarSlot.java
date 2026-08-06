package net.timothaty.timothatystrinkets.mechanics.damnation_altar;

import java.util.List;

public enum DamnationAltarSlot {
	NORTH_WEST,
	NORTH_EAST,
	SOUTH_EAST,
	SOUTH_WEST,
	CENTER;

	public static final List<DamnationAltarSlot> OUTER_SLOTS = List.of(NORTH_WEST, NORTH_EAST, SOUTH_EAST, SOUTH_WEST);

	public int index() {
		return ordinal();
	}
}
