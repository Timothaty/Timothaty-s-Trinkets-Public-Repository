package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.confession;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.world.entity.npc.Villager;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class ConfessionHealingGoalAttachment {
	private static final int GOAL_PRIORITY = 3;

	private ConfessionHealingGoalAttachment() {
	}

	@SubscribeEvent
	public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Villager villager))
			return;

		boolean alreadyAttached = villager.goalSelector.getAvailableGoals().stream()
				.anyMatch(wrapped -> wrapped.getGoal() instanceof ConfessionClericHealingGoal);
		if (!alreadyAttached)
			villager.goalSelector.addGoal(GOAL_PRIORITY, new ConfessionClericHealingGoal(villager));
	}
}
