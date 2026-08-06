package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium;

import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.gorge.GorgeAbility;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris.HubrisAbility;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.wrath_of_the_wicked.WrathOfTheWickedAbility;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;

public final class CorruptedRosariumActiveAbilityRouter {
	private CorruptedRosariumActiveAbilityRouter() {
	}

	public static CorruptedRosariumActivationResult tryActivate(Player player) {
		if (!(player instanceof ServerPlayer serverPlayer))
			return CorruptedRosariumActivationResult.NOT_APPLICABLE;

		Optional<SlotResult> activeRosarium = CorruptedRosariumHelper.findActiveRosariumResult(serverPlayer);
		if (activeRosarium.isEmpty())
			return CorruptedRosariumActivationResult.NOT_APPLICABLE;

		Optional<CorruptedRosariumCombination> combination = CorruptedRosariumCombination.fromMask(
				CorruptedRosariumData.getKnownBeadMask(activeRosarium.get().stack())
		);
		if (combination.isEmpty())
			return CorruptedRosariumActivationResult.NOT_APPLICABLE;

		boolean activated = switch (combination.get()) {
			case GORGE -> GorgeAbility.tryActivate(serverPlayer);
			case WRATH_OF_THE_WICKED -> WrathOfTheWickedAbility.tryActivate(serverPlayer);
			case HUBRIS -> HubrisAbility.tryActivate(serverPlayer);
		};
		return activated
				? CorruptedRosariumActivationResult.ACTIVATED
				: CorruptedRosariumActivationResult.HANDLED_FAILURE;
	}
}
