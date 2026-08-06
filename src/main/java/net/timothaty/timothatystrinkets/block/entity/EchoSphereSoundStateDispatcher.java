package net.timothaty.timothatystrinkets.block.entity;

import java.util.Objects;
import java.util.function.Consumer;

public final class EchoSphereSoundStateDispatcher {
	private static final Consumer<EchoSphereBlockEntity> NO_OP = sphere -> {
	};
	private static Consumer<EchoSphereBlockEntity> clientLoadListener = NO_OP;
	private static Consumer<EchoSphereBlockEntity> clientRemovalListener = NO_OP;

	private EchoSphereSoundStateDispatcher() {
	}

	public static void registerClientListeners(Consumer<EchoSphereBlockEntity> loadListener,
			Consumer<EchoSphereBlockEntity> removalListener) {
		clientLoadListener = Objects.requireNonNull(loadListener);
		clientRemovalListener = Objects.requireNonNull(removalListener);
	}

	static void notifyLoaded(EchoSphereBlockEntity sphere) {
		clientLoadListener.accept(sphere);
	}

	static void notifyRemoved(EchoSphereBlockEntity sphere) {
		clientRemovalListener.accept(sphere);
	}
}
