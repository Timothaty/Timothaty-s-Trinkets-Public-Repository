package net.timothaty.timothatystrinkets.mechanics.rosarium;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.api.rosarium.RosariumCombinationApi;
import net.timothaty.timothatystrinkets.api.rosarium.RosariumTypes;

import net.minecraft.resources.ResourceLocation;

public final class RosariumCombinationBootstrap {
	private static boolean initialized;

	private RosariumCombinationBootstrap() {
	}

	public static synchronized void bootstrap() {
		if (initialized)
			return;

		registerHoly("healing_presence", "bead_of_humility", "bead_of_repentance");
		registerHoly("salt_of_the_earth", "bead_of_humility", "bead_of_resurrection");
		registerHoly("confession", "bead_of_humility", "bead_of_the_sacrament");
		registerHoly("angels_shroud", "bead_of_repentance", "bead_of_resurrection");
		registerHoly("holy_light", "bead_of_repentance", "bead_of_the_sacrament");
		registerHoly("cherubims_wisdom", "bead_of_resurrection", "bead_of_the_sacrament");
		registerHoly("beatific_pallium", "bead_of_humility", "bead_of_the_saint");
		registerHoly("exorcism", "bead_of_repentance", "bead_of_the_saint");
		registerHoly("resurrection", "bead_of_resurrection", "bead_of_the_saint");
		registerHoly("smiting_of_the_unholy", "bead_of_the_sacrament", "bead_of_the_saint");

		registerCorrupted("hubris", "bead_of_pride", "bead_of_sin");
		registerCorrupted("killing_ground", "bead_of_pride", "bead_of_wrath");
		registerCorrupted("primary_necromancy", "bead_of_pride", "bead_of_blasphemy");
		registerCorrupted("frenzy", "bead_of_sin", "bead_of_wrath");
		registerCorrupted("gorge", "bead_of_sin", "bead_of_blasphemy");
		registerCorrupted("wrath_of_the_wicked", "bead_of_wrath", "bead_of_blasphemy");

		initialized = true;
	}

	private static void registerHoly(String combinationPath, String firstBeadPath, String secondBeadPath) {
		RosariumCombinationApi.register(
				id(combinationPath),
				RosariumTypes.HOLY,
				id(firstBeadPath),
				id(secondBeadPath)
		);
	}

	private static void registerCorrupted(String combinationPath, String firstBeadPath, String secondBeadPath) {
		RosariumCombinationApi.register(
				id(combinationPath),
				RosariumTypes.CORRUPTED,
				id(firstBeadPath),
				id(secondBeadPath)
		);
	}

	private static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, path);
	}
}
