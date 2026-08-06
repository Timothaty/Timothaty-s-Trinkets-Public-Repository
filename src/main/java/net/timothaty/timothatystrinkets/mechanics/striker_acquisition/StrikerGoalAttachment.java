package net.timothaty.timothatystrinkets.mechanics.striker_acquisition;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class StrikerGoalAttachment {
	private static final int GOAL_PRIORITY = 3;

	private StrikerGoalAttachment() {
	}

	@SubscribeEvent
	public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
		if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Villager villager))
			return;

		boolean alreadyAttached = villager.goalSelector.getAvailableGoals().stream()
				.anyMatch(wrapped -> wrapped.getGoal() instanceof StrikerForgingDeliveryGoal);
		if (!alreadyAttached)
			villager.goalSelector.addGoal(GOAL_PRIORITY, new StrikerForgingDeliveryGoal(villager));
		StrikerCommissionData.normalizeOnJoin(level, villager);
	}
}
