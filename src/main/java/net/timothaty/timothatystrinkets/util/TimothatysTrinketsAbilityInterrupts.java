package net.timothaty.timothatystrinkets.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;

import net.timothaty.timothatystrinkets.entity.UndeadKnightEntity;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.wrath_of_the_wicked.WrathOfTheWickedState;

public final class TimothatysTrinketsAbilityInterrupts {
	private TimothatysTrinketsAbilityInterrupts() {
	}

	public static void interruptAll(LivingEntity entity) {
		if (entity == null) {
			return;
		}

		softInterrupt(entity);
		interruptKnownVanillaAbilities(entity);
		interruptProjectAbilities(entity);
	}

	public static void keepInterrupted(LivingEntity entity) {
		if (entity == null) {
			return;
		}

		softInterrupt(entity);
		interruptKnownVanillaAbilities(entity);
		interruptProjectAbilities(entity);
	}

	private static void softInterrupt(LivingEntity entity) {
		entity.stopUsingItem();
		entity.setSprinting(false);

		if (entity instanceof Mob mob) {
			mob.getNavigation().stop();
			mob.setAggressive(false);
		}
	}

	private static void interruptKnownVanillaAbilities(LivingEntity entity) {
		if (entity instanceof Creeper creeper) {
			creeper.setSwellDir(-1);
		}
	}

	private static void interruptProjectAbilities(LivingEntity entity) {
		WrathOfTheWickedState.interrupt(entity);

		var data = entity.getPersistentData();

		data.remove("ttr_astral_active");
		data.remove("ttr_astral_ticks");
		data.remove("ttr_casting_active");
		data.remove("ttr_casting_ticks");
		data.remove("ttr_channeling_active");
		data.remove("ttr_channeling_ticks");

		if (entity instanceof UndeadKnightEntity undeadKnight) {
			undeadKnight.interruptSoulAbsorption();
		}

		if (entity instanceof Mob mob) {
			mob.getNavigation().stop();
		}
	}
}
