package net.timothaty.timothatystrinkets.block;

import net.timothaty.timothatystrinkets.block.entity.DormantSphereBlockEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModBlockEntities;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

public final class DormantSphereBlock extends Block implements EntityBlock {
	private static final VoxelShape SHAPE = Block.box(5.0D, 5.0D, 5.0D, 11.0D, 11.0D, 11.0D);

	public DormantSphereBlock() {
		super(BlockBehaviour.Properties.of()
				.sound(SoundType.AMETHYST)
				.strength(0.8F, 3.0F)
				.noOcclusion()
				.lightLevel(state -> 7)
				.isRedstoneConductor((state, getter, pos) -> false));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DormantSphereBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return !level.isClientSide && type == TimothatysTrinketsModBlockEntities.DORMANT_SPHERE.get()
				? (tickLevel, tickPos, tickState, blockEntity) -> DormantSphereBlockEntity.serverTick(
						tickLevel,
						tickPos,
						tickState,
						(DormantSphereBlockEntity) blockEntity
				)
				: null;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		if (hand == InteractionHand.MAIN_HAND && stack.isEmpty()
				&& DyeColor.getColor(player.getOffhandItem()) != null) {
			return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		}
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
		Block.popResource(level, pos, createSphereStack(level, pos));
		level.playSound(
				null,
				pos,
				TimothatysTrinketsModSounds.ECHO_ORB_CLAIM.get(),
				SoundSource.BLOCKS,
				1.0F,
				2.0F + level.getRandom().nextFloat() * 0.5F
		);
		level.removeBlock(pos, false);
	}

	private static ItemStack createSphereStack(Level level, BlockPos pos) {
		ItemStack sphereStack = new ItemStack(TimothatysTrinketsModItems.DORMANT_SPHERE.get());
		if (level.getBlockEntity(pos) instanceof DormantSphereBlockEntity sphere) {
			sphere.saveToItem(sphereStack, level.registryAccess());
		}
		return sphereStack;
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> drops = super.getDrops(state, params);
		BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		if (blockEntity instanceof DormantSphereBlockEntity sphere) {
			for (ItemStack drop : drops) {
				if (drop.is(TimothatysTrinketsModItems.DORMANT_SPHERE.get())) {
					sphere.saveToItem(drop, params.getLevel().registryAccess());
				}
			}
		}
		return drops;
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
		ItemStack sphereStack = new ItemStack(TimothatysTrinketsModItems.DORMANT_SPHERE.get());
		if (level.getBlockEntity(pos) instanceof DormantSphereBlockEntity sphere) {
			sphere.saveToItem(sphereStack, level.registryAccess());
		}
		return sphereStack;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		if (level.getBlockEntity(pos) instanceof DormantSphereBlockEntity sphere) {
			sphere.syncToClient();
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents,
			TooltipFlag tooltipFlag) {
		super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
		tooltipComponents.add(Component.translatable("tooltip.timothatys_trinkets.dormant_sphere.core")
				.withStyle(ChatFormatting.GRAY));
		tooltipComponents.add(Component.translatable("tooltip.timothatys_trinkets.dormant_sphere.outline")
				.withStyle(ChatFormatting.DARK_GRAY));
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
