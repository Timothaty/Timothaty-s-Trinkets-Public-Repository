package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.NecromancerEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.npc.Villager;

public class NecromancerVillagerTargetGoal extends NearestAttackableTargetGoal<Villager> {
	private final NecromancerEntity necromancer;
	private final NecromancerVillageSense.Cache villageSense;

	public NecromancerVillagerTargetGoal(NecromancerEntity necromancer, NecromancerVillageSense.Cache villageSense) {
		super(necromancer, Villager.class, true, NecromancerVillagerTargetGoal::canTargetVillager);
		this.necromancer = necromancer;
		this.villageSense = villageSense;
	}

	@Override
	public boolean canUse() {
		return !hasActiveVillage() && super.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		return !hasActiveVillage() && canTargetVillager(this.mob.getTarget()) && super.canContinueToUse();
	}

	private boolean hasActiveVillage() {
		return necromancer.level() instanceof ServerLevel serverLevel
			&& villageSense.hasNearbyVillage(serverLevel, necromancer.blockPosition(), necromancer.getRandom());
	}

	private static boolean canTargetVillager(LivingEntity candidate) {
		return candidate instanceof Villager
			&& candidate.isAlive()
			&& !candidate.hasEffect(TimothatysTrinketsModMobEffects.UNDEADIFICATION);
	}
}
