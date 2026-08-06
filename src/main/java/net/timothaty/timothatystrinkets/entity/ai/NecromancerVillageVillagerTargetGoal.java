package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.NecromancerEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.npc.Villager;

public class NecromancerVillageVillagerTargetGoal extends NearestAttackableTargetGoal<Villager> {
	private final NecromancerEntity necromancer;
	private final NecromancerVillageSense.Cache villageSense;

	public NecromancerVillageVillagerTargetGoal(NecromancerEntity necromancer, NecromancerVillageSense.Cache villageSense) {
		super(necromancer, Villager.class, true, candidate -> canTargetVillager(villageSense, candidate));
		this.necromancer = necromancer;
		this.villageSense = villageSense;
	}

	@Override
	public boolean canUse() {
		return isNearVillage() && super.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		return isNearVillage() && canTargetVillager(villageSense, this.mob.getTarget()) && super.canContinueToUse();
	}

	private boolean isNearVillage() {
		if (!(necromancer.level() instanceof ServerLevel serverLevel)) {
			return false;
		}

		return villageSense.hasNearbyVillage(serverLevel, necromancer.blockPosition(), necromancer.getRandom());
	}

	private static boolean canTargetVillager(NecromancerVillageSense.Cache villageSense, LivingEntity candidate) {
		return candidate instanceof Villager
			&& candidate.isAlive()
			&& !candidate.hasEffect(TimothatysTrinketsModMobEffects.UNDEADIFICATION)
			&& villageSense.isInsideCurrentVillage(candidate.blockPosition());
	}
}
