package net.timothaty.timothatystrinkets.client.gorge;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(
		modid = TimothatysTrinketsMod.MODID,
		value = Dist.CLIENT
)
public final class GorgeCameraShakeHandler {
	private static final float[] PITCH_POINTS =
			{0.0F, 1.0F, -0.42F, 0.16F, 0.0F};
	private static final float[] YAW_POINTS =
			{0.0F, 0.72F, -0.50F, 0.18F, 0.0F};
	private static final float[] ROLL_POINTS =
			{0.0F, 0.68F, -0.46F, 0.16F, 0.0F};

	private static ClientLevel trackedLevel;
	private static int startTick = -1;
	private static int durationTicks = 1;
	private static float pitchAmplitude;
	private static float yawAmplitude;
	private static float rollAmplitude;

	private GorgeCameraShakeHandler() {
	}

	public static void start(long seed) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		ClientLevel level = minecraft.level;
		if (player == null || level == null)
			return;

		RandomSource random = RandomSource.create(seed);
		trackedLevel = level;
		startTick = player.tickCount;
		durationTicks = 5 + random.nextInt(2);
		pitchAmplitude = randomBetween(random, 0.55F, 0.85F);
		yawAmplitude = randomBetween(random, 0.15F, 0.30F)
				* randomSign(random);
		rollAmplitude = randomBetween(random, 0.10F, 0.20F)
				* randomSign(random);
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

		float progress = age / durationTicks;
		if (progress >= 1.0F) {
			clear();
			return;
		}

		float envelope = 1.0F - smoothstep(progress);
		event.setPitch(
				event.getPitch()
						+ sampleCatmullRom(PITCH_POINTS, progress)
						* pitchAmplitude
						* envelope
		);
		event.setYaw(
				event.getYaw()
						+ sampleCatmullRom(YAW_POINTS, progress)
						* yawAmplitude
						* envelope
		);
		event.setRoll(
				event.getRoll()
						+ sampleCatmullRom(ROLL_POINTS, progress)
						* rollAmplitude
						* envelope
		);
	}

	public static void clear() {
		trackedLevel = null;
		startTick = -1;
		durationTicks = 1;
		pitchAmplitude = 0.0F;
		yawAmplitude = 0.0F;
		rollAmplitude = 0.0F;
	}

	private static float sampleCatmullRom(float[] points, float progress) {
		float clamped = clamp01(progress);
		int segmentCount = points.length - 1;
		float scaled = clamped * segmentCount;
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

	private static float randomBetween(
			RandomSource random,
			float minimum,
			float maximum
	) {
		return minimum + random.nextFloat() * (maximum - minimum);
	}

	private static float randomSign(RandomSource random) {
		return random.nextBoolean() ? 1.0F : -1.0F;
	}

	private static float clamp01(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}
}
