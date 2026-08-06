package net.timothaty.timothatystrinkets.mechanics.pagans_charm;

import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;

public final class PaganCharmMeditationRules {
	private PaganCharmMeditationRules() {
	}

	public static boolean canCountAsIdle(Player player, boolean initialized, boolean moved) {
		if (!initialized || moved)
			return false;
		if (!player.isAlive() || player.isDeadOrDying() || player.isSpectator())
			return false;
		if (TimothatysTrinketsStunHelper.isStunned(player) || TimothatysTrinketsStunHelper.isStaggered(player))
			return false;
		if (!PaganCharmCharge.hasEquippedCharmForMeditation(player))
			return false;
		if (player.hurtTime > 0)
			return false;
		if (player.isCrouching() || player.isSprinting() || player.isPassenger() || player.isSleeping())
			return false;
		if (player.isFallFlying() || player.isVisuallySwimming() || player.isSwimming() || !player.onGround())
			return false;
		if (isInForbiddenFluid(player))
			return false;

		return canMeditateInCurrentBiome(player);
	}

	public static boolean canMeditateInCurrentBiome(Player player) {
		if (!player.level().hasChunkAt(player.blockPosition()))
			return false;

		return !player.level().getBiome(player.blockPosition()).is(PaganCharmTags.PAGAN_BIOMES);
	}

	public static boolean isInUniquePaganBiome(Player player) {
		if (!player.level().hasChunkAt(player.blockPosition()))
			return false;

		return player.level().getBiome(player.blockPosition()).is(PaganCharmTags.UNIQUE_PAGAN_BIOMES);
	}

	public static boolean isInNetherBiome(Player player) {
		if (!player.level().hasChunkAt(player.blockPosition()))
			return false;

		return player.level().getBiome(player.blockPosition()).is(PaganCharmTags.NETHER_BIOMES);
	}

	public static boolean isInForbiddenFluid(Player player) {
		return player.isInWater()
				|| player.isInLava()
				|| player.getFluidHeight(FluidTags.WATER) > 0.0D
				|| player.getFluidHeight(FluidTags.LAVA) > 0.0D;
	}
}
