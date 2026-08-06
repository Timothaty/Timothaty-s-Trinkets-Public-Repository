package net.timothaty.timothatystrinkets.entity.ai;

import net.minecraft.core.BlockPos;

final class NecromancerSummonSpot {
	private final BlockPos groundPos;
	private final double spawnY;

	NecromancerSummonSpot(BlockPos groundPos, double spawnY) {
		this.groundPos = groundPos;
		this.spawnY = spawnY;
	}

	BlockPos groundPos() {
		return groundPos;
	}

	double spawnY() {
		return spawnY;
	}
}
