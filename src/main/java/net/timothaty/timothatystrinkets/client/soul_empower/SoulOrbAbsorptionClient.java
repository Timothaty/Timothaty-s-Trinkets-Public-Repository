package net.timothaty.timothatystrinkets.client.soul_empower;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.DuelistGuardClient;
import net.timothaty.timothatystrinkets.client.gorge.GorgeFirstPersonAnimation;
import net.timothaty.timothatystrinkets.entity.SoulOrbEntity;
import net.timothaty.timothatystrinkets.mechanics.armlet_gauntlet_synergy.ArmletGauntletSynergyData;
import net.timothaty.timothatystrinkets.mechanics.armlet_gauntlet_synergy.ArmletGauntletSynergyHelper;
import net.timothaty.timothatystrinkets.mechanics.armlet_gauntlet_synergy.ArmletGauntletSynergyState;
import net.timothaty.timothatystrinkets.mechanics.armlet_gauntlet_synergy.SoulEmpowerHelper;
import net.timothaty.timothatystrinkets.network.SoulOrbAbsorptionStateMessage;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class SoulOrbAbsorptionClient {
	private static final int EQUIPMENT_REVALIDATION_INTERVAL_TICKS = 5;
	private static final int VIEW_REVALIDATION_INTERVAL_TICKS = 3;
	private static boolean channeling;
	private static boolean lastSentHolding;
	private static int trackedOrbId = -1;
	private static HumanoidArm activeArm;
	private static InteractionHand activeHand;
	private static HumanoidArm visualArm;
	private static InteractionHand visualHand;
	private static int cachedEquipmentRevision = -1;
	private static int cachedGauntletSlot = -1;
	private static int lastEquipmentRevalidationTick;
	private static int lastViewRevalidationTick;
	private static boolean lastViewValid;
	private static ClientLevel channelLevel;
	private static LocalPlayer channelPlayer;

	private SoulOrbAbsorptionClient() {
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		SoulOrbAbsorptionVisualState.clientTick(minecraft.level);
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null) {
			resetLocalState();
			SoulOrbAbsorptionFirstPersonAnimation.reset();
			return;
		}

		if (channeling && (minecraft.level != channelLevel || player != channelPlayer)) {
			stopChanneling();
			clearVisualState();
			SoulOrbAbsorptionFirstPersonAnimation.reset();
		}
		if (channeling && !canContinue(minecraft, player)) {
			stopChanneling();
		}
		HumanoidArm pullingArm = SoulOrbAbsorptionVisualState.getPullingArm(player);
		boolean pulling = visualArm != null && pullingArm == visualArm && SoulOrbAbsorptionVisualState.isPulling(player);
		SoulOrbAbsorptionFirstPersonAnimation.tick(channeling, pulling);
		if (!channeling && !SoulOrbAbsorptionFirstPersonAnimation.isVisible()) {
			clearVisualState();
		}
	}

	@SubscribeEvent
	public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		ArmletGauntletSynergyState.clearClientStates();
		resetLocalState();
		SoulOrbAbsorptionFirstPersonAnimation.reset();
	}

	@SubscribeEvent
	public static void onPlayerClone(ClientPlayerNetworkEvent.Clone event) {
		ArmletGauntletSynergyState.clear(event.getOldPlayer());
		resetLocalState();
		SoulOrbAbsorptionFirstPersonAnimation.reset();
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
		if (!event.isUseItem()) {
			return;
		}

		if (channeling || tryStartChanneling()) {
			event.setSwingHand(false);
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (channeling && minecraft.options.keyUse.matchesMouse(event.getButton()) && event.getAction() == GLFW.GLFW_RELEASE) {
			stopChanneling();
		}
	}

	@SubscribeEvent
	public static void onKey(InputEvent.Key event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (channeling && minecraft.options.keyUse.matches(event.getKey(), event.getScanCode()) && event.getAction() == GLFW.GLFW_RELEASE) {
			stopChanneling();
		}
	}

	public static boolean isVisuallyChanneling() {
		return channeling;
	}

	public static boolean shouldTransformArm(HumanoidArm arm) {
		LocalPlayer player = Minecraft.getInstance().player;
		return SoulOrbAbsorptionFirstPersonAnimation.isVisible()
				&& !GorgeFirstPersonAnimation.isActive()
				&& player != null && arm == visualArm && visualHand != null && player.getItemInHand(visualHand).isEmpty();
	}

	private static boolean tryStartChanneling() {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (!passesCheapLocalChecks(minecraft, player)) {
			return false;
		}

		ArmletGauntletSynergyState.Snapshot snapshot = ArmletGauntletSynergyState.getOrRefresh(player);
		if (!isUsableSnapshot(player, snapshot)) {
			return false;
		}

		SoulOrbEntity orb = ArmletGauntletSynergyHelper.findBestAvailableSoulOrbWithMinimumDot(player, ArmletGauntletSynergyData.ACQUIRE_MIN_DOT);
		if (orb == null) {
			return false;
		}

		channeling = true;
		trackedOrbId = orb.getId();
		activeHand = snapshot.interactionHand();
		activeArm = snapshot.physicalArm();
		visualHand = activeHand;
		visualArm = activeArm;
		cachedEquipmentRevision = snapshot.revision();
		cachedGauntletSlot = snapshot.gauntletSlot();
		lastEquipmentRevalidationTick = player.tickCount;
		lastViewRevalidationTick = player.tickCount;
		lastViewValid = true;
		channelLevel = minecraft.level;
		channelPlayer = player;
		sendHoldingState(true);
		return true;
	}

	private static boolean passesCheapLocalChecks(Minecraft minecraft, LocalPlayer player) {
		if (minecraft.screen != null || player == null || !player.isAlive() || player.isDeadOrDying() || player.isRemoved()
				|| player.isSpectator() || player.isSleeping() || player.isUsingItem() || DuelistGuardClient.isVisuallyGuarding()
				|| SoulEmpowerHelper.getLevel(player) >= ArmletGauntletSynergyData.MAX_SOUL_EMPOWER_LEVEL) {
			return false;
		}
		return true;
	}

	private static boolean canContinue(Minecraft minecraft, LocalPlayer player) {
		if (!minecraft.options.keyUse.isDown() || !passesCheapLocalChecks(minecraft, player) || activeHand == null || activeArm == null
				|| !player.getItemInHand(activeHand).isEmpty()) {
			return false;
		}

		ArmletGauntletSynergyState.Snapshot current = ArmletGauntletSynergyState.get(player);
		if (!matchesCachedEquipment(current)) {
			return false;
		}

		if (player.tickCount - lastEquipmentRevalidationTick >= EQUIPMENT_REVALIDATION_INTERVAL_TICKS) {
			lastEquipmentRevalidationTick = player.tickCount;
			ArmletGauntletSynergyState.Snapshot refreshed = ArmletGauntletSynergyState.refreshFromCurios(player);
			if (!matchesCachedEquipment(refreshed)) {
				return false;
			}
		}

		SoulOrbEntity orb = findTrackedOrb(minecraft.level);
		if (orb == null || !isWithinRange(player, orb)) {
			return false;
		}
		if (player.tickCount - lastViewRevalidationTick >= VIEW_REVALIDATION_INTERVAL_TICKS) {
			lastViewRevalidationTick = player.tickCount;
			lastViewValid = ArmletGauntletSynergyHelper.isSoulOrbInViewWithMinimumDot(player, orb, ArmletGauntletSynergyData.RETAIN_MIN_DOT);
		}
		return lastViewValid;
	}

	private static boolean isUsableSnapshot(LocalPlayer player, ArmletGauntletSynergyState.Snapshot snapshot) {
		return snapshot != null && snapshot.synergyActive() && snapshot.gauntletSlot() >= 0
				&& snapshot.interactionHand() != null && snapshot.physicalArm() != null
				&& player.getItemInHand(snapshot.interactionHand()).isEmpty();
	}

	private static boolean matchesCachedEquipment(ArmletGauntletSynergyState.Snapshot snapshot) {
		return snapshot != null && snapshot.synergyActive() && snapshot.revision() == cachedEquipmentRevision
				&& snapshot.gauntletSlot() == cachedGauntletSlot && snapshot.interactionHand() == activeHand
				&& snapshot.physicalArm() == activeArm;
	}

	private static SoulOrbEntity findTrackedOrb(ClientLevel level) {
		if (trackedOrbId < 0 || level == null) {
			return null;
		}
		Entity entity = level.getEntity(trackedOrbId);
		return entity instanceof SoulOrbEntity orb && !orb.isRemoved() ? orb : null;
	}

	private static boolean isWithinRange(LocalPlayer player, SoulOrbEntity orb) {
		double x = orb.getX() - player.getX();
		double y = orb.getY() + orb.getBbHeight() * 0.5D - player.getEyeY();
		double z = orb.getZ() - player.getZ();
		return x * x + y * y + z * z <= ArmletGauntletSynergyData.MAX_RANGE_SQR;
	}

	private static void stopChanneling() {
		if (channeling || lastSentHolding) {
			sendHoldingState(false);
		}
		channeling = false;
		trackedOrbId = -1;
		activeArm = null;
		activeHand = null;
		cachedEquipmentRevision = -1;
		cachedGauntletSlot = -1;
		lastEquipmentRevalidationTick = 0;
		lastViewRevalidationTick = 0;
		lastViewValid = false;
		channelLevel = null;
		channelPlayer = null;
	}

	private static void sendHoldingState(boolean holding) {
		if (holding != lastSentHolding) {
			PacketDistributor.sendToServer(new SoulOrbAbsorptionStateMessage(holding));
			lastSentHolding = holding;
		}
	}

	private static void resetLocalState() {
		channeling = false;
		lastSentHolding = false;
		trackedOrbId = -1;
		activeArm = null;
		activeHand = null;
		cachedEquipmentRevision = -1;
		cachedGauntletSlot = -1;
		lastEquipmentRevalidationTick = 0;
		lastViewRevalidationTick = 0;
		lastViewValid = false;
		channelLevel = null;
		channelPlayer = null;
		clearVisualState();
	}

	private static void clearVisualState() {
		visualArm = null;
		visualHand = null;
	}
}
