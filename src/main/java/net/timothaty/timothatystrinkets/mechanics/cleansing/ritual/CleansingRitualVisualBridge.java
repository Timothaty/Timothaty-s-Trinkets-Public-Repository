package net.timothaty.timothatystrinkets.mechanics.cleansing.ritual;

import net.timothaty.timothatystrinkets.entity.CleansingRitualControllerEntity;

import java.util.Objects;
import java.util.function.Consumer;

/** Common-side no-op bridge populated only during client setup. */
public final class CleansingRitualVisualBridge {
	private static Consumer<CleansingRitualControllerEntity> clientTicker = controller -> {
	};

	private CleansingRitualVisualBridge() {
	}

	public static void install(Consumer<CleansingRitualControllerEntity> ticker) {
		clientTicker = Objects.requireNonNull(ticker);
	}

	public static void tickClient(CleansingRitualControllerEntity controller) {
		clientTicker.accept(controller);
	}
}
