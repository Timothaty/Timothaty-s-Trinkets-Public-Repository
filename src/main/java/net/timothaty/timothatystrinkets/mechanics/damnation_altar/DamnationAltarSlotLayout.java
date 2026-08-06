package net.timothaty.timothatystrinkets.mechanics.damnation_altar;

import net.timothaty.timothatystrinkets.block.DamnationAltarBlock;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class DamnationAltarSlotLayout {
	public static final double OUTER_ITEM_Y = 1.025D;
	public static final double CENTER_IDLE_Y = 1.20D;
	public static final double CENTER_RAISED_Y = 1.55D;

	private static final double CORNER_LOW_MIN = 0.12D;
	private static final double CORNER_LOW_MAX = 0.43D;
	private static final double CORNER_HIGH_MIN = 0.57D;
	private static final double CORNER_HIGH_MAX = 0.88D;
	private static final double CENTER_MIN = 0.37D;
	private static final double CENTER_MAX = 0.63D;

	private static final double OUTER_LOW = 0.08D;
	private static final double OUTER_HIGH = 0.92D;

	private DamnationAltarSlotLayout() {
	}

	public static DamnationAltarSlot resolveSlot(BlockState state, BlockHitResult hit) {
		if (hit.getDirection() != Direction.UP) return null;

		double worldX = hit.getLocation().x - hit.getBlockPos().getX();
		double worldZ = hit.getLocation().z - hit.getBlockPos().getZ();
		LocalPoint local = toLocal(state.getValue(DamnationAltarBlock.FACING), worldX, worldZ);

		if (inside(local.x(), CENTER_MIN, CENTER_MAX) && inside(local.z(), CENTER_MIN, CENTER_MAX)) return DamnationAltarSlot.CENTER;
		if (inside(local.x(), CORNER_LOW_MIN, CORNER_LOW_MAX) && inside(local.z(), CORNER_LOW_MIN, CORNER_LOW_MAX)) return DamnationAltarSlot.NORTH_WEST;
		if (inside(local.x(), CORNER_HIGH_MIN, CORNER_HIGH_MAX) && inside(local.z(), CORNER_LOW_MIN, CORNER_LOW_MAX)) return DamnationAltarSlot.NORTH_EAST;
		if (inside(local.x(), CORNER_HIGH_MIN, CORNER_HIGH_MAX) && inside(local.z(), CORNER_HIGH_MIN, CORNER_HIGH_MAX)) return DamnationAltarSlot.SOUTH_EAST;
		if (inside(local.x(), CORNER_LOW_MIN, CORNER_LOW_MAX) && inside(local.z(), CORNER_HIGH_MIN, CORNER_HIGH_MAX)) return DamnationAltarSlot.SOUTH_WEST;
		return null;
	}

	public static LocalPoint getWorldPosition(Direction facing, DamnationAltarSlot slot) {
		LocalPoint local = getLocalPosition(slot);
		double x = local.x() - 0.5D;
		double z = local.z() - 0.5D;
		return switch (facing) {
			case EAST -> new LocalPoint(0.5D - z, 0.5D + x);
			case SOUTH -> new LocalPoint(0.5D - x, 0.5D - z);
			case WEST -> new LocalPoint(0.5D + z, 0.5D - x);
			default -> local;
		};
	}

	private static LocalPoint getLocalPosition(DamnationAltarSlot slot) {
		return switch (slot) {
			case NORTH_WEST -> new LocalPoint(OUTER_LOW, OUTER_LOW);
			case NORTH_EAST -> new LocalPoint(OUTER_HIGH, OUTER_LOW);
			case SOUTH_EAST -> new LocalPoint(OUTER_HIGH, OUTER_HIGH);
			case SOUTH_WEST -> new LocalPoint(OUTER_LOW, OUTER_HIGH);
			case CENTER -> new LocalPoint(0.5D, 0.5D);
		};
	}

	private static LocalPoint toLocal(Direction facing, double worldX, double worldZ) {
		double x = worldX - 0.5D;
		double z = worldZ - 0.5D;
		return switch (facing) {
			case EAST -> new LocalPoint(0.5D + z, 0.5D - x);
			case SOUTH -> new LocalPoint(0.5D - x, 0.5D - z);
			case WEST -> new LocalPoint(0.5D - z, 0.5D + x);
			default -> new LocalPoint(worldX, worldZ);
		};
	}

	private static boolean inside(double value, double min, double max) {
		return value >= min && value <= max;
	}

	public record LocalPoint(double x, double z) {
	}
}
