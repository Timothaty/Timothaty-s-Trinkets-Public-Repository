package net.timothaty.timothatystrinkets.mechanics.holy_rosarium;

import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.cherubims_wisdom.CherubimsWisdomAbility;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.beatific_pallium.BeatificPalliumAbility;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class HolyRosariumActiveAbilityRouter {
	private HolyRosariumActiveAbilityRouter() {
	}

	public static HolyRosariumActivationResult tryActivate(Player player) {
		if (!(player instanceof ServerPlayer serverPlayer))
			return HolyRosariumActivationResult.NOT_APPLICABLE;
		if (HolyRosariumHelper.hasActiveCombination(
				serverPlayer,
				HolyRosariumBead.HUMILITY,
				HolyRosariumBead.SAINT
		)) {
			return BeatificPalliumAbility.tryActivate(serverPlayer)
					? HolyRosariumActivationResult.ACTIVATED
					: HolyRosariumActivationResult.HANDLED_FAILURE;
		}
		if (!HolyRosariumHelper.hasActiveCombination(
				serverPlayer,
				HolyRosariumBead.RESURRECTION,
				HolyRosariumBead.SACRAMENT
			))
			return HolyRosariumActivationResult.NOT_APPLICABLE;

		return CherubimsWisdomAbility.tryActivate(serverPlayer)
				? HolyRosariumActivationResult.ACTIVATED
				: HolyRosariumActivationResult.HANDLED_FAILURE;
	}
}
