package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.beatific_pallium;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;

public final class BeatificPalliumImpactGeometry {
	private static final double EPSILON = 1.0E-7D;
	private static final double ARROW_BACKTRACK_DISTANCE = 4.0D;

	private BeatificPalliumImpactGeometry() {
	}

	public static Result forMelee(LivingEntity target, LivingEntity attacker) {
		Vec3 center = visualCenter(target);
		Vec3 start = attacker.getEyePosition();
		Vec3 end;
		if (attacker instanceof Player) {
			double length = start.distanceTo(center) + BeatificPalliumData.SHELL_HALF_SIZE * 2.0D + 1.0D;
			end = start.add(attacker.getViewVector(1.0F).scale(length));
		} else {
			end = center;
		}

		Result exact = intersectWorldSegment(target, center, start, end);
		return exact != null ? exact : fallbackFromSource(target, center, start);
	}

	public static Result forArrow(LivingEntity target, AbstractArrow arrow) {
		Vec3 center = visualCenter(target);
		Vec3 current = arrow.position();
		Vec3 previous = new Vec3(arrow.xo, arrow.yo, arrow.zo);
		Result exact = intersectWorldSegment(target, center, previous, current);
		if (exact != null)
			return exact;

		Vec3 movement = arrow.getDeltaMovement();
		if (movement.lengthSqr() < EPSILON)
			movement = current.subtract(previous);
		if (movement.lengthSqr() >= EPSILON) {
			Vec3 extendedStart = current.subtract(movement.normalize().scale(ARROW_BACKTRACK_DISTANCE));
			exact = intersectWorldSegment(target, center, extendedStart, current);
			if (exact != null)
				return exact;
		}

		return projectToNearestFace(target, center, current, movement);
	}

	private static Result intersectWorldSegment(LivingEntity target, Vec3 center, Vec3 worldStart, Vec3 worldEnd) {
		Vec3 localStart = worldToLocal(target, center, worldStart);
		Vec3 localEnd = worldToLocal(target, center, worldEnd);
		Vec3 localHit = intersectLocalCube(localStart, localEnd);
		return localHit != null ? resultAtSurface(localHit) : null;
	}

	private static Vec3 intersectLocalCube(Vec3 start, Vec3 end) {
		double halfSize = BeatificPalliumData.SHELL_HALF_SIZE;
		double[] origin = {start.x, start.y, start.z};
		double[] direction = {end.x - start.x, end.y - start.y, end.z - start.z};
		double tEnter = Double.NEGATIVE_INFINITY;
		double tExit = Double.POSITIVE_INFINITY;

		for (int axis = 0; axis < 3; axis++) {
			if (Math.abs(direction[axis]) < EPSILON) {
				if (origin[axis] < -halfSize || origin[axis] > halfSize)
					return null;
				continue;
			}

			double near = (-halfSize - origin[axis]) / direction[axis];
			double far = (halfSize - origin[axis]) / direction[axis];
			if (near > far) {
				double swap = near;
				near = far;
				far = swap;
			}
			tEnter = Math.max(tEnter, near);
			tExit = Math.min(tExit, far);
			if (tEnter - tExit > EPSILON)
				return null;
		}

		boolean startsInside = isInside(start, halfSize);
		double hitTime = startsInside ? tExit : tEnter;
		if (!Double.isFinite(hitTime) || hitTime < -EPSILON || hitTime > 1.0D + EPSILON)
			return null;
		hitTime = Mth.clamp(hitTime, 0.0D, 1.0D);
		return start.add(end.subtract(start).scale(hitTime));
	}

	private static boolean isInside(Vec3 point, double halfSize) {
		return Math.abs(point.x) <= halfSize + EPSILON
				&& Math.abs(point.y) <= halfSize + EPSILON
				&& Math.abs(point.z) <= halfSize + EPSILON;
	}

	private static Result fallbackFromSource(LivingEntity target, Vec3 center, Vec3 sourcePoint) {
		Vec3 localSource = worldToLocal(target, center, sourcePoint);
		double dominant = maxAbs(localSource);
		if (dominant < EPSILON)
			return new Result(5, 0.5F, 0.5F);
		return resultAtSurface(localSource.scale(BeatificPalliumData.SHELL_HALF_SIZE / dominant));
	}

	private static Result projectToNearestFace(LivingEntity target, Vec3 center, Vec3 worldPoint, Vec3 worldMovement) {
		double halfSize = BeatificPalliumData.SHELL_HALF_SIZE;
		Vec3 local = worldToLocal(target, center, worldPoint);
		double dominant = maxAbs(local);
		if (dominant < EPSILON && worldMovement.lengthSqr() >= EPSILON) {
			Vec3 localMovement = worldVectorToLocal(target, worldMovement).reverse();
			dominant = maxAbs(localMovement);
			if (dominant >= EPSILON)
				local = localMovement.scale(halfSize / dominant);
		}

		if (maxAbs(local) < EPSILON)
			local = new Vec3(0.0D, 0.0D, -halfSize);
		else if (dominant > halfSize)
			local = new Vec3(
					Mth.clamp(local.x, -halfSize, halfSize),
					Mth.clamp(local.y, -halfSize, halfSize),
					Mth.clamp(local.z, -halfSize, halfSize)
			);
		else if (Math.abs(local.x) >= Math.abs(local.y) && Math.abs(local.x) >= Math.abs(local.z))
			local = new Vec3(Math.copySign(halfSize, nonZero(local.x)), local.y, local.z);
		else if (Math.abs(local.y) >= Math.abs(local.z))
			local = new Vec3(local.x, Math.copySign(halfSize, nonZero(local.y)), local.z);
		else
			local = new Vec3(local.x, local.y, Math.copySign(halfSize, nonZero(local.z)));

		return resultAtSurface(local);
	}

	private static double nonZero(double value) {
		return Math.abs(value) < EPSILON ? 1.0D : value;
	}

	private static Result resultAtSurface(Vec3 point) {
		double ax = Math.abs(point.x);
		double ay = Math.abs(point.y);
		double az = Math.abs(point.z);
		int face;
		double uAxis;
		double vAxis;
		if (ax >= ay && ax >= az) {
			face = point.x >= 0.0D ? 0 : 1;
			uAxis = point.z;
			vAxis = point.y;
		} else if (ay >= az) {
			face = point.y <= 0.0D ? 2 : 3;
			uAxis = point.x;
			vAxis = point.z;
		} else {
			face = point.z >= 0.0D ? 4 : 5;
			uAxis = point.x;
			vAxis = point.y;
		}
		return new Result(face, normalizeSurfaceCoordinate(uAxis), normalizeSurfaceCoordinate(vAxis));
	}

	private static float normalizeSurfaceCoordinate(double coordinate) {
		double halfSize = BeatificPalliumData.SHELL_HALF_SIZE;
		return (float) Mth.clamp(0.5D + coordinate / (halfSize * 2.0D), 0.0D, 1.0D);
	}

	private static double maxAbs(Vec3 vector) {
		return Math.max(Math.abs(vector.x), Math.max(Math.abs(vector.y), Math.abs(vector.z)));
	}

	private static Vec3 visualCenter(LivingEntity target) {
		return new Vec3(
				target.getX(),
				target.getY() + target.getBbHeight() * BeatificPalliumData.VISUAL_CENTER_HEIGHT_FACTOR,
				target.getZ()
		);
	}

	private static Vec3 worldToLocal(LivingEntity target, Vec3 center, Vec3 worldPoint) {
		return worldVectorToLocal(target, worldPoint.subtract(center));
	}

	private static Vec3 worldVectorToLocal(LivingEntity target, Vec3 worldVector) {
		double yaw = Math.toRadians(target.yBodyRot);
		double cos = Math.cos(yaw);
		double sin = Math.sin(yaw);
		return new Vec3(
				worldVector.x * cos + worldVector.z * sin,
				-worldVector.y,
				worldVector.x * sin - worldVector.z * cos
		);
	}

	public record Result(int face, float u, float v) {
	}
}
