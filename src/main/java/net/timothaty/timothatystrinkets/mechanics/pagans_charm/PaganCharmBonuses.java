package net.timothaty.timothatystrinkets.mechanics.pagans_charm;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class PaganCharmBonuses {
	public record ChargeBreakdown(double base, double campfire, double otherMeditator, double fishing, double uniqueBiome) {
		public static final ChargeBreakdown EMPTY = new ChargeBreakdown(0.0D, 0.0D, 0.0D, 0.0D, 0.0D);

		public double total() {
			return base + campfire + otherMeditator + fishing + uniqueBiome;
		}
	}

	private PaganCharmBonuses() {
	}

	public static double getChargePerSecond(Player player) {
		return getChargeBreakdown(player).total();
	}

	public static ChargeBreakdown getChargeBreakdown(Player player) {
		if (player instanceof PaganCharmMeditationPlayerState state) {
			int cachedAt = state.timothatys_trinkets$getPaganCharmBonusCacheTick();
			if (cachedAt != Integer.MIN_VALUE
					&& player.tickCount >= cachedAt
					&& player.tickCount - cachedAt < PaganCharmMeditationPlayerState.BONUS_CACHE_INTERVAL_TICKS)
				return state.timothatys_trinkets$getPaganCharmCachedChargeBreakdown();

			ChargeBreakdown breakdown = calculateChargeBreakdown(player);
			state.timothatys_trinkets$setPaganCharmBonusCache(player.tickCount, breakdown);
			return breakdown;
		}

		return calculateChargeBreakdown(player);
	}

	private static ChargeBreakdown calculateChargeBreakdown(Player player) {
		double campfire = hasNearbyLitCampfire(player) ? PaganCharmTuning.CAMPFIRE_BONUS_PER_SECOND : 0.0D;
		double otherMeditator = hasNearbyMeditatingPlayer(player) ? PaganCharmTuning.OTHER_MEDITATOR_BONUS_PER_SECOND : 0.0D;
		double fishing = hasFishingBobberInOpenWater(player) ? PaganCharmTuning.FISHING_BONUS_PER_SECOND : 0.0D;
		double uniqueBiome = PaganCharmMeditationRules.isInUniquePaganBiome(player) ? PaganCharmTuning.UNIQUE_BIOME_BONUS_PER_SECOND : 0.0D;

		return new ChargeBreakdown(PaganCharmTuning.BASE_CHARGE_PER_SECOND, campfire, otherMeditator, fishing, uniqueBiome);
	}

	private static boolean hasFishingBobberInOpenWater(Player player) {
		FishingHook bobber = player.fishing;
		if (bobber == null || !bobber.isAlive())
			return false;

		return bobber.isOpenWaterFishing()
				&& isFishingBobberInWater(bobber)
				&& hasEnoughWaterAroundBobber(bobber);
	}

	private static boolean isFishingBobberInWater(FishingHook bobber) {
		return bobber.isInWaterOrBubble()
				|| bobber.getFluidHeight(FluidTags.WATER) > 0.0D
				|| bobber.level().getFluidState(bobber.blockPosition()).is(FluidTags.WATER);
	}

	private static boolean hasEnoughWaterAroundBobber(FishingHook bobber) {
		Level level = bobber.level();
		BlockPos center = bobber.blockPosition();
		int waterBlocks = 0;

		for (BlockPos pos : BlockPos.betweenClosed(
				center.offset(-PaganCharmTuning.FISHING_WATER_VOLUME_RADIUS, PaganCharmTuning.FISHING_WATER_VOLUME_MIN_Y_OFFSET, -PaganCharmTuning.FISHING_WATER_VOLUME_RADIUS),
				center.offset(PaganCharmTuning.FISHING_WATER_VOLUME_RADIUS, PaganCharmTuning.FISHING_WATER_VOLUME_MAX_Y_OFFSET, PaganCharmTuning.FISHING_WATER_VOLUME_RADIUS))) {
			if (!level.hasChunkAt(pos))
				continue;
			if (level.getFluidState(pos).is(FluidTags.WATER)
					&& ++waterBlocks >= PaganCharmTuning.FISHING_MIN_WATER_BLOCKS)
				return true;
		}

		return false;
	}

	private static boolean hasNearbyLitCampfire(Player player) {
		BlockPos center = player.blockPosition();
		BlockPos min = center.offset(-PaganCharmTuning.CAMPFIRE_RADIUS, -PaganCharmTuning.CAMPFIRE_RADIUS, -PaganCharmTuning.CAMPFIRE_RADIUS);
		BlockPos max = center.offset(PaganCharmTuning.CAMPFIRE_RADIUS, PaganCharmTuning.CAMPFIRE_RADIUS, PaganCharmTuning.CAMPFIRE_RADIUS);

		for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
			if (pos.distSqr(center) > PaganCharmTuning.CAMPFIRE_RADIUS * PaganCharmTuning.CAMPFIRE_RADIUS)
				continue;

			BlockState state = player.level().getBlockState(pos);
			if (CampfireBlock.isLitCampfire(state))
				return true;
		}

		return false;
	}

	private static boolean hasNearbyMeditatingPlayer(Player player) {
		for (Player other : player.level().players()) {
			if (other == player || !other.isAlive() || other.isSpectator())
				continue;
			if (other.distanceToSqr(player) > PaganCharmTuning.OTHER_MEDITATOR_RADIUS_SQR)
				continue;
			if (other instanceof PaganCharmMeditationPlayerState state
					&& state.timothatys_trinkets$getPaganCharmMeditationPhase(other.tickCount) != PaganCharmMeditationPlayerState.PHASE_NONE)
				return true;
		}

		return false;
	}
}
