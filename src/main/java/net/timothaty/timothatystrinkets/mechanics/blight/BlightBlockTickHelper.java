package net.timothaty.timothatystrinkets.mechanics.blight;

import net.minecraft.world.level.LevelAccessor;

public class BlightBlockTickHelper {

	public static void spreadFrom(LevelAccessor world, double x, double y, double z) {
		trySpreadFrom(world, x, y, z);
	}

	public static boolean trySpreadFrom(LevelAccessor world, double x, double y, double z) {
		return BlightSpreadHelper.trySpreadFrom(world, x, y, z);
	}
}
