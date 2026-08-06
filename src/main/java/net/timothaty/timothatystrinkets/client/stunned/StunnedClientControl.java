package net.timothaty.timothatystrinkets.client.stunned;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class StunnedClientControl {
	private static final float FOV_IN_STEP = 1.0F / 9.0F;
	private static final float FOV_OUT_STEP = 1.0F / 14.0F;
	private static final double FOV_REDUCTION = 0.16D;
	private static boolean cameraLockInitialized;
	private static float lockedYaw;
	private static float lockedPitch;
	private static float fovBlend;
	private static float fovBlendOld;
	private static ClientLevel trackedLevel;
	private static LocalPlayer trackedPlayer;

	private StunnedClientControl() {
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!synchronizeContext(minecraft))
			return;
		LocalPlayer player = minecraft.player;

		boolean stunned = shouldControlClient(player);
		fovBlendOld = fovBlend;
		fovBlend = Mth.approach(fovBlend, stunned ? 1.0F : 0.0F, stunned ? FOV_IN_STEP : FOV_OUT_STEP);
		if (!stunned) {
			resetCameraLock();
			return;
		}

		initializeCameraLock(player.getYRot(), player.getXRot());

		closeForbiddenScreens(minecraft);
		lockCamera(player);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!synchronizeContext(minecraft))
			return;
		LocalPlayer player = minecraft.player;
		if (!shouldControlClient(player)) {
			resetCameraLock();
			return;
		}

		initializeCameraLock(event.getYaw(), event.getPitch());
		event.setYaw(lockedYaw);
		event.setPitch(lockedPitch);
	}

	@SubscribeEvent
	public static void onComputeFov(ViewportEvent.ComputeFov event) {
		if (!synchronizeContext(Minecraft.getInstance()))
			return;
		float blend = Mth.lerp((float) event.getPartialTick(), fovBlendOld, fovBlend);
		if (blend <= 0.001F)
			return;

		double eased = Mth.smoothstep(Mth.clamp(blend, 0.0F, 1.0F));
		event.setFOV(event.getFOV() * (1.0D - FOV_REDUCTION * eased));
	}

	private static boolean shouldControlClient(LocalPlayer player) {
		return player.hasEffect(TimothatysTrinketsModMobEffects.STUNNED) && !player.isCreative() && !player.isSpectator();
	}

	public static void clear() {
		cameraLockInitialized = false;
		lockedYaw = 0.0F;
		lockedPitch = 0.0F;
		fovBlend = 0.0F;
		fovBlendOld = 0.0F;
		trackedLevel = null;
		trackedPlayer = null;
	}

	private static void closeForbiddenScreens(Minecraft minecraft) {
		if (minecraft.screen == null)
			return;
		if (minecraft.screen instanceof ChatScreen || minecraft.screen instanceof PauseScreen)
			return;

		minecraft.setScreen(null);
	}

	private static void lockCamera(LocalPlayer player) {
		if (!cameraLockInitialized)
			return;
		player.setYRot(lockedYaw);
		player.setXRot(lockedPitch);
		player.setYHeadRot(lockedYaw);
		player.yRotO = lockedYaw;
		player.xRotO = lockedPitch;
		player.yHeadRotO = lockedYaw;
		player.yBodyRot = lockedYaw;
		player.yBodyRotO = lockedYaw;
	}

	private static void initializeCameraLock(float yaw, float pitch) {
		if (cameraLockInitialized)
			return;
		lockedYaw = yaw;
		lockedPitch = pitch;
		cameraLockInitialized = true;
	}

	private static void resetCameraLock() {
		cameraLockInitialized = false;
		lockedYaw = 0.0F;
		lockedPitch = 0.0F;
	}

	private static boolean synchronizeContext(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		LocalPlayer player = minecraft.player;
		if (level == null || player == null
				|| !player.isAlive() || player.isDeadOrDying() || player.isRemoved()) {
			clear();
			return false;
		}
		if (level != trackedLevel || player != trackedPlayer) {
			clear();
			trackedLevel = level;
			trackedPlayer = player;
		}
		return true;
	}
}
