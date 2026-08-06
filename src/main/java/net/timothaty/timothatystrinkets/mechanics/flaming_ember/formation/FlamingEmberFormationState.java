package net.timothaty.timothatystrinkets.mechanics.flaming_ember.formation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public final class FlamingEmberFormationState {
	private static final String ACTIVE = "tt_flaming_ember_formation_active";
	private static final String TOKEN = "tt_flaming_ember_formation_token";
	private static final String HAND = "tt_flaming_ember_formation_hand";
	private static final String PROGRESS = "tt_flaming_ember_formation_progress";
	private static final String LAST_VALID_ENVIRONMENT_TICK = "tt_flaming_ember_formation_last_valid_environment_tick";
	private static final String LAST_ENVIRONMENT_CHECK_TICK = "tt_flaming_ember_formation_last_environment_check_tick";
	private static final String CACHED_ENVIRONMENT_VALID = "tt_flaming_ember_formation_cached_environment_valid";

	private FlamingEmberFormationState() {
	}

	public static Snapshot get(Player player) {
		if (player == null)
			return Snapshot.inactive();

		CompoundTag data = player.getPersistentData();
		if (!data.getBoolean(ACTIVE))
			return Snapshot.inactive();

		return new Snapshot(
				true,
				data.getString(TOKEN),
				readHand(data.getString(HAND)),
				Mth.clamp(data.getInt(PROGRESS), 0, FlamingEmberFormationData.DURATION_TICKS),
				data.getLong(LAST_VALID_ENVIRONMENT_TICK),
				data.getLong(LAST_ENVIRONMENT_CHECK_TICK),
				data.getBoolean(CACHED_ENVIRONMENT_VALID)
		);
	}

	public static void start(Player player, String token, InteractionHand hand, long currentGameTime) {
		clear(player);
		CompoundTag data = player.getPersistentData();
		data.putBoolean(ACTIVE, true);
		data.putString(TOKEN, token);
		data.putString(HAND, hand.name());
		data.putInt(PROGRESS, 0);
		data.putLong(LAST_VALID_ENVIRONMENT_TICK, currentGameTime);
		data.putLong(LAST_ENVIRONMENT_CHECK_TICK, currentGameTime);
		data.putBoolean(CACHED_ENVIRONMENT_VALID, true);
	}

	public static int advanceProgress(Player player) {
		CompoundTag data = player.getPersistentData();
		int progress = Mth.clamp(data.getInt(PROGRESS) + 1, 0, FlamingEmberFormationData.DURATION_TICKS);
		data.putInt(PROGRESS, progress);
		return progress;
	}

	public static void updateEnvironment(Player player, boolean valid, long currentGameTime) {
		CompoundTag data = player.getPersistentData();
		data.putLong(LAST_ENVIRONMENT_CHECK_TICK, currentGameTime);
		data.putBoolean(CACHED_ENVIRONMENT_VALID, valid);
		if (valid)
			data.putLong(LAST_VALID_ENVIRONMENT_TICK, currentGameTime);
	}

	public static void clear(Player player) {
		if (player == null)
			return;

		CompoundTag data = player.getPersistentData();
		data.remove(ACTIVE);
		data.remove(TOKEN);
		data.remove(HAND);
		data.remove(PROGRESS);
		data.remove(LAST_VALID_ENVIRONMENT_TICK);
		data.remove(LAST_ENVIRONMENT_CHECK_TICK);
		data.remove(CACHED_ENVIRONMENT_VALID);
	}

	private static InteractionHand readHand(String value) {
		if (InteractionHand.MAIN_HAND.name().equals(value))
			return InteractionHand.MAIN_HAND;
		if (InteractionHand.OFF_HAND.name().equals(value))
			return InteractionHand.OFF_HAND;
		return null;
	}

	public record Snapshot(boolean active, String token, InteractionHand hand, int progress,
			long lastValidEnvironmentTick, long lastEnvironmentCheckTick, boolean environmentValid) {
		private static Snapshot inactive() {
			return new Snapshot(false, "", null, 0, 0L, 0L, false);
		}
	}
}
