package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.beatific_pallium;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.active_ability.ActiveAbilityUseGuard;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumBead;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumHelper;
import net.timothaty.timothatystrinkets.network.BeatificPalliumCastMessage;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import net.neoforged.neoforge.network.PacketDistributor;

public final class BeatificPalliumAbility {
	private BeatificPalliumAbility() {
	}

	public static boolean tryActivate(ServerPlayer player) {
		if (!canActivate(player))
			return false;

		if (!BeatificPalliumState.activate(player, player))
			return false;

		PacketDistributor.sendToPlayersTrackingEntityAndSelf(
				player,
				new BeatificPalliumCastMessage(player.getId())
		);
		player.getCooldowns().addCooldown(TimothatysTrinketsModItems.HOLY_ROSARIUM.get(), BeatificPalliumData.COOLDOWN_TICKS);
		player.level().playSound(
				null,
				player.getX(),
				player.getY() + player.getBbHeight() * 0.5D,
				player.getZ(),
				TimothatysTrinketsModSounds.BEATIFIC_PALLIUM_CAST.get(),
				SoundSource.PLAYERS,
				1.0F,
				1.0F
		);
		return true;
	}

	private static boolean canActivate(ServerPlayer player) {
		return player != null
				&& !ActiveAbilityUseGuard.isBlocked(player)
				&& !player.isSleeping()
				&& !player.isUsingItem()
				&& !player.isPassenger()
				&& player.containerMenu == player.inventoryMenu
				&& !player.getCooldowns().isOnCooldown(TimothatysTrinketsModItems.HOLY_ROSARIUM.get())
				&& HolyRosariumHelper.hasActiveCombination(player, HolyRosariumBead.HUMILITY, HolyRosariumBead.SAINT);
	}

}
