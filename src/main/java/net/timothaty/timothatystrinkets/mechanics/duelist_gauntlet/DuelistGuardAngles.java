package net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class DuelistGuardAngles {
	private DuelistGuardAngles() {
	}

	public static DuelistGuardDirection getAttackDirection(Player player, LivingEntity attacker) {
		double relative = getRelativeAttackAngle(player, attacker);
		if (relative >= -45.0D && relative <= 45.0D)
			return DuelistGuardDirection.CENTER;
		if (relative > 45.0D && relative <= 135.0D)
			return DuelistGuardDirection.RIGHT;
		if (relative < -45.0D && relative >= -135.0D)
			return DuelistGuardDirection.LEFT;
		return DuelistGuardDirection.BACK;
	}

	public static double getRelativeAttackAngle(Player player, LivingEntity attacker) {
		if (player == null || attacker == null)
			return 180.0D;

		double dx = attacker.getX() - player.getX();
		double dz = attacker.getZ() - player.getZ();
		double angleToAttacker = Math.toDegrees(Math.atan2(dz, dx)) - 90.0D;
		return Mth.wrapDegrees(angleToAttacker - player.getYRot());
	}

	public static boolean isInsideFrontalGuardArc(Player player, LivingEntity attacker) {
		return isInsideFrontalGuardArc(getRelativeAttackAngle(player, attacker));
	}

	public static boolean isInsideFrontalGuardArc(double relativeAttackAngle) {
		return Math.abs(relativeAttackAngle) <= DuelistGuardData.FRONTAL_GUARD_ARC_DEGREES;
	}
}
