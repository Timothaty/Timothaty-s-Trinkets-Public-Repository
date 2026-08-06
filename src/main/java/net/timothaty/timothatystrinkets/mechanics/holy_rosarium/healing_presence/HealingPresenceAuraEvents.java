package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.healing_presence;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumBead;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumHelper;
import net.timothaty.timothatystrinkets.mechanics.pact.PactOfAllianceHelper;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class HealingPresenceAuraEvents {
	public static final double AURA_RADIUS = 8.0D;
	public static final double AURA_RADIUS_SQR = 64.0D;

	public static final int AURA_PULSE_INTERVAL_TICKS = 40;
	public static final int EFFECT_DURATION_TICKS = 90;
	public static final int EFFECT_REFRESH_THRESHOLD_TICKS = 15;

	private HealingPresenceAuraEvents() {
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Pre event) {
		Player player = event.getEntity();
		if (player == null || player.level().isClientSide() || !isActiveAuraSource(player))
			return;

		HealingPresenceVfxKeepalive.tick(player);
		if (Math.floorMod(player.tickCount + player.getId(), AURA_PULSE_INTERVAL_TICKS) != 0)
			return;

		refreshEffect(player, player);

		AABB auraBounds = player.getBoundingBox().inflate(AURA_RADIUS);
		for (Player target : player.level().getEntitiesOfClass(Player.class, auraBounds, HealingPresenceAuraEvents::isValidTarget)) {
			if (target == player || player.distanceToSqr(target) > AURA_RADIUS_SQR)
				continue;
			if (!PactOfAllianceHelper.areAllied(player, target))
				continue;

			refreshEffect(target, player);
		}
	}

	private static boolean isActiveAuraSource(Player player) {
		return isValidTarget(player)
				&& HolyRosariumHelper.hasActiveCombination(
						player,
						HolyRosariumBead.HUMILITY,
						HolyRosariumBead.PENANCE
				);
	}

	private static boolean isValidTarget(Player player) {
		return player != null
				&& player.isAlive()
				&& !player.isDeadOrDying()
				&& !player.isRemoved()
				&& !player.isSpectator();
	}

	private static void refreshEffect(Player target, Player source) {
		MobEffectInstance active = target.getEffect(TimothatysTrinketsModMobEffects.HEALING_PRESENCE);
		if (active != null && active.getDuration() > EFFECT_REFRESH_THRESHOLD_TICKS)
			return;

		target.addEffect(new MobEffectInstance(
				TimothatysTrinketsModMobEffects.HEALING_PRESENCE,
				EFFECT_DURATION_TICKS,
				0,
				true,
				false,
				true
		), source);
	}
}
