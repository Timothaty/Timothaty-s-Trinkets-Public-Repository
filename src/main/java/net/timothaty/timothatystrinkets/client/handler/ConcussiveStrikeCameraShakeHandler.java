package net.timothaty.timothatystrinkets.client.handler;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

import net.minecraft.client.Minecraft;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class ConcussiveStrikeCameraShakeHandler {
	private static final float[] PITCH_POINTS = {0.0F, -0.65F, 0.35F, -0.18F, 0.08F, 0.0F};
	private static final float[] YAW_POINTS = {0.0F, 0.35F, -0.25F, 0.14F, -0.05F, 0.0F};
	private static final float[] ROLL_POINTS = {0.0F, -0.55F, 0.42F, -0.20F, 0.08F, 0.0F};
	private static final float[] STUNNED_PITCH_POINTS = {0.0F, -0.20F, 0.78F, -0.58F, 0.32F, -0.12F, 0.0F};
	private static final float[] STUNNED_YAW_POINTS = {0.0F, 0.55F, -0.72F, 0.44F, -0.20F, 0.08F, 0.0F};
	private static final float[] STUNNED_ROLL_POINTS = {0.0F, -0.70F, 0.62F, -0.38F, 0.16F, -0.06F, 0.0F};
	private static final float[] PARRY_PITCH_POINTS = {0.0F, -0.38F, 0.26F, -0.12F, 0.0F};
	private static final float[] PARRY_YAW_POINTS = {0.0F, 0.48F, -0.34F, 0.15F, 0.0F};
	private static final float[] PARRY_ROLL_POINTS = {0.0F, -0.42F, 0.30F, -0.10F, 0.0F};
	private static final float[] DUELIST_DIRECTION_PITCH_POINTS = {0.0F, -0.18F, 0.10F, 0.0F};
	private static final float[] DUELIST_DIRECTION_RIGHT_YAW_POINTS = {0.0F, 0.30F, -0.10F, 0.0F};
	private static final float[] DUELIST_DIRECTION_LEFT_YAW_POINTS = {0.0F, -0.30F, 0.10F, 0.0F};
	private static final float[] DUELIST_DIRECTION_RIGHT_ROLL_POINTS = {0.0F, -0.16F, 0.06F, 0.0F};
	private static final float[] DUELIST_DIRECTION_LEFT_ROLL_POINTS = {0.0F, 0.16F, -0.06F, 0.0F};

	private static int startTick = -1;
	private static int durationTicks = 1;
	private static float pitchAmplitude;
	private static float yawAmplitude;
	private static float rollAmplitude;
	private static float[] activePitchPoints = PITCH_POINTS;
	private static float[] activeYawPoints = YAW_POINTS;
	private static float[] activeRollPoints = ROLL_POINTS;

	private ConcussiveStrikeCameraShakeHandler() {
	}

	public static void startLightShake(int duration, float pitch, float yaw, float roll) {
		startShake(duration, pitch, yaw, roll, PITCH_POINTS, YAW_POINTS, ROLL_POINTS);
	}

	public static void startStunnedShake(int duration, float pitch, float yaw, float roll) {
		startShake(duration, pitch, yaw, roll, STUNNED_PITCH_POINTS, STUNNED_YAW_POINTS, STUNNED_ROLL_POINTS);
	}

	public static void startParryShake(int duration, float pitch, float yaw, float roll) {
		startShake(duration, pitch, yaw, roll, PARRY_PITCH_POINTS, PARRY_YAW_POINTS, PARRY_ROLL_POINTS);
	}

	public static void startDuelistDirectionShake(int directionSign) {
		boolean right = directionSign >= 0;
		startShake(6, 0.34F, 0.45F, 0.42F, DUELIST_DIRECTION_PITCH_POINTS, right ? DUELIST_DIRECTION_RIGHT_YAW_POINTS : DUELIST_DIRECTION_LEFT_YAW_POINTS, right ? DUELIST_DIRECTION_RIGHT_ROLL_POINTS : DUELIST_DIRECTION_LEFT_ROLL_POINTS);
	}

	public static void clear() {
		stopShake();
	}

	private static void startShake(int duration, float pitch, float yaw, float roll, float[] pitchPoints, float[] yawPoints, float[] rollPoints) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.player == null)
			return;

		startTick = minecraft.player.tickCount;
		durationTicks = Math.max(1, duration);
		pitchAmplitude = Math.max(0.0F, pitch);
		yawAmplitude = Math.max(0.0F, yaw);
		rollAmplitude = Math.max(0.0F, roll);
		activePitchPoints = pitchPoints;
		activeYawPoints = yawPoints;
		activeRollPoints = rollPoints;
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
		if (startTick < 0)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.player == null) {
			stopShake();
			return;
		}

		float age = (minecraft.player.tickCount - startTick) + (float) event.getPartialTick();
		if (age < 0.0F)
			return;

		float progress = age / (float) durationTicks;
		if (progress >= 1.0F) {
			stopShake();
			return;
		}

		float envelope = 1.0F - smoothStep(clamp01(progress));
		event.setPitch(event.getPitch() + sampleCatmullRom(activePitchPoints, progress) * pitchAmplitude * envelope);
		event.setYaw(event.getYaw() + sampleCatmullRom(activeYawPoints, progress) * yawAmplitude * envelope);
		event.setRoll(event.getRoll() + sampleCatmullRom(activeRollPoints, progress) * rollAmplitude * envelope);
	}

	private static void stopShake() {
		startTick = -1;
		pitchAmplitude = 0.0F;
		yawAmplitude = 0.0F;
		rollAmplitude = 0.0F;
		activePitchPoints = PITCH_POINTS;
		activeYawPoints = YAW_POINTS;
		activeRollPoints = ROLL_POINTS;
	}

	private static float sampleCatmullRom(float[] points, float progress) {
		if (points.length == 0)
			return 0.0F;
		if (points.length == 1)
			return points[0];

		float clampedProgress = clamp01(progress);
		int segmentCount = points.length - 1;
		float scaled = clampedProgress * segmentCount;
		int p1Index = Math.min((int) Math.floor(scaled), segmentCount - 1);
		float localT = scaled - p1Index;

		int p0Index = Math.max(0, p1Index - 1);
		int p2Index = Math.min(points.length - 1, p1Index + 1);
		int p3Index = Math.min(points.length - 1, p1Index + 2);

		float p0 = points[p0Index];
		float p1 = points[p1Index];
		float p2 = points[p2Index];
		float p3 = points[p3Index];

		float t2 = localT * localT;
		float t3 = t2 * localT;
		return 0.5F * (
				2.0F * p1
						+ (-p0 + p2) * localT
						+ (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * t2
						+ (-p0 + 3.0F * p1 - 3.0F * p2 + p3) * t3
		);
	}

	private static float smoothStep(float value) {
		float t = clamp01(value);
		return t * t * (3.0F - 2.0F * t);
	}

	private static float clamp01(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}
}
