package net.timothaty.timothatystrinkets.mechanics.flaming_ember;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("deprecation")
public final class FlamingEmberEnvironment {
	private static final long SUN_BONUS_DAY_END = 12000L;
	private static final long FULL_DAY_TICKS = 24000L;

	public record HeatBreakdown(int heatSources, double heatSource, double biome, double sun) {
		public static final HeatBreakdown EMPTY = new HeatBreakdown(0, 0.0D, 0.0D, 0.0D);

		public double total() {
			return heatSource + biome + sun;
		}
	}

	private FlamingEmberEnvironment() {
	}

	public static double getPassiveHeatPerSecond(Player player) {
		return getPassiveHeatBreakdown(player).total();
	}

	public static HeatBreakdown getPassiveHeatBreakdown(Player player) {
		if (player == null || !player.isAlive() || player.isSpectator())
			return HeatBreakdown.EMPTY;

		int heatSources = getCachedHeatSourceCount(player);
		double heatSourceHeat = heatSources * FlamingEmberData.HEAT_SOURCE_HEAT_PER_SECOND;
		double biomeHeat = 0.0D;
		double sunHeat = 0.0D;

		boolean coldBiome = isInColdBiome(player);
		if (isInHotBiome(player)) {
			biomeHeat += FlamingEmberData.HOT_BIOME_HEAT_PER_SECOND;
		}
		if (coldBiome) {
			biomeHeat += FlamingEmberData.COLD_BIOME_HEAT_PER_SECOND;
		} else if (hasVisibleSun(player)) {
			sunHeat += FlamingEmberData.SUN_HEAT_PER_SECOND;
		}

		return new HeatBreakdown(heatSources, heatSourceHeat, biomeHeat, sunHeat);
	}

	public static boolean isInHotBiome(Player player) {
		return player != null
				&& player.level().hasChunkAt(player.blockPosition())
				&& player.level().getBiome(player.blockPosition()).is(FlamingEmberTags.HOT_BIOMES);
	}

	public static boolean isInColdBiome(Player player) {
		return player != null
				&& player.level().hasChunkAt(player.blockPosition())
				&& player.level().getBiome(player.blockPosition()).is(FlamingEmberTags.COLD_BIOMES);
	}

	private static boolean hasVisibleSun(Player player) {
		if (player == null)
			return false;

		Level level = player.level();
		if (!level.dimensionType().hasSkyLight())
			return false;
		if (!level.isDay())
			return false;

		long dayTime = Math.floorMod(level.getDayTime(), FULL_DAY_TICKS);
		if (dayTime >= SUN_BONUS_DAY_END)
			return false;
		if (player.isInWaterRainOrBubble())
			return false;

		BlockPos eyePos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
		return level.canSeeSkyFromBelowWater(eyePos);
	}

	private static int getCachedHeatSourceCount(Player player) {
		CompoundTag data = player.getPersistentData();
		long now = player.level().getGameTime();
		long lastCheck = data.getLong(FlamingEmberData.NBT_HEAT_SOURCE_CHECK_TICK);
		if (lastCheck <= 0L || now - lastCheck >= FlamingEmberData.HEAT_SOURCE_CHECK_INTERVAL_TICKS) {
			int count = countNearbyHeatSources(player);
			data.putLong(FlamingEmberData.NBT_HEAT_SOURCE_CHECK_TICK, now);
			data.putInt(FlamingEmberData.NBT_HEAT_SOURCE_COUNT, count);
			return count;
		}

		return Math.min(FlamingEmberData.MAX_HEAT_SOURCES, Math.max(0, data.getInt(FlamingEmberData.NBT_HEAT_SOURCE_COUNT)));
	}

	private static int countNearbyHeatSources(Player player) {
		Level level = player.level();
		BlockPos center = player.blockPosition();
		int radius = FlamingEmberData.HEAT_SOURCE_RADIUS;
		int radiusSqr = radius * radius;
		int found = 0;

		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
			if (pos.distSqr(center) > radiusSqr)
				continue;
			if (!level.hasChunkAt(pos))
				continue;
			if (!isHeatSource(level, pos))
				continue;

			found++;
			if (found >= FlamingEmberData.MAX_HEAT_SOURCES)
				return FlamingEmberData.MAX_HEAT_SOURCES;
		}

		return found;
	}

	private static boolean isHeatSource(Level level, BlockPos pos) {
		if (level.getFluidState(pos).is(FluidTags.LAVA))
			return true;

		BlockState state = level.getBlockState(pos);
		return state.is(FlamingEmberTags.HEAT_SOURCES);
	}
}
