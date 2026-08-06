package net.timothaty.timothatystrinkets.api.rosarium;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Code registration and constant-time lookup for two-bead rosarium combinations.
 * Register combinations during mod initialization.
 *
 * <pre>{@code
 * RosariumCombinationApi.register(
 *     ResourceLocation.fromNamespaceAndPath("example_addon", "radiant_oath"),
 *     RosariumTypes.HOLY,
 *     ResourceLocation.fromNamespaceAndPath("example_addon", "bead_of_light"),
 *     ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "bead_of_humility")
 * );
 * }</pre>
 *
	 * <p>The addon bead must also be added to {@code timothatys_trinkets:holy_beads} for a Holy
	 * Rosarium, or {@code timothatys_trinkets:unholy_bead} for a Corrupted Rosarium.</p>
 */
public final class RosariumCombinationApi {
	private static final Map<ResourceLocation, RosariumCombination> BY_ID = new ConcurrentHashMap<>();
	private static final Map<CombinationKey, RosariumCombination> BY_BEAD_PAIR = new ConcurrentHashMap<>();

	private RosariumCombinationApi() {
	}

	public static synchronized RosariumCombination register(
			ResourceLocation combinationId,
			ResourceLocation rosariumType,
			ResourceLocation firstBeadId,
			ResourceLocation secondBeadId
	) {
		if (combinationId == null || rosariumType == null || firstBeadId == null || secondBeadId == null)
			throw new IllegalArgumentException("Rosarium combination IDs and type cannot be null");
		if (firstBeadId.equals(secondBeadId))
			throw new IllegalArgumentException("Rosarium combination " + combinationId + " uses the same bead twice");
		if (BY_ID.containsKey(combinationId))
			throw new IllegalStateException("Rosarium combination ID is already registered: " + combinationId);

		CombinationKey key = CombinationKey.create(rosariumType, firstBeadId, secondBeadId);
		RosariumCombination existingPair = BY_BEAD_PAIR.get(key);
		if (existingPair != null) {
			throw new IllegalStateException(
					"Rosarium bead pair is already registered for type " + rosariumType + ": " + existingPair.id()
			);
		}

		RosariumCombination combination = new RosariumCombination(
				combinationId,
				rosariumType,
				firstBeadId,
				secondBeadId
		);
		BY_ID.put(combinationId, combination);
		BY_BEAD_PAIR.put(key, combination);
		return combination;
	}

	public static Optional<RosariumCombination> get(ResourceLocation combinationId) {
		return combinationId == null ? Optional.empty() : Optional.ofNullable(BY_ID.get(combinationId));
	}

	public static Optional<RosariumCombination> find(
			ResourceLocation rosariumType,
			Collection<ResourceLocation> beadIds
	) {
		CombinationKey key = CombinationKey.fromCollection(rosariumType, beadIds);
		return key == null ? Optional.empty() : Optional.ofNullable(BY_BEAD_PAIR.get(key));
	}

	public static boolean matches(ResourceLocation combinationId, Collection<ResourceLocation> beadIds) {
		return get(combinationId)
				.map(combination -> combination.matches(beadIds))
				.orElse(false);
	}

	private record CombinationKey(
			ResourceLocation rosariumType,
			ResourceLocation firstBeadId,
			ResourceLocation secondBeadId
	) {
		private static CombinationKey fromCollection(
				ResourceLocation rosariumType,
				Collection<ResourceLocation> beadIds
		) {
			if (rosariumType == null || beadIds == null || beadIds.size() != 2)
				return null;

			var iterator = beadIds.iterator();
			ResourceLocation first = iterator.next();
			ResourceLocation second = iterator.next();
			if (first == null || second == null || first.equals(second))
				return null;
			return create(rosariumType, first, second);
		}

		private static CombinationKey create(
				ResourceLocation rosariumType,
				ResourceLocation firstBeadId,
				ResourceLocation secondBeadId
		) {
			return firstBeadId.toString().compareTo(secondBeadId.toString()) <= 0
					? new CombinationKey(rosariumType, firstBeadId, secondBeadId)
					: new CombinationKey(rosariumType, secondBeadId, firstBeadId);
		}
	}
}
