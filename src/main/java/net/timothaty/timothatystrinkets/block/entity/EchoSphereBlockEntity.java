package net.timothaty.timothatystrinkets.block.entity;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.Vector3f;

public final class EchoSphereBlockEntity extends BlockEntity {
	private static final DustParticleOptions BRIGHT_DUST = new DustParticleOptions(
			new Vector3f(0x29 / 255.0F, 0xDF / 255.0F, 0xEB / 255.0F),
			0.85F
	);
	private static final DustParticleOptions DARK_DUST = new DustParticleOptions(
			new Vector3f(0x1E / 255.0F, 0xB9 / 255.0F, 0xC3 / 255.0F),
			0.75F
	);
	private static final int PARTICLE_INTERVAL_TICKS = 3;

	public EchoSphereBlockEntity(BlockPos pos, BlockState state) {
		super(TimothatysTrinketsModBlockEntities.ECHO_SPHERE.get(), pos, state);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		EchoSphereSoundStateDispatcher.notifyLoaded(this);
	}

	@Override
	public void setRemoved() {
		EchoSphereSoundStateDispatcher.notifyRemoved(this);
		super.setRemoved();
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, EchoSphereBlockEntity sphere) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		if (level.getGameTime() % PARTICLE_INTERVAL_TICKS == 0L) {
			double x = pos.getX() + 0.5D;
			double y = pos.getY() + 0.5D;
			double z = pos.getZ() + 0.5D;
			serverLevel.sendParticles(BRIGHT_DUST, x, y, z, 2, 0.28D, 0.28D, 0.28D, 0.006D);
			serverLevel.sendParticles(DARK_DUST, x, y, z, 2, 0.34D, 0.34D, 0.34D, 0.004D);
		}
	}
}
