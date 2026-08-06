package net.timothaty.timothatystrinkets.api.rosarium;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

public record RosariumCombination(
		ResourceLocation id,
		ResourceLocation rosariumType,
		ResourceLocation firstBeadId,
		ResourceLocation secondBeadId
) {
	public RosariumCombination {
		Objects.requireNonNull(id, "Combination ID cannot be null");
		Objects.requireNonNull(rosariumType, "Rosarium type cannot be null");
		Objects.requireNonNull(firstBeadId, "First bead ID cannot be null");
		Objects.requireNonNull(secondBeadId, "Second bead ID cannot be null");
		if (firstBeadId.equals(secondBeadId))
			throw new IllegalArgumentException("A rosarium combination requires two different bead IDs");
	}

	public String translationKey() {
		return "rosarium_combination."
				+ id.getNamespace()
				+ "."
				+ id.getPath().replace('/', '.');
	}

	public Component displayName() {
		return Component.translatable(translationKey());
	}

	public boolean matches(Collection<ResourceLocation> beadIds) {
		if (beadIds == null || beadIds.size() != 2)
			return false;

		Iterator<ResourceLocation> iterator = beadIds.iterator();
		ResourceLocation firstCandidate = iterator.next();
		ResourceLocation secondCandidate = iterator.next();
		if (firstCandidate == null || secondCandidate == null || firstCandidate.equals(secondCandidate))
			return false;

		return firstBeadId.equals(firstCandidate) && secondBeadId.equals(secondCandidate)
				|| firstBeadId.equals(secondCandidate) && secondBeadId.equals(firstCandidate);
	}
}
