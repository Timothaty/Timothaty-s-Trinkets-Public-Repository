package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.active_ability.ActiveAbilityCastLock;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumCombination;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumData;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumHelper;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumState;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.gorge.GorgeState;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.wrath_of_the_wicked.WrathOfTheWickedState;
import net.timothaty.timothatystrinkets.mechanics.pagans_charm.PaganCharmMeditationPlayerState;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.SlotResult;

public final class HubrisAbility {
	private HubrisAbility() {
	}

	public static boolean tryActivate(Player player) {
		return player instanceof ServerPlayer serverPlayer && tryActivate(serverPlayer);
	}

	public static boolean tryActivate(ServerPlayer player) {
		if (player == null
				|| !player.isAlive()
				|| player.isDeadOrDying()
				|| player.isRemoved()
				|| player.isSpectator()
				|| TimothatysTrinketsStunHelper.isStunned(player)
				|| ActiveAbilityCastLock.isLocked(player)
				|| HubrisActivationState.isCasting(player)
				|| HubrisState.hasActiveSession(player)
				|| player.hasEffect(TimothatysTrinketsModMobEffects.HUBRIS)
				|| GorgeState.hasActiveSession(player)
				|| WrathOfTheWickedState.isActive(player)
				|| player.containerMenu != player.inventoryMenu
				|| isMeditating(player))
			return false;
		if (player.getCooldowns().isOnCooldown(TimothatysTrinketsModItems.CORRUPTED_ROSARY.get()))
			return false;

		ItemStack weapon = player.getMainHandItem();
		HubrisAnimationVariant variant = weapon.is(HubrisData.HEAVY_ARMS)
				? HubrisAnimationVariant.HEAVY
				: weapon.is(ItemTags.SWORDS) ? HubrisAnimationVariant.SWORD : null;
		if (variant == null)
			return false;

		CorruptedRosariumState.markDirty(player);
		CorruptedRosariumState.refreshNow(player);
		SlotResult sourceRosarium = CorruptedRosariumHelper.findActiveRosariumResult(player).orElse(null);
		if (sourceRosarium == null
				|| CorruptedRosariumData.getCombination(sourceRosarium.stack())
						.filter(combination -> combination == CorruptedRosariumCombination.HUBRIS)
						.isEmpty())
			return false;

		if (!HubrisActivationState.begin(
				player,
				variant,
				weapon,
				CorruptedRosariumState.getRevision(player),
				sourceRosarium.slotContext()
		))
			return false;

		TimothatysTrinketsStunHelper.applyStunImmunity(player, HubrisData.STUN_IMMUNITY_TICKS);
		player.getCooldowns().addCooldown(
				TimothatysTrinketsModItems.CORRUPTED_ROSARY.get(),
				HubrisData.COOLDOWN_TICKS
		);
		return true;
	}

	private static boolean isMeditating(ServerPlayer player) {
		return player instanceof PaganCharmMeditationPlayerState state
				&& state.timothatys_trinkets$getPaganCharmMeditationPhase(player.tickCount)
				!= PaganCharmMeditationPlayerState.PHASE_NONE;
	}
}
