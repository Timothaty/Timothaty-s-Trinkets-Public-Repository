package net.timothaty.timothatystrinkets.mechanics.olibanum;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class OlibanumPitEvents {
	private static final List<BlockPos> STRUCTURE_OFFSETS_FROM_POT = List.of(
			BlockPos.ZERO,
			new BlockPos(0, 1, 0),
			new BlockPos(0, -1, 0),
			new BlockPos(0, -2, 0),
			new BlockPos(1, 0, 0),
			new BlockPos(-1, 0, 0),
			new BlockPos(0, 0, 1),
			new BlockPos(0, 0, -1),
			new BlockPos(1, -1, 0),
			new BlockPos(-1, -1, 0),
			new BlockPos(0, -1, 1),
			new BlockPos(0, -1, -1)
	);

	private OlibanumPitEvents() {
	}

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (event.getLevel() instanceof ServerLevel level) {
			queuePotsNear(level, event.getPos());
		}
	}

	@SubscribeEvent
	public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
		if (event.getLevel() instanceof ServerLevel level) {
			queuePotsNear(level, event.getPos());
		}
	}

	@SubscribeEvent
	public static void onBlockBroken(BlockEvent.BreakEvent event) {
		if (event.getLevel() instanceof ServerLevel level) {
			queuePotsNear(level, event.getPos());
		}
	}

	@SubscribeEvent
	public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
		if (event.getLevel() instanceof ServerLevel level) {
			queuePotsNear(level, event.getPos());
		}
	}

	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent.Post event) {
		if (event.getLevel() instanceof ServerLevel level) {
			OlibanumPitSavedData.get(level).tick(level);
		}
	}

	private static void queuePotsNear(ServerLevel level, BlockPos changedPos) {
		OlibanumPitSavedData data = null;
		for (BlockPos offset : STRUCTURE_OFFSETS_FROM_POT) {
			BlockPos potPos = changedPos.offset(-offset.getX(), -offset.getY(), -offset.getZ());
			if (!level.hasChunkAt(potPos) || !level.getBlockState(potPos).is(Blocks.DECORATED_POT)) {
				continue;
			}
			if (data == null) {
				data = OlibanumPitSavedData.get(level);
			}
			data.requestStartCheck(potPos);
		}
	}
}
