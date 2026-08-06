package net.timothaty.timothatystrinkets.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public final class NecromancerSummonParticles {
	private NecromancerSummonParticles() {
	}

	public static void spawnRitualBlockParticles(ServerLevel serverLevel, NecromancerSummonSpot summonSpot) {
		BlockPos groundPos = summonSpot.groundPos();
		BlockState groundState = serverLevel.getBlockState(groundPos);
		serverLevel.sendParticles(
			new BlockParticleOption(ParticleTypes.BLOCK, groundState),
			groundPos.getX() + 0.5D,
			summonSpot.spawnY() + 0.05D,
			groundPos.getZ() + 0.5D,
			4,
			0.25D,
			0.08D,
			0.25D,
			0.02D
		);
	}
}
