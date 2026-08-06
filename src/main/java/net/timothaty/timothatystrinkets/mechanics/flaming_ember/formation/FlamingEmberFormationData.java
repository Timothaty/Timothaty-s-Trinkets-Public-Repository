package net.timothaty.timothatystrinkets.mechanics.flaming_ember.formation;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class FlamingEmberFormationData {
	public static final int DURATION_TICKS = 400;
	public static final int ENVIRONMENT_CHECK_INTERVAL_TICKS = 5;
	public static final int ENVIRONMENT_GRACE_TICKS = 10;
	public static final int VISUAL_UPDATE_INTERVAL_TICKS = 4;
	public static final int HEARTBEAT_FRESHNESS_TICKS = 20;
	public static final int LAVA_SEARCH_RADIUS = 4;
	public static final int REQUIRED_LAVA_SOURCES = 10;
	public static final double SUCCESS_CHANCE = 0.70D;

	public static final String FORMATION_TOKEN = "timothatys_trinkets_flaming_ember_formation_token";
	public static final String FORMATION_PROGRESS = "timothatys_trinkets_flaming_ember_formation_progress";
	public static final String FORMATION_HEARTBEAT = "timothatys_trinkets_flaming_ember_formation_heartbeat";
	private static final String CLIENT_VISUAL_STATES = "tt_flaming_ember_formation_client_visual_states";

	private FlamingEmberFormationData() {
	}

	@SuppressWarnings("deprecation")
	public static String getToken(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return "";

		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
		return tag.contains(FORMATION_TOKEN, Tag.TAG_STRING) ? tag.getString(FORMATION_TOKEN) : "";
	}

	public static boolean matchesToken(ItemStack stack, String token) {
		return token != null && !token.isEmpty() && token.equals(getToken(stack));
	}

	public static void writeToken(ItemStack stack, String token) {
		if (stack == null || stack.isEmpty() || token == null || token.isEmpty())
			return;

		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		tag.putString(FORMATION_TOKEN, token);
		tag.remove(FORMATION_PROGRESS);
		tag.remove(FORMATION_HEARTBEAT);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}

	public static void receiveVisualState(Player viewingPlayer, String token, int progress, boolean active) {
		if (viewingPlayer == null || token == null || token.isEmpty())
			return;

		CompoundTag playerData = viewingPlayer.getPersistentData();
		CompoundTag visualStates = playerData.getCompound(CLIENT_VISUAL_STATES);
		if (!active) {
			visualStates.remove(token);
		} else {
			CompoundTag visualState = new CompoundTag();
			visualState.putInt(FORMATION_PROGRESS, Mth.clamp(progress, 0, DURATION_TICKS));
			visualState.putLong(FORMATION_HEARTBEAT, viewingPlayer.level().getGameTime());
			visualStates.put(token, visualState);
		}

		if (visualStates.isEmpty()) {
			playerData.remove(CLIENT_VISUAL_STATES);
		} else {
			playerData.put(CLIENT_VISUAL_STATES, visualStates);
		}
	}

	public static int getFreshVisualProgress(Player viewingPlayer, ItemStack stack, long currentGameTime) {
		if (viewingPlayer == null)
			return -1;

		String token = getToken(stack);
		if (token.isEmpty())
			return -1;

		CompoundTag visualStates = viewingPlayer.getPersistentData().getCompound(CLIENT_VISUAL_STATES);
		if (!visualStates.contains(token, Tag.TAG_COMPOUND))
			return -1;

		CompoundTag visualState = visualStates.getCompound(token);
		if (!visualState.contains(FORMATION_HEARTBEAT, Tag.TAG_LONG))
			return -1;

		long age = currentGameTime - visualState.getLong(FORMATION_HEARTBEAT);
		if (age < -VISUAL_UPDATE_INTERVAL_TICKS || age > HEARTBEAT_FRESHNESS_TICKS)
			return -1;
		return Mth.clamp(visualState.getInt(FORMATION_PROGRESS), 0, DURATION_TICKS);
	}

	public static boolean hasFormationData(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return false;

		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		return tag.contains(FORMATION_TOKEN) || tag.contains(FORMATION_PROGRESS) || tag.contains(FORMATION_HEARTBEAT);
	}

	public static void clear(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return;

		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		if (!tag.contains(FORMATION_TOKEN) && !tag.contains(FORMATION_PROGRESS) && !tag.contains(FORMATION_HEARTBEAT))
			return;

		tag.remove(FORMATION_TOKEN);
		tag.remove(FORMATION_PROGRESS);
		tag.remove(FORMATION_HEARTBEAT);
		if (tag.isEmpty()) {
			stack.remove(DataComponents.CUSTOM_DATA);
		} else {
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		}
	}
}
