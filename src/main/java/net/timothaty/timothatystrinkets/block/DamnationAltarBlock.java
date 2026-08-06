package net.timothaty.timothatystrinkets.block;

import net.minecraft.world.level.material.MapColor;
import net.timothaty.timothatystrinkets.block.entity.DamnationAltarBlockEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModBlockEntities;
import net.timothaty.timothatystrinkets.mechanics.damnation_altar.DamnationAltarInteractionHandler;
import net.timothaty.timothatystrinkets.util.DamnationAltarOfferDisplayHandler;

import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import java.util.function.BiConsumer;

public class DamnationAltarBlock extends Block implements EntityBlock {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	private static final int CANDLE_LIGHT_LEVEL = 6;

	public DamnationAltarBlock() {
		super(BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_PURPLE)
				.sound(SoundType.DEEPSLATE_BRICKS)
				.strength(1f, 10f)
				.requiresCorrectToolForDrops()
				.noOcclusion()
				.lightLevel(state -> state.getValue(LIT) ? CANDLE_LIGHT_LEVEL : 0)
				.isRedstoneConductor((bs, br, bp) -> false));
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
		return true;
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 0;
	}

	@Override
	public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			default -> box(0, 0, 0, 16, 16, 16);
			case NORTH -> box(0, 0, 0, 16, 16, 16);
			case EAST -> box(0, 0, 0, 16, 16, 16);
			case WEST -> box(0, 0, 0, 16, 16, 16);
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING, LIT);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return super.getStateForPlacement(context).setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DamnationAltarBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return type == TimothatysTrinketsModBlockEntities.DAMNATION_ALTAR.get()
				? (tickLevel, tickPos, tickState, blockEntity) -> {
					DamnationAltarBlockEntity altar = (DamnationAltarBlockEntity) blockEntity;
					if (tickLevel.isClientSide) DamnationAltarBlockEntity.clientTick(tickLevel, tickPos, tickState, altar);
					else DamnationAltarBlockEntity.serverTick(tickLevel, tickPos, tickState, altar);
				}
				: null;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		return DamnationAltarInteractionHandler.handleItemInteraction(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return DamnationAltarInteractionHandler.handleEmptyHandInteraction(state, level, pos, player, hit);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof DamnationAltarBlockEntity altar) {
			altar.cancelForDestruction();
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
		if (blockEntity instanceof DamnationAltarBlockEntity altar && altar.shouldSuppressDestructionDrops()) return;
		super.playerDestroy(level, player, pos, state, blockEntity, tool);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
			ServerLevel serverLevel = (ServerLevel) level;
			if (level.getBlockEntity(pos) instanceof DamnationAltarBlockEntity altar) {
				DamnationAltarOfferDisplayHandler.migrateLegacyDisplaysAt(serverLevel, pos);
				if (altar.isTransmuting()) altar.cancelForDestruction();
				else altar.dropStoredItems();
			} else {
				ItemStack recovered = DamnationAltarOfferDisplayHandler.recoverLegacyOfferWithoutBlockEntity(serverLevel, pos);
				if (!recovered.isEmpty()) {
					Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, recovered);
				}
			}
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Override
	protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> dropConsumer) {
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof DamnationAltarBlockEntity altar && altar.isTransmuting()) {
			altar.cancelForDestruction();
			level.removeBlock(pos, false);
			return;
		}
		super.onExplosionHit(state, level, pos, explosion, dropConsumer);
	}

	public BlockState rotate(BlockState state, Rotation rot) {
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
	}
}
