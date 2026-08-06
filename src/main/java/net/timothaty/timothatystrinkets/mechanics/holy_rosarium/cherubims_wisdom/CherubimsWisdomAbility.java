package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.cherubims_wisdom;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.active_ability.ActiveAbilityUseGuard;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.gorge.GorgeState;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris.HubrisState;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.wrath_of_the_wicked.WrathOfTheWickedState;
import net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet.DuelistGuardState;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumBead;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumHelper;
import net.timothaty.timothatystrinkets.mechanics.pagans_charm.PaganCharmMeditationPlayerState;
import net.timothaty.timothatystrinkets.network.CherubimsWisdomActivationMessage;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import net.neoforged.neoforge.network.PacketDistributor;

public final class CherubimsWisdomAbility {
	private CherubimsWisdomAbility() {
	}

	public static boolean tryActivate(ServerPlayer player) {
		if (!canActivate(player))
			return false;

		int xpCost = CherubimsWisdomData.calculateXpCost(player.experienceLevel);
		boolean creative = player.getAbilities().instabuild;
		if (!creative && player.totalExperience < xpCost) {
			player.displayClientMessage(
					Component.translatable(CherubimsWisdomData.INSUFFICIENT_XP_MESSAGE, xpCost),
					true
			);
			return false;
		}

		MobEffectInstance effect = new MobEffectInstance(
				TimothatysTrinketsModMobEffects.CHERUBIMS_WISDOM,
				CherubimsWisdomData.DURATION_TICKS,
				0,
				false,
				false,
				true
		);
		if (!player.addEffect(effect, player))
			return false;

		if (!creative)
			player.giveExperiencePoints(-xpCost);
		player.getCooldowns().addCooldown(
				TimothatysTrinketsModItems.HOLY_ROSARIUM.get(),
				CherubimsWisdomData.COOLDOWN_TICKS
		);
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(
				player,
				new CherubimsWisdomActivationMessage(player.getId(), player.getMainArm().ordinal())
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
				&& !DuelistGuardState.isGuarding(player)
				&& !GorgeState.hasActiveSession(player)
				&& !HubrisState.hasActiveSession(player)
				&& !WrathOfTheWickedState.isActive(player)
				&& !isMeditating(player)
				&& !player.hasEffect(TimothatysTrinketsModMobEffects.CHERUBIMS_WISDOM)
				&& !player.getCooldowns().isOnCooldown(TimothatysTrinketsModItems.HOLY_ROSARIUM.get())
				&& HolyRosariumHelper.hasActiveCombination(
						player,
						HolyRosariumBead.RESURRECTION,
						HolyRosariumBead.SACRAMENT
				);
	}

	private static boolean isMeditating(ServerPlayer player) {
		return player instanceof PaganCharmMeditationPlayerState state
				&& state.timothatys_trinkets$getPaganCharmMeditationPhase(player.tickCount)
				!= PaganCharmMeditationPlayerState.PHASE_NONE;
	}

}
