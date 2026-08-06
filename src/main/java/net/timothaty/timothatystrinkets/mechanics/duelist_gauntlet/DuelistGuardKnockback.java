package net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class DuelistGuardKnockback {
	private static final double SIDE_KNOCKBACK_BIAS = 0.45D;

	private DuelistGuardKnockback() {
	}

	public static void knockAttackerAway(Player defender, LivingEntity attacker, DuelistGuardDirection direction) {
		if (defender == null || attacker == null)
			return;

		Vec3 away = attacker.position().subtract(defender.position());
		Vec3 horizontalAway = new Vec3(away.x, 0.0D, away.z);
		if (horizontalAway.lengthSqr() < 1.0E-4D)
			horizontalAway = new Vec3(defender.getLookAngle().x, 0.0D, defender.getLookAngle().z);
		if (horizontalAway.lengthSqr() < 1.0E-4D)
			horizontalAway = new Vec3(0.0D, 0.0D, 1.0D);

		Vec3 push = horizontalAway.normalize();
		if (direction != null && direction.isSide()) {
			Vec3 right = getRightVector(defender);
			double sideSign = direction == DuelistGuardDirection.LEFT ? -1.0D : 1.0D;
			push = push.scale(0.92D).add(right.scale(sideSign * SIDE_KNOCKBACK_BIAS));
		}
		if (push.lengthSqr() < 1.0E-4D)
			return;

		push = push.normalize();
		attacker.knockback(DuelistGuardData.SIDE_DEFLECT_KNOCKBACK, -push.x, -push.z);
	}

	private static Vec3 getRightVector(Player defender) {
		Vec3 look = defender.getLookAngle();
		Vec3 right = new Vec3(-look.z, 0.0D, look.x);
		return right.lengthSqr() > 1.0E-5D ? right.normalize() : new Vec3(1.0D, 0.0D, 0.0D);
	}
}
