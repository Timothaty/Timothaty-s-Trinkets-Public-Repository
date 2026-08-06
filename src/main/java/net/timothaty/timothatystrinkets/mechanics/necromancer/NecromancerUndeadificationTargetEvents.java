package net.timothaty.timothatystrinkets.mechanics.necromancer;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class NecromancerUndeadificationTargetEvents {
	private NecromancerUndeadificationTargetEvents() {
	}

	@SubscribeEvent
	public static void onEntityTickPost(EntityTickEvent.Post event) {
		if (!(event.getEntity() instanceof Mob mob)
			|| Math.floorMod(mob.tickCount + mob.getId(), NecromancerConfig.FRIENDLY_FIRE_TARGET_CLEANUP_INTERVAL_TICKS) != 0) {
			return;
		}

		Level level = mob.level();
		LivingEntity target = mob.getTarget();
		if (level.isClientSide() || target == null || !target.isAlive()) {
			return;
		}

		if (NecromancerAllyHelper.shouldUndeadIgnoreTarget(mob, target)) {
			clearCombatWithAlly(mob, target);
			if (target instanceof Mob targetMob && NecromancerAllyHelper.isFriendlyPair(mob, targetMob)) {
				clearCombatWithAlly(targetMob, mob);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
		LivingEntity victim = event.getEntity();
		if (victim == null || victim.level().isClientSide()) {
			return;
		}

		LivingEntity attacker = NecromancerAllyHelper.getLivingAttacker(event.getSource());
		if (!NecromancerAllyHelper.shouldBlockFriendlyDamage(victim, attacker)) {
			return;
		}

		event.setCanceled(true);

		if (victim instanceof Mob victimMob) {
			clearCombatWithAlly(victimMob, attacker);
		}

		if (attacker instanceof Mob attackerMob) {
			clearCombatWithAlly(attackerMob, victim);
		}
	}

	private static void clearCombatWithAlly(Mob mob, LivingEntity ally) {
		if (mob.getTarget() == ally) {
			mob.setTarget(null);
			mob.getNavigation().stop();
		}

		if (mob.getLastHurtByMob() == ally) {
			mob.setLastHurtByMob(null);
		}

		if (mob.getTarget() == null) {
			mob.setAggressive(false);
		}
	}
}
