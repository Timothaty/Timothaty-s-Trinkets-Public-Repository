package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium;

import java.util.Optional;

public enum CorruptedRosariumCombination {
	GORGE(CorruptedRosariumBead.SIN, CorruptedRosariumBead.BLASPHEMY),
	WRATH_OF_THE_WICKED(CorruptedRosariumBead.WRATH, CorruptedRosariumBead.BLASPHEMY),
	HUBRIS(CorruptedRosariumBead.PRIDE, CorruptedRosariumBead.SIN);

	private static final CorruptedRosariumCombination[] CACHED_VALUES = values();

	private final CorruptedRosariumBead first;
	private final CorruptedRosariumBead second;
	private final int beadMask;

	CorruptedRosariumCombination(CorruptedRosariumBead first, CorruptedRosariumBead second) {
		this.first = first;
		this.second = second;
		this.beadMask = first.bit() | second.bit();
	}

	public CorruptedRosariumBead first() {
		return first;
	}

	public CorruptedRosariumBead second() {
		return second;
	}

	public int beadMask() {
		return beadMask;
	}

	public boolean matches(int candidateMask) {
		return Integer.bitCount(candidateMask) == CorruptedRosariumData.SLOT_COUNT
				&& candidateMask == beadMask;
	}

	public static Optional<CorruptedRosariumCombination> fromMask(int beadMask) {
		for (CorruptedRosariumCombination combination : CACHED_VALUES) {
			if (combination.matches(beadMask))
				return Optional.of(combination);
		}
		return Optional.empty();
	}
}
