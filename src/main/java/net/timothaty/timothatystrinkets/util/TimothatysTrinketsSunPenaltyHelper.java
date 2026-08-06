package net.timothaty.timothatystrinkets.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class TimothatysTrinketsSunPenaltyHelper {
	private TimothatysTrinketsSunPenaltyHelper() {
	}

	public static void apply(Player player, Settings settings) {
		if (player == null || settings == null)
			return;
		if (Math.floorMod(player.tickCount, settings.checkIntervalTicks()) != 0)
			return;
		applyPenalty(player, settings, true);
	}

	public static void applyNow(Player player, Settings settings) {
		applyPenalty(player, settings, false);
	}

	private static void applyPenalty(Player player, Settings settings, boolean damageHelmet) {
		if (player == null)
			return;
		if (settings == null)
			return;
		if (player.isCreative() || player.isSpectator())
			return;
		if (!shouldSunBurn(player))
			return;

		ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
		boolean hasHelmet = helmet != null && !helmet.isEmpty();
		if (!hasHelmet) {
			player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), settings.minFireTicks()));
			return;
		}

		if (damageHelmet && Math.floorMod(player.tickCount, settings.helmetDamageIntervalTicks()) == 0) {
			helmet.hurtAndBreak(settings.helmetDamage(), player, EquipmentSlot.HEAD);
		}
	}

	public static boolean shouldSunBurn(Player player) {
		Level level = player.level();
		if (!level.isDay())
			return false;
		if (player.isInWaterRainOrBubble())
			return false;
		BlockPos pos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
		return level.canSeeSkyFromBelowWater(pos);
	}

	public record Settings(
			int checkIntervalTicks,
			int helmetDamageIntervalTicks,
			int helmetDamage,
			int minFireTicks
	) {
		public Settings {
			if (checkIntervalTicks <= 0)
				throw new IllegalArgumentException("checkIntervalTicks must be positive");
			if (helmetDamageIntervalTicks <= 0)
				throw new IllegalArgumentException("helmetDamageIntervalTicks must be positive");
			if (helmetDamage <= 0)
				throw new IllegalArgumentException("helmetDamage must be positive");
			if (minFireTicks <= 0)
				throw new IllegalArgumentException("minFireTicks must be positive");
		}
	}
}
