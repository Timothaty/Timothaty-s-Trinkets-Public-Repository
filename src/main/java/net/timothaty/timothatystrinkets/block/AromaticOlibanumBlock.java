package net.timothaty.timothatystrinkets.block;

import com.mojang.serialization.MapCodec;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public final class AromaticOlibanumBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<AromaticOlibanumBlock> CODEC = simpleCodec(AromaticOlibanumBlock::new);
	public static final IntegerProperty AGE = BlockStateProperties.AGE_2;
	public static final int MAX_AGE = 2;

	private static final Map<Direction, VoxelShape> SHAPES = createShapes();

	public AromaticOlibanumBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(AGE, 0));
	}

	@Override
	public MapCodec<AromaticOlibanumBlock> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return state.getValue(AGE) < MAX_AGE;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		int age = state.getValue(AGE);
		if (age < MAX_AGE && random.nextInt(5) == 0) {
			level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockPos supportPos = pos.relative(state.getValue(FACING).getOpposite());
		BlockState support = level.getBlockState(supportPos);
		return support.is(Blocks.OAK_LOG) || support.is(Blocks.BIRCH_LOG);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction changedDirection, BlockState neighborState,
			LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (changedDirection == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
			return Blocks.AIR.defaultBlockState();
		}
		return state;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (state.getValue(AGE) != MAX_AGE) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide) {
			ItemStack resin = new ItemStack(TimothatysTrinketsModItems.AROMATIC_OLIBANUM.get());
			if (!player.getInventory().add(resin)) {
				Block.popResource(level, pos, resin);
			}
			level.setBlock(pos, state.setValue(AGE, 0), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 0.4F, 1.15F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, AGE);
	}

	private static Map<Direction, VoxelShape> createShapes() {
		EnumMap<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
		shapes.put(Direction.NORTH, Block.box(0.0D, 0.0D, 15.99D, 16.0D, 16.0D, 16.0D));
		shapes.put(Direction.SOUTH, Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 0.01D));
		shapes.put(Direction.EAST, Block.box(0.0D, 0.0D, 0.0D, 0.01D, 16.0D, 16.0D));
		shapes.put(Direction.WEST, Block.box(15.99D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D));
		return Map.copyOf(shapes);
	}
}
