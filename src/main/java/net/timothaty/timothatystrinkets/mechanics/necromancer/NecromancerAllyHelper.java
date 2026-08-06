package net.timothaty.timothatystrinkets.mechanics.necromancer;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

public final class NecromancerAllyHelper {
	private NecromancerAllyHelper() {
	}

	public static boolean shouldUndeadIgnoreTarget(LivingEntity attacker, LivingEntity target) {
		return attacker != null
			&& target != null
			&& (isFriendlyPair(attacker, target)
					|| attacker.getType().is(EntityTypeTags.UNDEAD)
					&& target.hasEffect(TimothatysTrinketsModMobEffects.UNDEADIFICATION));
	}

	public static boolean shouldBlockFriendlyDamage(LivingEntity victim, LivingEntity attacker) {
		if (victim == null || attacker == null || victim == attacker) {
			return false;
		}

		return shouldUndeadIgnoreTarget(attacker, victim) || isFriendlyPair(victim, attacker);
	}

	public static boolean isFriendlyPair(LivingEntity first, LivingEntity second) {
		return first != null
			&& second != null
			&& (NecromancerSummonedMinionEvents.isNecromancerOwnerPair(first, second)
					|| NecromancerSummonedMinionEvents.hasSameNecromancerOwner(first, second)
					|| isUnholyAuraUndeadAlly(first, second));
	}

	private static boolean isUnholyAuraUndeadAlly(LivingEntity first, LivingEntity second) {
		return first.getType().is(EntityTypeTags.UNDEAD)
			&& second.getType().is(EntityTypeTags.UNDEAD)
			&& first.hasEffect(TimothatysTrinketsModMobEffects.UNHOLY_AURA)
			&& second.hasEffect(TimothatysTrinketsModMobEffects.UNHOLY_AURA);
	}

	public static LivingEntity getLivingAttacker(DamageSource source) {
		if (source == null) {
			return null;
		}

		Entity attacker = source.getEntity();
		if (attacker instanceof LivingEntity livingAttacker) {
			return livingAttacker;
		}

		Entity directEntity = source.getDirectEntity();
		if (directEntity instanceof LivingEntity directLiving) {
			return directLiving;
		}

		if (directEntity instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity projectileOwner) {
			return projectileOwner;
		}

		return null;
	}
}
