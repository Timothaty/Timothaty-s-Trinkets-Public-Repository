package net.timothaty.timothatystrinkets.mechanics.fire;

import net.minecraft.world.entity.player.Player;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CustomSweepVisualState {
	private static final Map<UUID, ArrayDeque<AttackVisual>> ATTACKS = new HashMap<>();

	private CustomSweepVisualState() {
	}

	public static void beginAttack(Player player) {
		if (player == null)
			return;
		ATTACKS.computeIfAbsent(player.getUUID(), ignored -> new ArrayDeque<>()).push(new AttackVisual());
	}

	public static void markFiery(Player player) {
		AttackVisual visual = current(player);
		if (visual != null)
			visual.fiery = true;
	}

	public static void markPrideful(Player player) {
		AttackVisual visual = current(player);
		if (visual != null)
			visual.prideful = true;
	}

	public static Visual consume(Player player) {
		AttackVisual visual = current(player);
		if (visual == null || visual.consumed)
			return Visual.VANILLA;
		visual.consumed = true;
		if (visual.prideful)
			return Visual.PRIDEFUL;
		return visual.fiery ? Visual.FIERY : Visual.VANILLA;
	}

	public static void endAttack(Player player) {
		if (player == null)
			return;
		ArrayDeque<AttackVisual> stack = ATTACKS.get(player.getUUID());
		if (stack == null)
			return;
		if (!stack.isEmpty())
			stack.pop();
		if (stack.isEmpty())
			ATTACKS.remove(player.getUUID());
	}

	public static void clear(Player player) {
		if (player != null)
			ATTACKS.remove(player.getUUID());
	}

	public static void clearAll() {
		ATTACKS.clear();
	}

	private static AttackVisual current(Player player) {
		if (player == null)
			return null;
		ArrayDeque<AttackVisual> stack = ATTACKS.get(player.getUUID());
		return stack == null ? null : stack.peek();
	}

	public enum Visual {
		VANILLA,
		FIERY,
		PRIDEFUL
	}

	private static final class AttackVisual {
		private boolean fiery;
		private boolean prideful;
		private boolean consumed;
	}
}
