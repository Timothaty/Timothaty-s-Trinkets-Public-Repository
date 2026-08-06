package net.timothaty.timothatystrinkets.entity;

import java.util.Objects;
import java.util.function.Consumer;

public final class SoulOrbTrailStateDispatcher {
	private static final Consumer<SoulOrbEntity> NO_OP = orb -> {
	};
	private static Consumer<SoulOrbEntity> clientListener = NO_OP;

	private SoulOrbTrailStateDispatcher() {
	}

	public static void registerClientListener(Consumer<SoulOrbEntity> listener) {
		clientListener = Objects.requireNonNull(listener);
	}

	public static void notifyChanged(SoulOrbEntity orb) {
		clientListener.accept(orb);
	}
}
