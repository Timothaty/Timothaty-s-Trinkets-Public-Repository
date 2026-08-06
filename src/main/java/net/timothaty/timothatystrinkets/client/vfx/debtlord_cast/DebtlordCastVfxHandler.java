package net.timothaty.timothatystrinkets.client.vfx.debtlord_cast;

import net.timothaty.timothatystrinkets.entity.DebtlordEntity;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public final class DebtlordCastVfxHandler {
	public static final int DARK_TRAIL_MAX_AGE = 14;
	public static final int LASER_WARNING_TRAIL_MAX_AGE = 12;
	private static final int DARK_TRAIL_MAX_POINTS = 18;
	private static final int LASER_WARNING_TRAIL_MAX_POINTS = 15;
	private static final Map<DebtlordEntity, CastVfx> ACTIVE_VFX = new WeakHashMap<>();

	private DebtlordCastVfxHandler() {
	}

	public static void record(DebtlordEntity entity, Vec3 leftArmPosition, boolean chainWarning, boolean desolation,
			Vec3 laserWarningPosition, boolean laserWarning, float partialTick) {
		if (!chainWarning && !desolation && !laserWarning)
			return;

		CastVfx vfx = ACTIVE_VFX.computeIfAbsent(entity, ignored -> new CastVfx());
		vfx.leftArmPosition = leftArmPosition;
		vfx.bodyCenter = interpolatedBodyCenter(entity, partialTick);
		vfx.chainWarning = chainWarning;
		vfx.desolation = desolation;
		vfx.laserWarning = laserWarning;
		if (laserWarning)
			vfx.laserWarningPosition = laserWarningPosition;
		vfx.lastRecordedTick = entity.tickCount;

		if (desolation && vfx.lastDarkTrailTick != entity.tickCount) {
			vfx.darkTrail.addLast(new TrailPoint(leftArmPosition, 0));
			vfx.lastDarkTrailTick = entity.tickCount;
			while (vfx.darkTrail.size() > DARK_TRAIL_MAX_POINTS)
				vfx.darkTrail.removeFirst();
		}
		if (laserWarning && vfx.lastLaserWarningTrailTick != entity.tickCount) {
			vfx.laserWarningTrail.addLast(new TrailPoint(laserWarningPosition, 0));
			vfx.lastLaserWarningTrailTick = entity.tickCount;
			while (vfx.laserWarningTrail.size() > LASER_WARNING_TRAIL_MAX_POINTS)
				vfx.laserWarningTrail.removeFirst();
		}
	}

	public static void tick() {
		Iterator<Map.Entry<DebtlordEntity, CastVfx>> iterator = ACTIVE_VFX.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<DebtlordEntity, CastVfx> entry = iterator.next();
			DebtlordEntity entity = entry.getKey();
			CastVfx vfx = entry.getValue();
			vfx.tickTrail();
			if (entity.isRemoved() || entity.tickCount - vfx.lastRecordedTick > 2) {
				vfx.chainWarning = false;
				vfx.desolation = false;
				vfx.laserWarning = false;
				if (vfx.darkTrail.isEmpty() && vfx.laserWarningTrail.isEmpty())
					iterator.remove();
			}
		}
	}

	public static Collection<CastVfx> activeVfx() {
		return Collections.unmodifiableCollection(ACTIVE_VFX.values());
	}

	private static Vec3 interpolatedBodyCenter(DebtlordEntity entity, float partialTick) {
		double x = Mth.lerp(partialTick, entity.xo, entity.getX());
		double y = Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getBbHeight() * 0.48D;
		double z = Mth.lerp(partialTick, entity.zo, entity.getZ());
		return new Vec3(x, y, z);
	}

	public static final class CastVfx {
		private Vec3 leftArmPosition = Vec3.ZERO;
		private Vec3 bodyCenter = Vec3.ZERO;
		private Vec3 laserWarningPosition = Vec3.ZERO;
		private final Deque<TrailPoint> darkTrail = new ArrayDeque<>();
		private final Deque<TrailPoint> laserWarningTrail = new ArrayDeque<>();
		private boolean chainWarning;
		private boolean desolation;
		private boolean laserWarning;
		private int lastRecordedTick;
		private int lastDarkTrailTick = -1;
		private int lastLaserWarningTrailTick = -1;

		public Vec3 leftArmPosition() {
			return leftArmPosition;
		}

		public Vec3 bodyCenter() {
			return bodyCenter;
		}

		public Deque<TrailPoint> darkTrail() {
			return darkTrail;
		}

		public Vec3 laserWarningPosition() {
			return laserWarningPosition;
		}

		public Deque<TrailPoint> laserWarningTrail() {
			return laserWarningTrail;
		}

		public boolean chainWarning() {
			return chainWarning;
		}

		public boolean desolation() {
			return desolation;
		}

		public boolean laserWarning() {
			return laserWarning;
		}

		private void tickTrail() {
			tickTrail(darkTrail, DARK_TRAIL_MAX_AGE);
			tickTrail(laserWarningTrail, LASER_WARNING_TRAIL_MAX_AGE);
		}

		private void tickTrail(Deque<TrailPoint> trail, int maxAge) {
			Iterator<TrailPoint> iterator = trail.iterator();
			while (iterator.hasNext()) {
				TrailPoint point = iterator.next();
				point.tick();
				if (point.age() > maxAge)
					iterator.remove();
			}
		}
	}

	public static final class TrailPoint {
		private final Vec3 position;
		private int age;

		private TrailPoint(Vec3 position, int age) {
			this.position = position;
			this.age = age;
		}

		public Vec3 position() {
			return position;
		}

		public int age() {
			return age;
		}

		private void tick() {
			age++;
		}
	}
}
