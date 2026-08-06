package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium;

import net.timothaty.timothatystrinkets.mechanics.pact.PactOfAllianceHelper;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class CorruptedRosariumTargeting {
	private CorruptedRosariumTargeting() {
	}

	public static boolean isAlliedMinion(Player owner, LivingEntity target) {
		if (owner == null || target == null || target == owner || target instanceof Player)
			return false;

		if (target instanceof OwnableEntity ownable) {
			UUID ownerUuid = ownable.getOwnerUUID();
			if (owner.getUUID().equals(ownerUuid))
				return true;
		}

		return owner.isAlliedTo(target) || target.isAlliedTo(owner);
	}

	public static boolean isProtectedCombatTarget(Player owner, LivingEntity target) {
		if (owner == null || target == null || target == owner)
			return true;
		if (!target.isAlive() || target.isRemoved() || target instanceof ArmorStand || target.isSpectator())
			return true;
		if (target instanceof Player targetPlayer && (targetPlayer.isCreative() || targetPlayer.isSpectator()))
			return true;
		return PactOfAllianceHelper.areAllied(owner, target) || isAlliedMinion(owner, target);
	}
}
