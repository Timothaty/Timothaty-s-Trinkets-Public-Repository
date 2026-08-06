package net.timothaty.timothatystrinkets.client.gorge;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.compat.FirstPersonModelCompat;
import net.timothaty.timothatystrinkets.client.stunned.StunnedClientAnimationState;
import net.timothaty.timothatystrinkets.client.wrath_of_the_wicked.WrathOfTheWickedClientState;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(
		modid = TimothatysTrinketsMod.MODID,
		value = Dist.CLIENT
)
public final class GorgeFirstPersonAnimation {
	public static final int ITEM_SWING_DURATION_TICKS = 6;
	public static final int BARE_HAND_DURATION_TICKS =
			ITEM_SWING_DURATION_TICKS;

	private static State state;

	private GorgeFirstPersonAnimation() {
	}

	public static void start(int consumerEntityId) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		ClientLevel level = minecraft.level;
		if (player == null
				|| level == null
				|| player.getId() != consumerEntityId
				|| !player.isAlive()
				|| player.isDeadOrDying()
				|| player.isRemoved()
				|| minecraft.options.getCameraType()
						!= CameraType.FIRST_PERSON
				|| FirstPersonModelCompat.isTrueFirstPersonActive()
				|| StunnedClientAnimationState.isStunned(player)
				|| WrathOfTheWickedClientState.isActive(player)) {
			return;
		}

		ItemStack mainHandSnapshot = player.getMainHandItem().copy();
		boolean bareHand = mainHandSnapshot.isEmpty();
		state = new State(
				level,
				player,
				player.tickCount,
				bareHand
						? BARE_HAND_DURATION_TICKS
						: ITEM_SWING_DURATION_TICKS,
				player.getMainArm(),
				bareHand,
				mainHandSnapshot
		);
	}

	public static boolean isActive() {
		return state != null && validateState(false);
	}

	public static VisualState sample(float partialTick) {
		if (!validateState(false))
			return null;
		float elapsedTicks = state.player.tickCount
				- state.startTick
				+ partialTick;
		float progress = Mth.clamp(
				elapsedTicks / state.durationTicks,
				0.0F,
				1.0F
		);
		return new VisualState(
				elapsedTicks,
				progress,
				state.mainArm,
				state.bareHand,
				state.itemSnapshot
		);
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		validateState(true);
	}

	public static void clear() {
		state = null;
	}

	private static boolean validateState(boolean clearInvalid) {
		if (state == null)
			return false;

		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer currentPlayer = minecraft.player;
		boolean valid = minecraft.level == state.level
				&& currentPlayer == state.player
				&& currentPlayer != null
				&& currentPlayer.isAlive()
				&& !currentPlayer.isDeadOrDying()
				&& !currentPlayer.isRemoved()
				&& minecraft.options.getCameraType()
						== CameraType.FIRST_PERSON
				&& !FirstPersonModelCompat.isTrueFirstPersonActive()
				&& !StunnedClientAnimationState.isStunned(currentPlayer)
				&& !WrathOfTheWickedClientState.isActive(currentPlayer)
				&& currentPlayer.tickCount - state.startTick
						< state.durationTicks;
		if (!valid && clearInvalid)
			clear();
		return valid;
	}

	public record VisualState(
			float elapsedTicks,
			float progress,
			HumanoidArm mainArm,
			boolean bareHand,
			ItemStack itemSnapshot
	) {
	}

	private record State(
			ClientLevel level,
			LocalPlayer player,
			int startTick,
			int durationTicks,
			HumanoidArm mainArm,
			boolean bareHand,
			ItemStack itemSnapshot
	) {
	}
}
