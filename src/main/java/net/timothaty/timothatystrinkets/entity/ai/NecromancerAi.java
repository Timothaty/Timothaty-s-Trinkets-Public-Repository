package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.NecromancerEntity;

import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;

public final class NecromancerAi {
	private NecromancerAi() {
	}

	public static void registerGoals(NecromancerEntity necromancer, GoalSelector goalSelector, GoalSelector targetSelector) {
		NecromancerUndeadificationSpellGoal undeadificationSpellGoal = new NecromancerUndeadificationSpellGoal(necromancer);
		NecromancerMagicDamageSpellGoal magicDamageSpellGoal = new NecromancerMagicDamageSpellGoal(necromancer);
		NecromancerSummonUndeadGoal summonUndeadGoal = new NecromancerSummonUndeadGoal(necromancer);
		NecromancerVillageSense.Cache villageSense = new NecromancerVillageSense.Cache();

		goalSelector.addGoal(0, new FloatGoal(necromancer));
		goalSelector.addGoal(2, undeadificationSpellGoal);
		goalSelector.addGoal(3, magicDamageSpellGoal);
		goalSelector.addGoal(4, new NecromancerApproachCastTargetGoal(necromancer, undeadificationSpellGoal, magicDamageSpellGoal, summonUndeadGoal, 1.05D));
		goalSelector.addGoal(5, summonUndeadGoal);
		goalSelector.addGoal(6, new NecromancerKeepDistanceGoal(necromancer, 1.05D, 7, 4));
		goalSelector.addGoal(7, new NecromancerStrafeGoal(necromancer, 1.0D, 0.45F));
		goalSelector.addGoal(8, new NecromancerVillageStalkGoal(necromancer, 1.0D, villageSense));
		goalSelector.addGoal(9, new RandomStrollGoal(necromancer, 0.8D));
		goalSelector.addGoal(10, new LookAtPlayerGoal(necromancer, Player.class, 8.0F));
		goalSelector.addGoal(11, new RandomLookAroundGoal(necromancer));

		targetSelector.addGoal(1, new HurtByTargetGoal(necromancer));
		targetSelector.addGoal(2, new NecromancerVillageVillagerTargetGoal(necromancer, villageSense));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(necromancer, Player.class, true));
		targetSelector.addGoal(4, new NecromancerVillagerTargetGoal(necromancer, villageSense));
		targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(necromancer, IronGolem.class, true));
	}
}
