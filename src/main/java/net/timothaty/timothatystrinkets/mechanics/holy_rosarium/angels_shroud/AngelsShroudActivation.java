package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.angels_shroud;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;

public final class AngelsShroudActivation {
	private AngelsShroudActivation() {
	}

	public static boolean tryActivate(ServerPlayer player) {
		if (player == null || !player.isAlive() || player.isDeadOrDying() || player.isRemoved()
				|| player.hasEffect(TimothatysTrinketsModMobEffects.ANGELS_SHROUD)) {
			return false;
		}

		MobEffectInstance shroud = new MobEffectInstance(
				TimothatysTrinketsModMobEffects.ANGELS_SHROUD,
				AngelsShroudData.DURATION_TICKS,
				0,
				false,
				false,
				true
		);
		if (!player.addEffect(shroud, player))
			return false;

		player.addEffect(new MobEffectInstance(
				TimothatysTrinketsModMobEffects.STUN_IMMUNITY,
				AngelsShroudData.STUN_IMMUNITY_TICKS,
				0,
				false,
				false,
				false
		), player);
		player.getCooldowns().addCooldown(
				TimothatysTrinketsModItems.HOLY_ROSARIUM.get(),
				AngelsShroudData.HOLY_ROSARIUM_COOLDOWN_TICKS
		);
		player.level().playSound(
				null,
				player.getX(),
				player.getY() + player.getBbHeight() * 0.5D,
				player.getZ(),
				TimothatysTrinketsModSounds.ANGELS_SHROUD_ACTIVATION.get(),
				SoundSource.PLAYERS,
				1.0F,
				1.0F
		);
		AngelsShroudCrowdControl.apply(player);
		return true;
	}
}
