package net.timothaty.timothatystrinkets.mechanics.armlet_gauntlet_synergy;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.timothaty.timothatystrinkets.entity.SoulOrbEntity;
import net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet.DuelistGuardState;

import java.util.List;

public final class ArmletGauntletSynergyHelper {
	private static final double HAND_SIDE_OFFSET = 0.32D;
	private static final double HAND_VERTICAL_OFFSET = -0.42D;
	private static final double HAND_FORWARD_OFFSET = 0.38D;
	private static final double GEOMETRY_EPSILON = 1.0E-6D;

	private ArmletGauntletSynergyHelper() {
	}

	public static ArmletGauntletSynergyState.Snapshot getSnapshot(Player player) {
		return ArmletGauntletSynergyState.get(player);
	}

	public static ArmletGauntletSynergyState.Snapshot getOrRefreshSnapshot(Player player) {
		return ArmletGauntletSynergyState.getOrRefresh(player);
	}

	public static boolean hasSynergy(Player player) {
		return player != null && getOrRefreshSnapshot(player).synergyActive();
	}

	public static boolean hasSynergy(ArmletGauntletSynergyState.Snapshot snapshot) {
		return snapshot != null && snapshot.synergyActive();
	}

	public static int getActiveGauntletSlot(Player player) {
		ArmletGauntletSynergyState.Snapshot snapshot = getOrRefreshSnapshot(player);
		return snapshot.synergyActive() ? snapshot.gauntletSlot() : -1;
	}

	public static InteractionHand getActiveInteractionHand(Player player) {
		ArmletGauntletSynergyState.Snapshot snapshot = getOrRefreshSnapshot(player);
		return snapshot.synergyActive() ? snapshot.interactionHand() : null;
	}

	public static HumanoidArm getActivePhysicalArm(Player player) {
		ArmletGauntletSynergyState.Snapshot snapshot = getOrRefreshSnapshot(player);
		return snapshot.synergyActive() ? snapshot.physicalArm() : null;
	}

	public static boolean canChannel(Player player) {
		return canChannel(player, getOrRefreshSnapshot(player));
	}

	public static boolean canChannel(Player player, ArmletGauntletSynergyState.Snapshot snapshot) {
		if (player == null || snapshot == null || !snapshot.synergyActive()
				|| player.level().isClientSide() || !player.isAlive() || player.isDeadOrDying() || player.isRemoved()
				|| player.isSpectator() || player.isSleeping() || player.isUsingItem() || DuelistGuardState.isGuarding(player)
				|| SoulEmpowerHelper.getLevel(player) >= ArmletGauntletSynergyData.MAX_SOUL_EMPOWER_LEVEL) {
			return false;
		}

		InteractionHand activeHand = snapshot.interactionHand();
		return activeHand != null && player.getItemInHand(activeHand).isEmpty();
	}

	public static SoulOrbEntity findBestAvailableSoulOrb(Player player, double coneDegrees) {
		return findBestAvailableSoulOrbWithMinimumDot(player, minimumDot(coneDegrees));
	}

	public static SoulOrbEntity findBestAvailableSoulOrbWithMinimumDot(Player player, double minimumDot) {
		if (player == null) {
			return null;
		}

		Vec3 eyes = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		double lookLengthSqr = look.lengthSqr();
		if (lookLengthSqr <= GEOMETRY_EPSILON) {
			return null;
		}
		double inverseLookLength = 1.0D / Math.sqrt(lookLengthSqr);
		double lookX = look.x * inverseLookLength;
		double lookY = look.y * inverseLookLength;
		double lookZ = look.z * inverseLookLength;

		AABB searchBox = player.getBoundingBox().inflate(ArmletGauntletSynergyData.MAX_RANGE);
		List<SoulOrbEntity> orbs = player.level().getEntitiesOfClass(
				SoulOrbEntity.class,
				searchBox,
				orb -> player.level().isClientSide()
						? !orb.hasSoulAbsorptionTrail() && !orb.isRemoved()
						: orb.isAvailableForSoulAbsorption()
		);
		SoulOrbEntity best = null;
		double bestDot = -1.0D;
		double bestDistanceSqr = Double.MAX_VALUE;

		for (SoulOrbEntity orb : orbs) {
			double centerX = orb.getX();
			double centerY = orb.getY() + orb.getBbHeight() * 0.5D;
			double centerZ = orb.getZ();
			double offsetX = centerX - eyes.x;
			double offsetY = centerY - eyes.y;
			double offsetZ = centerZ - eyes.z;
			double distanceSqr = offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ;
			if (distanceSqr > ArmletGauntletSynergyData.MAX_RANGE_SQR || distanceSqr <= GEOMETRY_EPSILON) {
				continue;
			}

			double inverseDistance = 1.0D / Math.sqrt(distanceSqr);
			double dot = (lookX * offsetX + lookY * offsetY + lookZ * offsetZ) * inverseDistance;
			if (dot < minimumDot) {
				continue;
			}
			Vec3 center = new Vec3(centerX, centerY, centerZ);
			if (!hasClearBlockPath(player, eyes, center, distanceSqr)) {
				continue;
			}

			if (dot > bestDot + GEOMETRY_EPSILON
					|| (Math.abs(dot - bestDot) <= GEOMETRY_EPSILON && distanceSqr < bestDistanceSqr)) {
				best = orb;
				bestDot = dot;
				bestDistanceSqr = distanceSqr;
			}
		}

		return best;
	}

	public static boolean isSoulOrbInView(Player player, SoulOrbEntity orb, double coneDegrees) {
		return isSoulOrbInViewWithMinimumDot(player, orb, minimumDot(coneDegrees));
	}

	public static boolean isSoulOrbInViewWithMinimumDot(Player player, SoulOrbEntity orb, double minimumDot) {
		if (player == null || orb == null || orb.isRemoved()) {
			return false;
		}

		Vec3 eyes = player.getEyePosition();
		double centerX = orb.getX();
		double centerY = orb.getY() + orb.getBbHeight() * 0.5D;
		double centerZ = orb.getZ();
		double offsetX = centerX - eyes.x;
		double offsetY = centerY - eyes.y;
		double offsetZ = centerZ - eyes.z;
		double distanceSqr = offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ;
		if (distanceSqr > ArmletGauntletSynergyData.MAX_RANGE_SQR || distanceSqr <= GEOMETRY_EPSILON) {
			return false;
		}

		Vec3 look = player.getLookAngle();
		double lookLengthSqr = look.lengthSqr();
		if (lookLengthSqr <= GEOMETRY_EPSILON) {
			return false;
		}
		double dot = (look.x * offsetX + look.y * offsetY + look.z * offsetZ)
				/ Math.sqrt(lookLengthSqr * distanceSqr);
		if (dot < minimumDot) {
			return false;
		}

		return hasClearBlockPath(player, eyes, new Vec3(centerX, centerY, centerZ), distanceSqr);
	}

	public static Vec3 getActiveHandPosition(Player player) {
		HumanoidArm arm = getActivePhysicalArm(player);
		return arm == null ? player.getEyePosition() : getHandPosition(player, arm);
	}

	public static Vec3 getHandPosition(Player player, HumanoidArm arm) {
		if (player == null) {
			return Vec3.ZERO;
		}
		if (arm == null) {
			return player.getEyePosition();
		}

		Vec3 look = player.getLookAngle();
		double lookLengthSqr = look.lengthSqr();
		double lookScale = lookLengthSqr <= GEOMETRY_EPSILON ? 0.0D : 1.0D / Math.sqrt(lookLengthSqr);
		double lookX = look.x * lookScale;
		double lookY = look.y * lookScale;
		double lookZ = look.z * lookScale;
		double flatLengthSqr = lookX * lookX + lookZ * lookZ;
		double forwardX;
		double forwardZ;
		if (flatLengthSqr <= GEOMETRY_EPSILON) {
			forwardX = 0.0D;
			forwardZ = 1.0D;
		} else {
			double inverseFlatLength = 1.0D / Math.sqrt(flatLengthSqr);
			forwardX = lookX * inverseFlatLength;
			forwardZ = lookZ * inverseFlatLength;
		}
		double rightX = -forwardZ;
		double rightZ = forwardX;
		double side = arm == HumanoidArm.RIGHT ? 1.0D : -1.0D;
		Vec3 eyes = player.getEyePosition();
		return new Vec3(
				eyes.x + rightX * HAND_SIDE_OFFSET * side + lookX * HAND_FORWARD_OFFSET,
				eyes.y + lookY * HAND_FORWARD_OFFSET + HAND_VERTICAL_OFFSET,
				eyes.z + rightZ * HAND_SIDE_OFFSET * side + lookZ * HAND_FORWARD_OFFSET
		);
	}

	private static boolean hasClearBlockPath(Player player, Vec3 eyes, Vec3 center, double distanceSqr) {
		HitResult obstruction = player.level().clip(
				new ClipContext(eyes, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)
		);
		return obstruction.getType() == HitResult.Type.MISS
				|| obstruction.getLocation().distanceToSqr(eyes) + 0.01D >= distanceSqr;
	}

	private static double minimumDot(double coneDegrees) {
		if (Double.compare(coneDegrees, ArmletGauntletSynergyData.ACQUIRE_CONE_DEGREES) == 0) {
			return ArmletGauntletSynergyData.ACQUIRE_MIN_DOT;
		}
		if (Double.compare(coneDegrees, ArmletGauntletSynergyData.RETAIN_CONE_DEGREES) == 0) {
			return ArmletGauntletSynergyData.RETAIN_MIN_DOT;
		}
		return Math.cos(Math.toRadians(coneDegrees));
	}
}
