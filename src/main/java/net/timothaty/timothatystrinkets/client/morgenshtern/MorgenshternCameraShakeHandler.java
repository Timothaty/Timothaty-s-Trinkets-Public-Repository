package net.timothaty.timothatystrinkets.client.morgenshtern;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

@EventBusSubscriber(
		modid = TimothatysTrinketsMod.MODID,
		value = Dist.CLIENT
)
public final class MorgenshternCameraShakeHandler {
	private static final int DURATION_TICKS = 7;
	private static final float PITCH_AMPLITUDE = 2.15F;
	private static final float YAW_AMPLITUDE = 0.72F;
	private static final float ROLL_AMPLITUDE = 0.92F;

	private static final float[] PITCH_POINTS = {
		0.0F,
		1.0F,
		-0.58F,
		0.29F,
		-0.11F,
		0.03F,
		0.0F
	};
	private static final float[] YAW_POINTS = {
		0.0F,
		0.66F,
		-0.47F,
		0.23F,
		-0.09F,
		0.02F,
		0.0F
	};
	private static final float[] ROLL_POINTS = {
		0.0F,
		-0.82F,
		0.56F,
		-0.27F,
		0.10F,
		-0.02F,
		0.0F
	};

	private static ClientLevel trackedLevel;
	private static int startTick = -1;
	private static float directionSign = 1.0F;

	private MorgenshternCameraShakeHandler() {
	}

	public static void start(int attackerEntityId, int targetEntityId) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		ClientLevel level = minecraft.level;
		if (player == null
				|| level == null
				|| player.getId() != attackerEntityId)
			return;

		trackedLevel = level;
		startTick = player.tickCount;
		directionSign = ((attackerEntityId * 31 + targetEntityId) & 1) == 0
				? 1.0F
				: -1.0F;
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onComputeCameraAngles(
			ViewportEvent.ComputeCameraAngles event
	) {
		if (startTick < 0)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		ClientLevel level = minecraft.level;
		if (player == null
				|| level == null
				|| trackedLevel != level
				|| event.getCamera().getEntity() != player) {
			clear();
			return;
		}

		float age = player.tickCount - startTick
				+ (float) event.getPartialTick();
		if (age < 0.0F)
			return;

		float progress = age / DURATION_TICKS;
		if (progress >= 1.0F) {
			clear();
			return;
		}

		float envelope = 1.0F - smoothstep(progress);
		event.setPitch(
				event.getPitch()
						+ sampleCatmullRom(PITCH_POINTS, progress)
						* PITCH_AMPLITUDE
						* envelope
		);
		event.setYaw(
				event.getYaw()
						+ sampleCatmullRom(YAW_POINTS, progress)
						* YAW_AMPLITUDE
						* envelope
						* directionSign
		);
		event.setRoll(
				event.getRoll()
						+ sampleCatmullRom(ROLL_POINTS, progress)
						* ROLL_AMPLITUDE
						* envelope
						* directionSign
		);
	}

	public static void clear() {
		trackedLevel = null;
		startTick = -1;
		directionSign = 1.0F;
	}

	private static float sampleCatmullRom(
			float[] points,
			float progress
	) {
		float clamped = clamp01(progress);
		int segmentCount = points.length - 1;
		float scaled = clamped * segmentCount;
		int p1Index = Math.min(
				(int) Math.floor(scaled),
				segmentCount - 1
		);
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
						+ (2.0F * p0
								- 5.0F * p1
								+ 4.0F * p2
								- p3) * t2
						+ (-p0
								+ 3.0F * p1
								- 3.0F * p2
								+ p3) * t3
		);
	}

	private static float smoothstep(float value) {
		float clamped = clamp01(value);
		return clamped * clamped * (3.0F - 2.0F * clamped);
	}

	private static float clamp01(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}
}
