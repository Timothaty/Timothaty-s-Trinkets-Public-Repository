package net.timothaty.timothatystrinkets.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class SoulOrbPullPhysics {
	public static final double COLLECT_DISTANCE = 0.35D;
	private static final double COLLECT_DISTANCE_SQR = COLLECT_DISTANCE * COLLECT_DISTANCE;
	private static final double MIN_PULL_SPEED = 0.045D;
	private static final double MAX_PULL_SPEED = 0.22D;
	private static final double PREVIOUS_MOTION_DAMPING = 0.45D;

	private SoulOrbPullPhysics() {
	}

	public static boolean pullOrReached(SoulOrbEntity orb, Vec3 target) {
		double dx = target.x - orb.getX();
		double dy = target.y - (orb.getY() + orb.getBbHeight() * 0.5D);
		double dz = target.z - orb.getZ();
		double distanceSqr = dx * dx + dy * dy + dz * dz;
		if (distanceSqr <= COLLECT_DISTANCE_SQR) {
			return true;
		}

		double distance = Math.sqrt(distanceSqr);
		double speed = Mth.clamp(MIN_PULL_SPEED + distance * 0.035D, MIN_PULL_SPEED, MAX_PULL_SPEED)
				* orb.getSoulAbsorptionSpeedMultiplier();
		double pullScale = speed / distance;
		Vec3 previousMotion = orb.getDeltaMovement();
		orb.setDeltaMovement(
				previousMotion.x * PREVIOUS_MOTION_DAMPING + dx * pullScale,
				previousMotion.y * PREVIOUS_MOTION_DAMPING + dy * pullScale,
				previousMotion.z * PREVIOUS_MOTION_DAMPING + dz * pullScale);
		orb.hurtMarked = true;
		orb.hasImpulse = true;
		return false;
	}

}
