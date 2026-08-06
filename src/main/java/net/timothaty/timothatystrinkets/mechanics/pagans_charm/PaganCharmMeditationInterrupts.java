package net.timothaty.timothatystrinkets.mechanics.pagans_charm;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PaganCharmMeditationInterrupts {
	private static final Map<UUID, Integer> BENIGN_SWING_UNTIL = new HashMap<>();

	private PaganCharmMeditationInterrupts() {
	}

	public static void allowBenignSwing(Player player) {
		if (player == null)
			return;
		if (player.level().isClientSide())
			return;
		if (!(player instanceof PaganCharmMeditationPlayerState state) || !state.timothatys_trinkets$isPaganCharmMeditationPrimed())
			return;

		BENIGN_SWING_UNTIL.put(player.getUUID(), player.tickCount + PaganCharmTuning.BENIGN_SWING_GRACE_TICKS);
	}

	public static void interruptFromSwing(Player player) {
		if (player == null)
			return;

		Integer benignUntil = BENIGN_SWING_UNTIL.get(player.getUUID());
		if (benignUntil != null) {
			BENIGN_SWING_UNTIL.remove(player.getUUID());
			if (player.tickCount <= benignUntil)
				return;
		}

		interrupt(player);
	}

	public static void interrupt(Player player) {
		if (player instanceof PaganCharmMeditationPlayerState state) {
			state.timothatys_trinkets$interruptPaganCharmMeditation();
		}
		if (player != null) {
			BENIGN_SWING_UNTIL.remove(player.getUUID());
		}
	}

	public static void clearAll() {
		BENIGN_SWING_UNTIL.clear();
	}
}
