package net.timothaty.timothatystrinkets.client.handler;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.wrath_of_the_wicked.WrathOfTheWickedClientState;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@EventBusSubscriber(
		modid = TimothatysTrinketsMod.MODID,
		value = Dist.CLIENT
)
public final class WrathOfTheWickedCameraHandler {
	private static final float[] PITCH_POINTS = {0.0F, -0.72F, 0.48F, -0.20F, 0.0F};
	private static final float[] YAW_POINTS = {0.0F, 1.0F, -0.65F, 0.22F, 0.0F};
	private static final float[] ROLL_POINTS = {0.0F, -1.0F, 0.68F, -0.24F, 0.0F};
	private static final float MAX_PITCH_OFFSET = 1.0F;
	private static final float MAX_YAW_OFFSET = 1.4F;
	private static final float MAX_ROLL_OFFSET = 0.85F;

	private static final List<Impulse> IMPULSES = new ArrayList<>();
	private static ClientLevel trackedLevel;

	private WrathOfTheWickedCameraHandler() {
	}

	public static void startLaserImpulse() {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null
				|| minecraft.level == null
				|| !WrathOfTheWickedClientState.isActive(player)) {
			return;
		}
		if (trackedLevel != minecraft.level) {
			IMPULSES.clear();
			trackedLevel = minecraft.level;
		}

		ThreadLocalRandom random = ThreadLocalRandom.current();
		int duration = random.nextBoolean() ? 4 : 5;
		float pitch = randomBetween(random, 0.20F, 0.40F);
		float yaw = randomBetween(random, 0.30F, 0.60F) * randomSign(random);
		float roll = randomBetween(random, 0.15F, 0.35F) * randomSign(random);
		IMPULSES.add(new Impulse(player.tickCount, duration, pitch, yaw, roll));
	}

	public static void clear() {
		IMPULSES.clear();
		trackedLevel = null;
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		ClientLevel level = minecraft.level;
		if (player == null || level == null || event.getCamera().getEntity() != player) {
			clear();
			return;
		}
		if (trackedLevel != level) {
			IMPULSES.clear();
			trackedLevel = level;
		}

		float partialTick = (float) event.getPartialTick();
		if (minecraft.options.getCameraType() == CameraType.FIRST_PERSON) {
			WrathOfTheWickedClientState.CameraPose pose =
					WrathOfTheWickedClientState.getCameraPose(player, partialTick);
			if (pose != null) {
				event.setYaw(pose.yaw());
				event.setPitch(pose.pitch());
			}
		}

		if (!WrathOfTheWickedClientState.isActive(player)) {
			IMPULSES.clear();
			return;
		}

		float pitchOffset = 0.0F;
		float yawOffset = 0.0F;
		float rollOffset = 0.0F;
		Iterator<Impulse> iterator = IMPULSES.iterator();
		while (iterator.hasNext()) {
			Impulse impulse = iterator.next();
			float age = player.tickCount - impulse.startTick + partialTick;
			if (age < 0.0F)
				continue;
			float progress = age / impulse.durationTicks;
			if (progress >= 1.0F) {
				iterator.remove();
				continue;
			}

			float envelope = 1.0F - smoothstep(progress);
			pitchOffset += sampleCatmullRom(PITCH_POINTS, progress)
					* impulse.pitchAmplitude * envelope;
			yawOffset += sampleCatmullRom(YAW_POINTS, progress)
					* impulse.yawAmplitude * envelope;
			rollOffset += sampleCatmullRom(ROLL_POINTS, progress)
					* impulse.rollAmplitude * envelope;
		}

		event.setPitch(event.getPitch() + clamp(pitchOffset, MAX_PITCH_OFFSET));
		event.setYaw(event.getYaw() + clamp(yawOffset, MAX_YAW_OFFSET));
		event.setRoll(event.getRoll() + clamp(rollOffset, MAX_ROLL_OFFSET));
	}

	private static float sampleCatmullRom(float[] points, float progress) {
		float clampedProgress = clamp01(progress);
		int segmentCount = points.length - 1;
		float scaled = clampedProgress * segmentCount;
		int p1Index = Math.min((int) Math.floor(scaled), segmentCount - 1);
		float localT = scaled - p1Index;
		float p0 = points[Math.max(0, p1Index - 1)];
		float p1 = points[p1Index];
		float p2 = points[Math.min(points.length - 1, p1Index + 1)];
		float p3 = points[Math.min(points.length - 1, p1Index + 2)];
		float t2 = localT * localT;
		float t3 = t2 * localT;
		return 0.5F * (
				2.0F * p1
						+ (-p0 + p2) * localT
						+ (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * t2
						+ (-p0 + 3.0F * p1 - 3.0F * p2 + p3) * t3
		);
	}

	private static float smoothstep(float value) {
		float clamped = clamp01(value);
		return clamped * clamped * (3.0F - 2.0F * clamped);
	}

	private static float randomBetween(ThreadLocalRandom random, float minimum, float maximum) {
		return minimum + random.nextFloat() * (maximum - minimum);
	}

	private static float randomSign(ThreadLocalRandom random) {
		return random.nextBoolean() ? 1.0F : -1.0F;
	}

	private static float clamp(float value, float absoluteMaximum) {
		return Math.max(-absoluteMaximum, Math.min(absoluteMaximum, value));
	}

	private static float clamp01(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}

	private record Impulse(
			int startTick,
			int durationTicks,
			float pitchAmplitude,
			float yawAmplitude,
			float rollAmplitude
	) {
	}
}
