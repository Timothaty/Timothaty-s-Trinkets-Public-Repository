package net.timothaty.timothatystrinkets.client.vfx.debtlord_finger;

import net.timothaty.timothatystrinkets.entity.DebtlordEntity;

import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public final class FingerOfDeathLaserHandler {
	private static final int MAX_STALE_TICKS = 3;
	private static final Map<DebtlordEntity, LaserVisual> ACTIVE_LASERS = new WeakHashMap<>();

	private FingerOfDeathLaserHandler() {
	}

	public static void record(DebtlordEntity entity, Vec3 source, Vec3 end, float partialTick) {
		if (entity == null || !entity.isAlive() || !entity.isFingerOfDeathIdleAnimationActive())
			return;

		LaserVisual visual = ACTIVE_LASERS.computeIfAbsent(entity, LaserVisual::new);
		visual.source = source;
		visual.end = end;
		visual.partialTick = partialTick;
		visual.staleTicks = 0;
	}

	public static void tick() {
		Iterator<Map.Entry<DebtlordEntity, LaserVisual>> iterator = ACTIVE_LASERS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<DebtlordEntity, LaserVisual> entry = iterator.next();
			DebtlordEntity entity = entry.getKey();
			LaserVisual visual = entry.getValue();
			visual.staleTicks++;
			if (entity == null || !entity.isAlive() || !entity.isFingerOfDeathIdleAnimationActive() || visual.staleTicks > MAX_STALE_TICKS)
				iterator.remove();
		}
	}

	public static Collection<LaserVisual> activeLasers() {
		return ACTIVE_LASERS.values();
	}

	public static final class LaserVisual {
		private final DebtlordEntity entity;
		private Vec3 source = Vec3.ZERO;
		private Vec3 end = Vec3.ZERO;
		private float partialTick;
		private int staleTicks;

		private LaserVisual(DebtlordEntity entity) {
			this.entity = entity;
		}

		public DebtlordEntity entity() {
			return entity;
		}

		public Vec3 source() {
			return source;
		}

		public Vec3 end() {
			return end;
		}

		public float partialTick() {
			return partialTick;
		}
	}
}
