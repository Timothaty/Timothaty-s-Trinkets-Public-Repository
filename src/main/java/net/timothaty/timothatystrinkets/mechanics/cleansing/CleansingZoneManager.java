package net.timothaty.timothatystrinkets.mechanics.cleansing;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.entity.TargetAreaEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModBlocks;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.mechanics.blight.storage.BlightSavedData;
import net.timothaty.timothatystrinkets.mechanics.blight.storage.BlightedBlockSnapshot;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class CleansingZoneManager {
	private static final int DURATION_TICKS = 20 * 60 * 2;
	private static final int HALF_SIZE = 3;
	private static final int CLEANSING_DEPTH = 6;
	private static final int CLEANSING_HEIGHT = 5;
	private static final int BLOCKS_CLEANSED_PER_SECOND = 5;
	private static final float UNDEAD_DAMAGE_PER_SECOND = 2.0F;
	private static final float VISUAL_SIZE = 7.0F;
	private static final float VISUAL_HEIGHT = 0.25F;
	private static final int VISUAL_COLOR = 0x80FF66CC;
	private static final int VISUAL_FADE_IN_TICKS = 10;
	private static final int VISUAL_FADE_OUT_TICKS = 20;
	private static final double UP_PARTICLE_SPAWN_CHANCE_PER_TICK = 0.35D;
	private static final BlockState FALLBACK_RESTORED_BLOCK = Blocks.DIRT.defaultBlockState();
	private static final List<CleansingZone> ACTIVE_ZONES = new ArrayList<>();

	private CleansingZoneManager() {
	}

	public static void createZone(ServerLevel level, Player owner, BlockPos center) {
		long endGameTime = level.getGameTime() + DURATION_TICKS;
		ACTIVE_ZONES.add(new CleansingZone(level, center.immutable(), HALF_SIZE, CLEANSING_DEPTH, CLEANSING_HEIGHT, endGameTime));
		TargetAreaEntity.spawnSquareOnBlock(level, center, VISUAL_SIZE, VISUAL_COLOR, DURATION_TICKS, VISUAL_HEIGHT, VISUAL_FADE_IN_TICKS, VISUAL_FADE_OUT_TICKS);
	}

	public static boolean isProtected(Level level, BlockPos pos) {
		if (level == null || pos == null) {
			return false;
		}

		ResourceKey<Level> dimension = level.dimension();
		long gameTime = level.getGameTime();
		for (CleansingZone zone : ACTIVE_ZONES) {
			if (zone.endGameTime > gameTime && zone.dimension.equals(dimension) && zone.contains(pos)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isProtected(ServerLevel level, BlockPos pos) {
		return isProtected((Level) level, pos);
	}

	@SubscribeEvent
	public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
		if (!(event.getLevel() instanceof ServerLevel level)
				|| !event.getPlacedBlock().is(TimothatysTrinketsModBlocks.BLOCK_OF_BLIGHT.get())) {
			return;
		}

		BlockPos pos = event.getPos().immutable();
		ResourceKey<Level> dimension = level.dimension();
		long gameTime = level.getGameTime();
		for (CleansingZone zone : ACTIVE_ZONES) {
			if (zone.endGameTime > gameTime && zone.dimension.equals(dimension) && zone.contains(pos)) {
				zone.enqueueCleansingCandidate(pos);
			}
		}
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (ACTIVE_ZONES.isEmpty()) {
			return;
		}

		Iterator<CleansingZone> iterator = ACTIVE_ZONES.iterator();
		while (iterator.hasNext()) {
			CleansingZone zone = iterator.next();
			ServerLevel level = event.getServer().getLevel(zone.dimension);
			if (level == null || level.getGameTime() >= zone.endGameTime) {
				iterator.remove();
				continue;
			}

			zone.spawnUpParticle(level);
			if (level.getGameTime() % 20L == 0L) {
				zone.tick(level);
			}
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		ACTIVE_ZONES.clear();
	}

	private static final class CleansingZone {
		private final ResourceKey<Level> dimension;
		private final BlockPos center;
		private final int halfSize;
		private final int depth;
		private final int height;
		private final long endGameTime;
		private final Deque<BlockPos> cleansingCandidates;
		private int fallbackRestoreCount;

		private CleansingZone(ServerLevel level, BlockPos center, int halfSize, int depth, int height, long endGameTime) {
			this.dimension = level.dimension();
			this.center = center;
			this.halfSize = halfSize;
			this.depth = depth;
			this.height = height;
			this.endGameTime = endGameTime;
			this.cleansingCandidates = collectCleansingCandidates(level);
		}

		private boolean contains(BlockPos pos) {
			if (Math.abs(pos.getX() - center.getX()) > halfSize || Math.abs(pos.getZ() - center.getZ()) > halfSize) {
				return false;
			}

			int minY = center.getY() - depth + 1;
			int maxY = center.getY() + height;
			return pos.getY() >= minY && pos.getY() <= maxY;
		}

		private void tick(ServerLevel level) {
			cleanseBlight(level);
			hurtUndead(level);
		}

		private void spawnUpParticle(ServerLevel level) {
			if (level.random.nextDouble() >= UP_PARTICLE_SPAWN_CHANCE_PER_TICK) {
				return;
			}

			double halfVisualSize = VISUAL_SIZE * 0.5D - 0.20D;
			double x = center.getX() + 0.5D + (level.random.nextDouble() * 2.0D - 1.0D) * halfVisualSize;
			double y = center.getY() + 1.07D + level.random.nextDouble() * 0.08D;
			double z = center.getZ() + 0.5D + (level.random.nextDouble() * 2.0D - 1.0D) * halfVisualSize;
			level.sendParticles(TimothatysTrinketsModParticleTypes.CLEANSING_DUST_PARTICLE_UP.get(), x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}

		private void cleanseBlight(ServerLevel level) {
			int remainingCandidates = cleansingCandidates.size();
			int cleansed = 0;
			while (remainingCandidates-- > 0 && cleansed < BLOCKS_CLEANSED_PER_SECOND) {
				BlockPos pos = cleansingCandidates.removeFirst();
				if (!level.hasChunkAt(pos)) {
					cleansingCandidates.addLast(pos);
					continue;
				}
				if (!level.getBlockState(pos).is(TimothatysTrinketsModBlocks.BLOCK_OF_BLIGHT.get())) {
					continue;
				}

				if (restoreBlightedBlock(level, pos)) {
					cleansed++;
				} else {
					cleansingCandidates.addLast(pos);
				}
			}
		}

		private void enqueueCleansingCandidate(BlockPos pos) {
			if (!cleansingCandidates.contains(pos)) {
				cleansingCandidates.addLast(pos);
			}
		}

		private Deque<BlockPos> collectCleansingCandidates(ServerLevel level) {
			List<BlockPos> candidates = new ArrayList<>();
			for (int dx = -halfSize; dx <= halfSize; dx++) {
				for (int dz = -halfSize; dz <= halfSize; dz++) {
					for (int dy = -depth + 1; dy <= height; dy++) {
						BlockPos pos = center.offset(dx, dy, dz);
						if (!level.hasChunkAt(pos) || level.getBlockState(pos).is(TimothatysTrinketsModBlocks.BLOCK_OF_BLIGHT.get())) {
							candidates.add(pos.immutable());
						}
					}
				}
			}

			candidates.sort(Comparator.comparingInt(this::distanceScore));
			return new ArrayDeque<>(candidates);
		}

		private int distanceScore(BlockPos pos) {
			int horizontal = Math.abs(pos.getX() - center.getX()) + Math.abs(pos.getZ() - center.getZ());
			int vertical = Math.abs(pos.getY() - center.getY());
			return horizontal * 10 + vertical;
		}

		private boolean restoreBlightedBlock(ServerLevel level, BlockPos pos) {
			BlightSavedData data = BlightSavedData.get(level);
			BlightedBlockSnapshot snapshot = data.getSnapshot(pos);

			boolean corruptSnapshot = snapshot == null
					|| snapshot.originalState().is(TimothatysTrinketsModBlocks.BLOCK_OF_BLIGHT.get());
			BlockState restored = corruptSnapshot ? FALLBACK_RESTORED_BLOCK : snapshot.originalState();
			if (!level.setBlock(pos, restored, 3)) {
				return false;
			}

			data.remove(pos);
			if (corruptSnapshot) {
				fallbackRestoreCount++;
			}
			if (corruptSnapshot && fallbackRestoreCount == 1) {
				TimothatysTrinketsMod.LOGGER.warn(
						"Cleansing Zone at {} in {} restored Blight without a valid snapshot to dirt; further fallback warnings for this zone are suppressed!",
						center,
						dimension.location());
			}
			return true;
		}

		private void hurtUndead(ServerLevel level) {
			AABB bounds = new AABB(
				center.getX() - halfSize - 0.5D,
				center.getY() - 1.0D,
				center.getZ() - halfSize - 0.5D,
				center.getX() + halfSize + 1.5D,
				center.getY() + 3.0D,
				center.getZ() + halfSize + 1.5D
			);

			// remove
			List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, bounds, entity -> entity.isAlive() && entity.isInvertedHealAndHarm());
			for (LivingEntity target : targets) {
				target.hurt(level.damageSources().magic(), UNDEAD_DAMAGE_PER_SECOND);
			}
		}
	}
}
