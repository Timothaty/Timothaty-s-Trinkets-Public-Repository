package net.timothaty.timothatystrinkets.client.vfx.soul_rip;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;

@OnlyIn(Dist.CLIENT)
public final class SoulRipTrailHandler {
	private static final int MAX_TRAILS = 256;
	private static final List<SoulRipTrail> TRAILS = new ArrayList<>();
	private static final List<SoulRipTrail> TRAILS_VIEW = Collections.unmodifiableList(TRAILS);
	private static final Random RANDOM = new Random();

	private SoulRipTrailHandler() {
	}

	public static void spawn(double x, double y, double z, float width, float height, boolean empowered) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return;

		int count = empowered ? 4 : 2;
		float baseTrailWidth = empowered ? 0.060F : 0.045F;
		float baseAlpha = empowered ? 0.58F : 0.44F;
		for (int i = 0; i < count; i++) {
			Vec3 start = randomBodyPoint(x, y, z, width, height);
			Vec3 lateral = randomHorizontalVector(0.20D + RANDOM.nextDouble() * 0.26D);
			double lift = height * (0.62D + RANDOM.nextDouble() * 0.42D);
			Vec3 control = start.add(lateral.scale(0.34D)).add(0.0D, lift * 0.50D, 0.0D);
			Vec3 end = start.add(lateral).add(0.0D, lift, 0.0D);
			int lifetime = 11 + RANDOM.nextInt(empowered ? 7 : 5);
			float trailWidth = baseTrailWidth * (0.72F + RANDOM.nextFloat() * 0.34F);
			float phase = RANDOM.nextFloat() * 6.2831855F;
			float turnRate = (RANDOM.nextBoolean() ? 1.0F : -1.0F) * (0.045F + RANDOM.nextFloat() * 0.035F);
			double curveRadius = (empowered ? 0.070D : 0.050D) + RANDOM.nextDouble() * 0.035D;
			TRAILS.add(new SoulRipTrail(start, control, end, lifetime, trailWidth, baseAlpha, phase, turnRate, curveRadius));
			trimTrails();
		}
	}

	public static List<SoulRipTrail> trails() {
		return TRAILS_VIEW;
	}

	public static void tick() {
		TRAILS.removeIf(trail -> !trail.tick());
	}

	public static void clear() {
		TRAILS.clear();
	}

	private static void trimTrails() {
		while (TRAILS.size() > MAX_TRAILS) {
			TRAILS.remove(0);
		}
	}

	private static Vec3 randomBodyPoint(double x, double y, double z, float width, float height) {
		double radius = Math.max(0.10D, width * 0.34D);
		double angle = RANDOM.nextDouble() * Math.PI * 2.0D;
		double distance = radius * Math.sqrt(RANDOM.nextDouble());
		double offsetX = Math.cos(angle) * distance;
		double offsetZ = Math.sin(angle) * distance;
		double offsetY = height * (0.20D + RANDOM.nextDouble() * 0.52D);
		return new Vec3(x + offsetX, y + offsetY, z + offsetZ);
	}

	private static Vec3 randomHorizontalVector(double length) {
		double angle = RANDOM.nextDouble() * Math.PI * 2.0D;
		return new Vec3(Math.cos(angle) * length, 0.0D, Math.sin(angle) * length);
	}
}
