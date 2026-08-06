package net.timothaty.timothatystrinkets.block;

import net.minecraft.world.level.material.MapColor;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.mechanics.blight.BlightConfig;
import net.timothaty.timothatystrinkets.mechanics.blight.BlightSpreadHelper;
import net.timothaty.timothatystrinkets.mechanics.blight.storage.BlightSavedData;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BlockofBlightBlock extends Block {
	public BlockofBlightBlock() {
		super(BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_BLACK)
				.sound(SoundType.SOUL_SOIL)
				.strength(1.0f, 1.0f));
	}

	private static int nextSpreadDelay(RandomSource random) {
		return BlightConfig.SPREAD_MIN_DELAY_TICKS + random.nextInt(BlightConfig.SPREAD_MAX_DELAY_TICKS - BlightConfig.SPREAD_MIN_DELAY_TICKS + 1);
	}

	private static int nextRetryDelay(RandomSource random) {
		return BlightConfig.SPREAD_RETRY_MIN_DELAY_TICKS + random.nextInt(BlightConfig.SPREAD_RETRY_MAX_DELAY_TICKS - BlightConfig.SPREAD_RETRY_MIN_DELAY_TICKS + 1);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (level instanceof ServerLevel serverLevel) {
			if (!oldState.is(this) && !oldState.hasBlockEntity()) {
				BlightSavedData.get(serverLevel).remember(pos, oldState, pos.getY());
			}
			if (!BlightSpreadHelper.isCompletelySurroundedByBlight(serverLevel, pos)) {
				serverLevel.scheduleTick(pos, this, nextSpreadDelay(level.getRandom()));
			}
		}
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (level instanceof ServerLevel serverLevel && state.getBlock() != newState.getBlock()) {
			BlightSavedData.get(serverLevel).remove(pos);
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
		super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
		if (level.hasChunkAt(neighborPos) && level.getBlockState(neighborPos).is(this)) {
			return;
		}
		if (!level.isClientSide() && level instanceof ServerLevel serverLevel && BlightSpreadHelper.hasSpreadCandidate(serverLevel, pos)) {
			level.scheduleTick(pos, this, BlightConfig.SPREAD_WAKE_DELAY_TICKS);
		}
	}

	@Override
	public boolean addRunningEffects(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (level.isClientSide()) {
			RandomSource random = level.getRandom();
			Vec3 movement = entity.getDeltaMovement();
			double spread = Math.max(0.18D, entity.getBbWidth() * 0.45D);
			double x = entity.getX() + (random.nextDouble() - 0.5D) * spread;
			double z = entity.getZ() + (random.nextDouble() - 0.5D) * spread;
			double y = entity.getY() + 0.08D;
			level.addParticle(
					TimothatysTrinketsModParticleTypes.BLIGHTED_DUST.get(),
					x,
					y,
					z,
					movement.x * -0.45D,
					0.025D + random.nextDouble() * 0.025D,
					movement.z * -0.45D);
		}
		return true;
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		super.tick(state, level, pos, random);

		BlightSpreadHelper.SpreadTickResult result = BlightSpreadHelper.tickSpreadFrom(level, pos);
		if (result == BlightSpreadHelper.SpreadTickResult.DORMANT) {
			return;
		}

		level.scheduleTick(pos, this, result == BlightSpreadHelper.SpreadTickResult.RETRY ? nextRetryDelay(random) : nextSpreadDelay(random));
	}
}
