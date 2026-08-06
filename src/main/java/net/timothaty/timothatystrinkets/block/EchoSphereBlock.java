package net.timothaty.timothatystrinkets.block;

import net.timothaty.timothatystrinkets.block.entity.EchoSphereBlockEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModBlockEntities;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class EchoSphereBlock extends Block implements EntityBlock {
	private static final VoxelShape SHAPE = Block.box(5.0D, 5.0D, 5.0D, 11.0D, 11.0D, 11.0D);

	public EchoSphereBlock() {
		super(BlockBehaviour.Properties.of()
				.sound(SoundType.AMETHYST)
				.strength(0.8F, 3.0F)
				.noOcclusion()
				.lightLevel(state -> 7)
				.isRedstoneConductor((state, getter, pos) -> false));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EchoSphereBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return !level.isClientSide && type == TimothatysTrinketsModBlockEntities.ECHO_SPHERE.get()
				? (tickLevel, tickPos, tickState, blockEntity) -> EchoSphereBlockEntity.serverTick(
						tickLevel,
						tickPos,
						tickState,
						(EchoSphereBlockEntity) blockEntity
				)
				: null;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		takeSphere(level, pos);
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		takeSphere(level, pos);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	private static void takeSphere(Level level, BlockPos pos) {
		if (level.isClientSide) {
			return;
		}
		Block.popResource(level, pos, new ItemStack(TimothatysTrinketsModItems.ECHO_SPHERE.get()));
		level.playSound(
				null,
				pos,
				TimothatysTrinketsModSounds.ECHO_ORB_CLAIM.get(),
				SoundSource.BLOCKS,
				1.0F,
				1.0F + level.getRandom().nextFloat() * 0.3F
		);
		level.removeBlock(pos, false);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}
