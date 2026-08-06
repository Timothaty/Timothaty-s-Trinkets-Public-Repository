package net.timothaty.timothatystrinkets.client.vfx.debtlord_claws;

import net.timothaty.timothatystrinkets.entity.DebtlordEntity;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class DebtlordClawTrailHandler {
	public static final int FINGER_COUNT = 8;
	public static final int MAX_POINT_AGE = 8;
	private static final int MAX_POINTS_PER_FINGER = 10;
	private static final double TELEPORT_DISTANCE_SQR = 16.0D;
	private static final Map<DebtlordEntity, FingerTrails> ACTIVE_TRAILS = new WeakHashMap<>();

	private DebtlordClawTrailHandler() {
	}

	public static void record(DebtlordEntity entity, List<Vec3> fingerPositions) {
		if (fingerPositions.size() != FINGER_COUNT)
			return;

		FingerTrails trails = ACTIVE_TRAILS.computeIfAbsent(entity, ignored -> new FingerTrails());
		if (trails.lastRecordedTick == entity.tickCount)
			return;
		trails.lastRecordedTick = entity.tickCount;

		for (int i = 0; i < FINGER_COUNT; i++) {
			Deque<TrailPoint> points = trails.fingers.get(i);
			Vec3 position = fingerPositions.get(i);
			TrailPoint previous = points.peekLast();
			if (previous != null && previous.position.distanceToSqr(position) > TELEPORT_DISTANCE_SQR)
				points.clear();
			points.addLast(new TrailPoint(position));
			while (points.size() > MAX_POINTS_PER_FINGER)
				points.removeFirst();
		}
	}

	public static void tick() {
		Iterator<Map.Entry<DebtlordEntity, FingerTrails>> trailIterator = ACTIVE_TRAILS.entrySet().iterator();
		while (trailIterator.hasNext()) {
			Map.Entry<DebtlordEntity, FingerTrails> entry = trailIterator.next();
			FingerTrails trails = entry.getValue();
			boolean hasPoints = false;
			for (Deque<TrailPoint> points : trails.fingers) {
				points.forEach(point -> point.age++);
				while (!points.isEmpty() && points.peekFirst().age > MAX_POINT_AGE)
					points.removeFirst();
				hasPoints |= !points.isEmpty();
			}
			if (!hasPoints || entry.getKey().isRemoved())
				trailIterator.remove();
		}
	}

	public static Collection<FingerTrails> trails() {
		return Collections.unmodifiableCollection(ACTIVE_TRAILS.values());
	}

	public static final class FingerTrails {
		private final List<Deque<TrailPoint>> fingers = new ArrayList<>(FINGER_COUNT);
		private int lastRecordedTick = Integer.MIN_VALUE;

		private FingerTrails() {
			for (int i = 0; i < FINGER_COUNT; i++)
				fingers.add(new ArrayDeque<>());
		}

		public List<Deque<TrailPoint>> fingers() {
			return fingers;
		}
	}

	public static final class TrailPoint {
		private final Vec3 position;
		private int age;

		private TrailPoint(Vec3 position) {
			this.position = position;
		}

		public Vec3 position() {
			return position;
		}

		public int age() {
			return age;
		}
	}
}
