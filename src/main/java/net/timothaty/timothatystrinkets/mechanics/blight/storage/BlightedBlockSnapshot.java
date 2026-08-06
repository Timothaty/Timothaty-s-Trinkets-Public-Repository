package net.timothaty.timothatystrinkets.mechanics.blight.storage;

import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

public record BlightedBlockSnapshot(BlockState originalState, int originY) {
	public BlightedBlockSnapshot {
		Objects.requireNonNull(originalState, "originalState");
	}
}
