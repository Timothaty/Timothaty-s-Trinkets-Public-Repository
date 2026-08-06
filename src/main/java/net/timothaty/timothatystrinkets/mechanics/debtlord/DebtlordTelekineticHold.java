package net.timothaty.timothatystrinkets.mechanics.debtlord;

import net.timothaty.timothatystrinkets.network.DebtlordHoldStateMessage;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class DebtlordTelekineticHold {
	private static final Set<Player> HELD_PLAYERS = Collections.newSetFromMap(new WeakHashMap<>());

	private DebtlordTelekineticHold() {
	}

	public static void start(LivingEntity target) {
		if (target instanceof ServerPlayer player && HELD_PLAYERS.add(player))
			PacketDistributor.sendToPlayer(player, new DebtlordHoldStateMessage(true));
	}

	public static void tick(LivingEntity target, Vec3 anchor) {
		target.fallDistance = 0.0F;
		target.setDeltaMovement(Vec3.ZERO);
		target.setPos(anchor.x, anchor.y, anchor.z);
		target.hurtMarked = true;
		target.hasImpulse = true;
	}

	public static void end(LivingEntity target) {
		if (target == null)
			return;
		target.fallDistance = 0.0F;
		if (target instanceof ServerPlayer player && HELD_PLAYERS.remove(player) && !player.isRemoved())
			PacketDistributor.sendToPlayer(player, new DebtlordHoldStateMessage(false));
	}

	public static boolean isHeld(Player player) {
		return player != null && HELD_PLAYERS.contains(player);
	}
}
