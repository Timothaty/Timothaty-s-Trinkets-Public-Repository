package net.timothaty.timothatystrinkets.mechanics.bloodstained;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class BloodstainedHelper {
	private BloodstainedHelper() {
	}

	public static boolean hasBloodstained(LivingEntity entity) {
		return entity != null && entity.hasEffect(TimothatysTrinketsModMobEffects.BLOODSTAINED);
	}

	public static void applyOrRefresh(ServerPlayer player) {
		if (player == null || !(player.level() instanceof ServerLevel level))
			return;

		player.removeEffect(TimothatysTrinketsModMobEffects.BLOODSTAINED);
		player.addEffect(new MobEffectInstance(
			TimothatysTrinketsModMobEffects.BLOODSTAINED,
			BloodstainedData.DEFAULT_DURATION_TICKS,
			0,
			false,
			false,
			true
		));
		level.sendParticles(
			TimothatysTrinketsModParticleTypes.BLOOD_BIT.get(),
			player.getX(),
			player.getY() + player.getBbHeight() * 0.55D,
			player.getZ(),
			14,
			player.getBbWidth() * 0.45D,
			player.getBbHeight() * 0.4D,
			player.getBbWidth() * 0.45D,
			0.04D
		);
		level.playSound(
			null,
			player.blockPosition(),
			TimothatysTrinketsModSounds.BLOODSTAINED.get(),
			SoundSource.PLAYERS,
			0.8F,
			0.9F + level.getRandom().nextFloat() * 0.3F
		);
	}

	public static void replaceDuration(ServerPlayer player, MobEffectInstance current, int duration) {
		if (player == null || current == null || player.level().isClientSide())
			return;
		player.removeEffect(TimothatysTrinketsModMobEffects.BLOODSTAINED);
		if (duration > 0) {
			player.addEffect(new MobEffectInstance(
				TimothatysTrinketsModMobEffects.BLOODSTAINED,
				duration,
				0,
				current.isAmbient(),
				false,
				current.showIcon()
			));
		}
	}
}
