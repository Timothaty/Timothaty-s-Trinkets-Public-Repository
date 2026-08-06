package net.timothaty.timothatystrinkets.mechanics.blight;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModBlocks;
import net.timothaty.timothatystrinkets.mechanics.cleansing.CleansingZoneManager;
import net.timothaty.timothatystrinkets.mechanics.blight.storage.BlightSavedData;
import net.timothaty.timothatystrinkets.mechanics.blight.storage.BlightedBlockSnapshot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class BlightSpreadHelper {
	private static final TagKey<Block> CAN_BE_BLIGHTED = TagKey.create(
			Registries.BLOCK,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "can_be_blighted")
	);
	private static final Direction[] SPREAD_DIRECTIONS = Direction.values();

	private static long lastServerTick = -1L;
	private static int attemptsThisTick = 0;
	private static int globalThisTick = 0;
	private static final Map<ResourceKey<Level>, Long2IntOpenHashMap> perChunkThisTick = new HashMap<>();
	private static final List<SpreadJob> spreadJobs = new ArrayList<>();

	private BlightSpreadHelper() {
	}

	public enum SpreadTickResult {
		INFECTED,
		WAITING,
		RETRY,
		DORMANT
	}

	public static boolean trySpreadFrom(LevelAccessor world, double x, double y, double z) {
		if (!(world instanceof ServerLevel level)) {
			return false;
		}
		return trySpreadFrom(level, BlockPos.containing(x, y, z));
	}

	public static boolean trySpreadFrom(ServerLevel level, BlockPos origin) {
		return tickSpreadFrom(level, origin) == SpreadTickResult.INFECTED;
	}

	public static SpreadTickResult tickSpreadFrom(ServerLevel level, BlockPos origin) {
		resetTickLimitersIfNeeded(level);

		RandomSource random = level.getRandom();
		if (BlightConfig.SPREAD_CHANCE < 1.0F && random.nextFloat() > BlightConfig.SPREAD_CHANCE) {
			return SpreadTickResult.WAITING;
		}
		if (!reserveAttemptSlot()) {
			return SpreadTickResult.RETRY;
		}

		int originY = getOriginY(level, origin);
		int candidateMask = 0;
		BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
		for (int directionIndex = 0; directionIndex < SPREAD_DIRECTIONS.length; directionIndex++) {
			setRelative(target, origin, SPREAD_DIRECTIONS[directionIndex]);
			if (canInfect(level, target, originY)) {
				candidateMask |= 1 << directionIndex;
			}
		}

		if (candidateMask == 0) {
			return SpreadTickResult.DORMANT;
		}

		while (candidateMask != 0) {
			int directionIndex = selectRandomSetBit(random, candidateMask);
			candidateMask &= ~(1 << directionIndex);
			setRelative(target, origin, SPREAD_DIRECTIONS[directionIndex]);
			if (!reserveInfectionSlot(level, target)) {
				continue;
			}
			if (infectBlock(level, target, originY)) {
				return SpreadTickResult.INFECTED;
			}
		}

		return SpreadTickResult.RETRY;
	}

	public static boolean hasSpreadCandidate(ServerLevel level, BlockPos origin) {
		int originY = getOriginY(level, origin);
		BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
		for (Direction direction : SPREAD_DIRECTIONS) {
			setRelative(target, origin, direction);
			if (canInfect(level, target, originY)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isCompletelySurroundedByBlight(ServerLevel level, BlockPos origin) {
		BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
		for (Direction direction : SPREAD_DIRECTIONS) {
			setRelative(target, origin, direction);
			if (!level.hasChunkAt(target)
					|| !level.getBlockState(target).is(TimothatysTrinketsModBlocks.BLOCK_OF_BLIGHT.get())) {
				return false;
			}
		}
		return true;
	}

	public static void startPutrefactionSpread(ServerLevel level, LivingEntity entity) {
		BlockPos center = BlockPos.containing(entity.getX(), entity.getBoundingBox().minY - 0.1D, entity.getZ());
		int radius = Mth.nextInt(level.random, BlightConfig.PUTREFACTION_MIN_RADIUS, BlightConfig.PUTREFACTION_MAX_RADIUS);
		startRadialSpread(level, center, radius);
	}

	public static boolean infectBlock(ServerLevel level, BlockPos target, int originY) {
		if (CleansingZoneManager.isProtected(level, target)) {
			return false;
		}
		if (!canInfect(level, target, originY)) {
			return false;
		}

		BlockState replaced = level.getBlockState(target);
		return replaceWithBlight(level, target, replaced, originY);
	}

	public static boolean infectTaggedBlock(ServerLevel level, BlockPos target, int originY) {
		if (!level.hasChunkAt(target)) {
			return false;
		}
		if (CleansingZoneManager.isProtected(level, target)) {
			return false;
		}

		BlockState state = level.getBlockState(target);
		if (state.is(TimothatysTrinketsModBlocks.BLOCK_OF_BLIGHT.get()) || !state.is(CAN_BE_BLIGHTED)) {
			return false;
		}
		if (hasProtectedBlockEntity(level, target, state)) {
			return false;
		}

		return replaceWithBlight(level, target, state, originY);
	}

	public static boolean canBeBlighted(BlockState state) {
		return state.is(CAN_BE_BLIGHTED);
	}

	private static void startRadialSpread(ServerLevel level, BlockPos center, int radius) {
		List<BlockPos> positions = new ArrayList<>();
		int radiusSqr = radius * radius;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if ((dx * dx) + (dz * dz) > radiusSqr) {
					continue;
				}

				BlockPos candidate = findBlightableGroundAt(level, center.offset(dx, 0, dz));
				if (candidate != null) {
					positions.add(candidate);
				}
			}
		}

		positions.sort(Comparator.comparingInt(pos -> pos.distManhattan(center)));
		if (!positions.isEmpty()) {
			spreadJobs.add(new SpreadJob(level.dimension(), positions, center.getY()));
		}
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (spreadJobs.isEmpty()) {
			return;
		}

		Iterator<SpreadJob> iterator = spreadJobs.iterator();
		while (iterator.hasNext()) {
			SpreadJob job = iterator.next();
			ServerLevel level = event.getServer().getLevel(job.dimension);
			if (level == null || job.tick(level)) {
				iterator.remove();
			}
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		spreadJobs.clear();
		perChunkThisTick.clear();
		lastServerTick = -1L;
		attemptsThisTick = 0;
		globalThisTick = 0;
	}

	private static BlockPos findBlightableGroundAt(ServerLevel level, BlockPos base) {
		for (int dy = 0; dy >= -2; dy--) {
			BlockPos candidate = base.offset(0, dy, 0);
			if (canInfect(level, candidate, base.getY())) {
				return candidate;
			}
		}
		return null;
	}

	private static boolean canInfect(ServerLevel level, BlockPos target, int originY) {
		if (target.getY() < originY - BlightConfig.SPREAD_MAX_DEPTH_BELOW_ORIGIN) {
			return false;
		}
		if (!level.hasChunkAt(target)) {
			return false;
		}
		if (CleansingZoneManager.isProtected(level, target)) {
			return false;
		}

		BlockState state = level.getBlockState(target);
		if (state.is(TimothatysTrinketsModBlocks.BLOCK_OF_BLIGHT.get()) || !state.is(CAN_BE_BLIGHTED)) {
			return false;
		}
		return !hasProtectedBlockEntity(level, target, state);
	}

	private static int getOriginY(ServerLevel level, BlockPos pos) {
		BlightedBlockSnapshot snapshot = BlightSavedData.get(level).getSnapshot(pos);
		return snapshot == null ? pos.getY() : snapshot.originY();
	}

	private static boolean hasProtectedBlockEntity(ServerLevel level, BlockPos pos, BlockState state) {
		return state.hasBlockEntity() || level.getBlockEntity(pos) != null;
	}

	private static boolean replaceWithBlight(ServerLevel level, BlockPos pos, BlockState replaced, int originY) {
		BlightSavedData data = BlightSavedData.get(level);
		boolean addedSnapshot = data.remember(pos, replaced, originY);
		if (!addedSnapshot && !data.contains(pos)) {
			return false;
		}

		boolean replacedBlock = level.setBlock(pos, TimothatysTrinketsModBlocks.BLOCK_OF_BLIGHT.get().defaultBlockState(), 3);
		if (!replacedBlock) {
			if (addedSnapshot) {
				data.remove(pos);
			}
			return false;
		}

		removePlantAbove(level, pos);
		return true;
	}

	private static void removePlantAbove(ServerLevel level, BlockPos pos) {
		BlockPos above = pos.above();
		if (!level.hasChunkAt(above)) {
			return;
		}

		BlockState aboveState = level.getBlockState(above);
		if (!aboveState.isAir() && isPlantLike(aboveState)) {
			level.setBlock(above, Blocks.AIR.defaultBlockState(), 3);
		}
	}

	private static void resetTickLimitersIfNeeded(ServerLevel level) {
		long serverTick = level.getServer().getTickCount();
		if (serverTick != lastServerTick) {
			lastServerTick = serverTick;
			attemptsThisTick = 0;
			globalThisTick = 0;
			for (Long2IntOpenHashMap chunkCounts : perChunkThisTick.values()) {
				chunkCounts.clear();
			}
		}
	}

	private static boolean reserveAttemptSlot() {
		if (BlightConfig.SPREAD_MAX_GLOBAL_ATTEMPTS_PER_TICK > 0
				&& attemptsThisTick >= BlightConfig.SPREAD_MAX_GLOBAL_ATTEMPTS_PER_TICK) {
			return false;
		}
		attemptsThisTick++;
		return true;
	}

	private static boolean reserveInfectionSlot(ServerLevel level, BlockPos chosen) {
		if (BlightConfig.SPREAD_MAX_GLOBAL_PER_TICK > 0 && globalThisTick >= BlightConfig.SPREAD_MAX_GLOBAL_PER_TICK) {
			return false;
		}

		Long2IntOpenHashMap chunkCounts = perChunkThisTick.computeIfAbsent(
				level.dimension(),
				ignored -> new Long2IntOpenHashMap());
		long chunkKey = ChunkPos.asLong(chosen.getX() >> 4, chosen.getZ() >> 4);
		int used = chunkCounts.get(chunkKey);
		if (used >= BlightConfig.SPREAD_MAX_PER_CHUNK_PER_TICK) {
			return false;
		}

		chunkCounts.put(chunkKey, used + 1);
		globalThisTick++;
		return true;
	}

	private static int selectRandomSetBit(RandomSource random, int mask) {
		int selected = random.nextInt(Integer.bitCount(mask));
		while (selected-- > 0) {
			mask &= mask - 1;
		}
		return Integer.numberOfTrailingZeros(mask);
	}

	private static void setRelative(BlockPos.MutableBlockPos target, BlockPos origin, Direction direction) {
		target.set(
				origin.getX() + direction.getStepX(),
				origin.getY() + direction.getStepY(),
				origin.getZ() + direction.getStepZ());
	}

	private static boolean isPlantLike(BlockState state) {
		Block b = state.getBlock();
		return (b instanceof BushBlock)
				|| (b instanceof CropBlock)
				|| (b instanceof DoublePlantBlock)
				|| (b instanceof VineBlock)
				|| state.is(BlockTags.FLOWERS)
				|| state.is(BlockTags.SAPLINGS)
				|| state.is(BlockTags.CROPS)
				|| state.canBeReplaced();
	}

	private static final class SpreadJob {
		private final ResourceKey<Level> dimension;
		private final List<BlockPos> positions;
		private final int originY;
		private int index;

		private SpreadJob(ResourceKey<Level> dimension, List<BlockPos> positions, int originY) {
			this.dimension = dimension;
			this.positions = positions;
			this.originY = originY;
		}

		private boolean tick(ServerLevel level) {
			int processed = 0;
			while (index < positions.size() && processed < BlightConfig.PUTREFACTION_BLOCKS_PER_TICK) {
				infectBlock(level, positions.get(index), originY);
				index++;
				processed++;
			}
			return index >= positions.size();
		}
	}
}
