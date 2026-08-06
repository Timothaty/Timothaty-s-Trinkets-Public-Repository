package net.timothaty.timothatystrinkets.client.vfx.spark;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class SparkTrailHandler {
	public static final int MAX_POINT_AGE = 7;
	private static final int DEFAULT_RED = 255;
	private static final int DEFAULT_GREEN = 106;
	private static final int DEFAULT_BLUE = 0;
	private static final int MAX_POINTS = 12;
	private static final int MAX_TRAILS = 256;
	private static final double MIN_RECORD_DISTANCE_SQR = 0.0009D;
	private static final List<SparkTrail> TRAILS = new ArrayList<>();
	private static final List<SparkTrail> TRAILS_VIEW = Collections.unmodifiableList(TRAILS);

	private SparkTrailHandler() {
	}

	public static SparkTrail create(Vec3 position) {
		return create(position, DEFAULT_RED, DEFAULT_GREEN, DEFAULT_BLUE);
	}

	public static SparkTrail create(Vec3 position, int red, int green, int blue) {
		SparkTrail trail = new SparkTrail(position, red, green, blue);
		TRAILS.add(trail);
		trimTrails();
		return trail;
	}

	public static void tick() {
		Iterator<SparkTrail> iterator = TRAILS.iterator();
		while (iterator.hasNext()) {
			SparkTrail trail = iterator.next();
			if (!trail.tick()) {
				iterator.remove();
			}
		}
	}

	public static void discard(SparkTrail trail) {
		TRAILS.remove(trail);
	}

	public static void clear() {
		TRAILS.clear();
	}

	public static List<SparkTrail> trails() {
		return TRAILS_VIEW;
	}

	private static void trimTrails() {
		while (TRAILS.size() > MAX_TRAILS) {
			TRAILS.remove(0);
		}
	}

	public static final class SparkTrail {
		private final Deque<TrailPoint> points = new ArrayDeque<>();
		private final int red;
		private final int green;
		private final int blue;
		private boolean recording = true;

		private SparkTrail(Vec3 position, int red, int green, int blue) {
			this.red = red;
			this.green = green;
			this.blue = blue;
			this.points.addLast(new TrailPoint(position, position));
		}

		public void record(Vec3 previousPosition, Vec3 position) {
			if (!this.recording) {
				return;
			}

			TrailPoint previous = this.points.peekLast();
			if (previous != null && previous.position.distanceToSqr(position) < MIN_RECORD_DISTANCE_SQR) {
				previous.previousPosition = previous.position;
				previous.position = position;
				previous.age = 0;
				return;
			}

			this.points.addLast(new TrailPoint(previousPosition, position));
			while (this.points.size() > MAX_POINTS) {
				this.points.removeFirst();
			}
		}

		public void stopRecording() {
			this.recording = false;
		}

		private boolean tick() {
			Iterator<TrailPoint> iterator = this.points.iterator();
			while (iterator.hasNext()) {
				TrailPoint point = iterator.next();
				point.age++;
				if (point.age > MAX_POINT_AGE) {
					iterator.remove();
				}
			}
			return !this.points.isEmpty() && (this.recording || this.points.size() > 1);
		}

		public Deque<TrailPoint> points() {
			return this.points;
		}

		public int red() {
			return this.red;
		}

		public int green() {
			return this.green;
		}

		public int blue() {
			return this.blue;
		}
	}

	public static final class TrailPoint {
		private Vec3 previousPosition;
		private Vec3 position;
		private int age;

		private TrailPoint(Vec3 previousPosition, Vec3 position) {
			this.previousPosition = previousPosition;
			this.position = position;
		}

		public Vec3 position(float partialTick) {
			return this.previousPosition.lerp(this.position, partialTick);
		}

		public double interpolatedX(float partialTick) {
			return Mth.lerp(partialTick, this.previousPosition.x, this.position.x);
		}

		public double interpolatedY(float partialTick) {
			return Mth.lerp(partialTick, this.previousPosition.y, this.position.y);
		}

		public double interpolatedZ(float partialTick) {
			return Mth.lerp(partialTick, this.previousPosition.z, this.position.z);
		}

		public int age() {
			return this.age;
		}
	}
}
