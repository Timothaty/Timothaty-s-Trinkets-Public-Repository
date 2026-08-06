package net.timothaty.timothatystrinkets.block;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;

import net.timothaty.timothatystrinkets.compat.supplementaries.SupplementariesCompat;
import net.timothaty.timothatystrinkets.mechanics.cleansing.ritual.CleansingRitualService;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** A visual trail with redstone-like geometry and no redstone semantics. */
public final class IncenseTrailBlock extends Block {
	public static final MapCodec<IncenseTrailBlock> CODEC = simpleCodec(IncenseTrailBlock::new);
	public static final EnumProperty<RedstoneSide> NORTH = BlockStateProperties.NORTH_REDSTONE;
	public static final EnumProperty<RedstoneSide> EAST = BlockStateProperties.EAST_REDSTONE;
	public static final EnumProperty<RedstoneSide> SOUTH = BlockStateProperties.SOUTH_REDSTONE;
	public static final EnumProperty<RedstoneSide> WEST = BlockStateProperties.WEST_REDSTONE;
	public static final BooleanProperty ASH = BooleanProperty.create("ash");

	public static final Map<Direction, EnumProperty<RedstoneSide>> PROPERTY_BY_DIRECTION = ImmutableMap.of(
			Direction.NORTH, NORTH,
			Direction.EAST, EAST,
			Direction.SOUTH, SOUTH,
			Direction.WEST, WEST
	);

	private static final VoxelShape SHAPE_DOT = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 1.0D, 13.0D);
	private static final Map<Direction, VoxelShape> FLOOR_SHAPES = createFloorShapes();
	private static final Map<Direction, VoxelShape> UP_SHAPES = createUpShapes();

	private final BlockState crossState;
	private final Map<BlockState, VoxelShape> shapes;

	public IncenseTrailBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(NORTH, RedstoneSide.NONE)
				.setValue(EAST, RedstoneSide.NONE)
				.setValue(SOUTH, RedstoneSide.NONE)
				.setValue(WEST, RedstoneSide.NONE)
				.setValue(ASH, false));
		this.crossState = this.defaultBlockState()
				.setValue(NORTH, RedstoneSide.SIDE)
				.setValue(EAST, RedstoneSide.SIDE)
				.setValue(SOUTH, RedstoneSide.SIDE)
				.setValue(WEST, RedstoneSide.SIDE);

		ImmutableMap.Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();
		for (BlockState state : this.getStateDefinition().getPossibleStates()) {
			builder.put(state, calculateShape(state));
		}
		this.shapes = builder.build();
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return this.shapes.get(state);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return getConnectionState(context.getLevel(), this.crossState, context.getClickedPos());
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction changedDirection, BlockState neighborState,
			LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (changedDirection == Direction.DOWN) {
			return canSurviveOn(level, neighborPos, neighborState) ? state : Blocks.AIR.defaultBlockState();
		}
		if (changedDirection == Direction.UP) {
			return getConnectionState(level, state, pos);
		}

		RedstoneSide side = getConnectingSide(level, pos, changedDirection);
		EnumProperty<RedstoneSide> property = PROPERTY_BY_DIRECTION.get(changedDirection);
		if (side.isConnected() == state.getValue(property).isConnected() && !isCross(state)) {
			return state.setValue(property, side);
		}
		BlockState base = this.crossState.setValue(ASH, state.getValue(ASH)).setValue(property, side);
		return getConnectionState(level, base, pos);
	}

	@Override
	protected void updateIndirectNeighbourShapes(BlockState state, LevelAccessor level, BlockPos pos, int flags, int recursionLeft) {
		BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			if (state.getValue(PROPERTY_BY_DIRECTION.get(direction)) == RedstoneSide.NONE) {
				continue;
			}
			mutable.setWithOffset(pos, direction);
			if (level.getBlockState(mutable).is(this)) {
				continue;
			}

			mutable.move(Direction.DOWN);
			if (level.getBlockState(mutable).is(this)) {
				BlockPos source = mutable.relative(direction.getOpposite());
				level.neighborShapeChanged(direction.getOpposite(), level.getBlockState(source), mutable, source, flags, recursionLeft);
			}

			mutable.setWithOffset(pos, direction).move(Direction.UP);
			if (level.getBlockState(mutable).is(this)) {
				BlockPos source = mutable.relative(direction.getOpposite());
				level.neighborShapeChanged(direction.getOpposite(), level.getBlockState(source), mutable, source, flags, recursionLeft);
			}
		}
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
		if (!level.isClientSide && !state.canSurvive(level, pos)) {
			dropResources(state, level, pos);
			level.removeBlock(pos, false);
		}
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!level.isClientSide && !oldState.is(this)) refreshNearbyTrailShapes(level, pos);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		super.onRemove(state, level, pos, newState, movedByPiston);
		if (!level.isClientSide && !newState.is(this)) refreshNearbyTrailShapes(level, pos);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockPos below = pos.below();
		return canSurviveOn(level, below, level.getBlockState(below));
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return false;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (state.getValue(ASH)) {
			return ItemInteractionResult.FAIL;
		}
		if (level.isClientSide) {
			return ItemInteractionResult.SUCCESS;
		}
		return CleansingRitualService.tryStart(level, pos, player, hand, stack)
				? ItemInteractionResult.CONSUME
				: ItemInteractionResult.FAIL;
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder lootParams) {
		if (state.getValue(ASH)) {
			ItemStack ash = SupplementariesCompat.createAshDrop();
			return ash.isEmpty() ? List.of() : List.of(ash);
		}
		return super.getDrops(state, lootParams);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return switch (rotation) {
			case CLOCKWISE_180 -> state.setValue(NORTH, state.getValue(SOUTH))
					.setValue(EAST, state.getValue(WEST))
					.setValue(SOUTH, state.getValue(NORTH))
					.setValue(WEST, state.getValue(EAST));
			case COUNTERCLOCKWISE_90 -> state.setValue(NORTH, state.getValue(EAST))
					.setValue(EAST, state.getValue(SOUTH))
					.setValue(SOUTH, state.getValue(WEST))
					.setValue(WEST, state.getValue(NORTH));
			case CLOCKWISE_90 -> state.setValue(NORTH, state.getValue(WEST))
					.setValue(EAST, state.getValue(NORTH))
					.setValue(SOUTH, state.getValue(EAST))
					.setValue(WEST, state.getValue(SOUTH));
			default -> state;
		};
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return switch (mirror) {
			case LEFT_RIGHT -> state.setValue(NORTH, state.getValue(SOUTH)).setValue(SOUTH, state.getValue(NORTH));
			case FRONT_BACK -> state.setValue(EAST, state.getValue(WEST)).setValue(WEST, state.getValue(EAST));
			default -> super.mirror(state, mirror);
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, ASH);
	}

	private BlockState getConnectionState(BlockGetter level, BlockState source, BlockPos pos) {
		boolean wasDot = isDot(source);
		BlockState state = this.defaultBlockState().setValue(ASH, source.getValue(ASH));
		boolean openAbove = !level.getBlockState(pos.above()).isRedstoneConductor(level, pos.above());
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), getConnectingSide(level, pos, direction, openAbove));
		}
		if (wasDot && isDot(state)) {
			return state;
		}

		boolean north = state.getValue(NORTH).isConnected();
		boolean south = state.getValue(SOUTH).isConnected();
		boolean east = state.getValue(EAST).isConnected();
		boolean west = state.getValue(WEST).isConnected();
		boolean noNorthSouth = !north && !south;
		boolean noEastWest = !east && !west;
		if (!west && noNorthSouth) state = state.setValue(WEST, RedstoneSide.SIDE);
		if (!east && noNorthSouth) state = state.setValue(EAST, RedstoneSide.SIDE);
		if (!north && noEastWest) state = state.setValue(NORTH, RedstoneSide.SIDE);
		if (!south && noEastWest) state = state.setValue(SOUTH, RedstoneSide.SIDE);
		return state;
	}

	private RedstoneSide getConnectingSide(BlockGetter level, BlockPos pos, Direction direction) {
		BlockPos above = pos.above();
		return getConnectingSide(level, pos, direction, !level.getBlockState(above).isRedstoneConductor(level, above));
	}

	private RedstoneSide getConnectingSide(BlockGetter level, BlockPos pos, Direction direction, boolean openAbove) {
		BlockPos neighborPos = pos.relative(direction);
		BlockState neighbor = level.getBlockState(neighborPos);
		if (openAbove && canSurviveOn(level, neighborPos, neighbor)) {
			BlockPos upperPos = neighborPos.above();
			if (level.getBlockState(upperPos).is(this)) {
				return neighbor.isFaceSturdy(level, neighborPos, direction.getOpposite()) ? RedstoneSide.UP : RedstoneSide.SIDE;
			}
		}
		if (neighbor.is(this)) {
			return RedstoneSide.SIDE;
		}
		if (neighbor.isRedstoneConductor(level, neighborPos)) {
			return RedstoneSide.NONE;
		}
		return level.getBlockState(neighborPos.below()).is(this) ? RedstoneSide.SIDE : RedstoneSide.NONE;
	}

	/** Refreshes only the finite set of trail blocks whose slope can depend on this position. */
	private void refreshNearbyTrailShapes(Level level, BlockPos changedPos) {
		BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			mutable.setWithOffset(changedPos, direction);
			refreshTrailShape(level, mutable);
			mutable.move(Direction.UP);
			refreshTrailShape(level, mutable);
			mutable.move(Direction.DOWN, 2);
			refreshTrailShape(level, mutable);
		}
	}

	private void refreshTrailShape(Level level, BlockPos pos) {
		BlockState current = level.getBlockState(pos);
		if (!current.is(this)) return;
		BlockState updated = getConnectionState(level, current, pos);
		if (updated != current) level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
	}

	private static boolean canSurviveOn(BlockGetter level, BlockPos pos, BlockState state) {
		return state.isFaceSturdy(level, pos, Direction.UP) || state.is(Blocks.HOPPER);
	}

	private static boolean isCross(BlockState state) {
		return state.getValue(NORTH).isConnected() && state.getValue(EAST).isConnected()
				&& state.getValue(SOUTH).isConnected() && state.getValue(WEST).isConnected();
	}

	private static boolean isDot(BlockState state) {
		return !state.getValue(NORTH).isConnected() && !state.getValue(EAST).isConnected()
				&& !state.getValue(SOUTH).isConnected() && !state.getValue(WEST).isConnected();
	}

	private static VoxelShape calculateShape(BlockState state) {
		VoxelShape shape = SHAPE_DOT;
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			RedstoneSide side = state.getValue(PROPERTY_BY_DIRECTION.get(direction));
			if (side == RedstoneSide.SIDE) shape = Shapes.or(shape, FLOOR_SHAPES.get(direction));
			if (side == RedstoneSide.UP) shape = Shapes.or(shape, UP_SHAPES.get(direction));
		}
		return shape;
	}

	private static Map<Direction, VoxelShape> createFloorShapes() {
		EnumMap<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
		shapes.put(Direction.NORTH, Block.box(3.0D, 0.0D, 0.0D, 13.0D, 1.0D, 13.0D));
		shapes.put(Direction.SOUTH, Block.box(3.0D, 0.0D, 3.0D, 13.0D, 1.0D, 16.0D));
		shapes.put(Direction.EAST, Block.box(3.0D, 0.0D, 3.0D, 16.0D, 1.0D, 13.0D));
		shapes.put(Direction.WEST, Block.box(0.0D, 0.0D, 3.0D, 13.0D, 1.0D, 13.0D));
		return Map.copyOf(shapes);
	}

	private static Map<Direction, VoxelShape> createUpShapes() {
		EnumMap<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
		shapes.put(Direction.NORTH, Shapes.or(FLOOR_SHAPES.get(Direction.NORTH), Block.box(3.0D, 0.0D, 0.0D, 13.0D, 16.0D, 1.0D)));
		shapes.put(Direction.SOUTH, Shapes.or(FLOOR_SHAPES.get(Direction.SOUTH), Block.box(3.0D, 0.0D, 15.0D, 13.0D, 16.0D, 16.0D)));
		shapes.put(Direction.EAST, Shapes.or(FLOOR_SHAPES.get(Direction.EAST), Block.box(15.0D, 0.0D, 3.0D, 16.0D, 16.0D, 13.0D)));
		shapes.put(Direction.WEST, Shapes.or(FLOOR_SHAPES.get(Direction.WEST), Block.box(0.0D, 0.0D, 3.0D, 1.0D, 16.0D, 13.0D)));
		return Map.copyOf(shapes);
	}
}
