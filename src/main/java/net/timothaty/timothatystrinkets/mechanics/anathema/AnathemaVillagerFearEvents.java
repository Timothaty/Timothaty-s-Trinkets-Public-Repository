package net.timothaty.timothatystrinkets.mechanics.anathema;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.pillagers_coin.PillagersCoinVillagerFearEvents;
import net.timothaty.timothatystrinkets.mechanics.pillagers_coin.PillagersCoinVillagerSleepEvents;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.display.ClericQuestRewardDisplayController;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.AABB;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class AnathemaVillagerFearEvents {
	private static final double FEAR_RADIUS = 24.0D;
	private static final int SCAN_INTERVAL_TICKS = 10;
	private static final long HIDE_GRACE_TICKS = 100L;
	private AnathemaVillagerFearEvents() {
	}

	@SubscribeEvent
	public static void onAnathemaPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (!(player.level() instanceof ServerLevel level)
				|| Math.floorMod(player.tickCount + player.getId(), SCAN_INTERVAL_TICKS) != 0
				|| !AnathemaHelper.hasLevel(player, 5)
				|| !AnathemaVillageRules.isVillageTerritory(level, player.blockPosition()))
			return;

		long hideUntil = level.getGameTime() + HIDE_GRACE_TICKS;
		AABB bounds = player.getBoundingBox().inflate(FEAR_RADIUS);
		for (Villager villager : level.getEntitiesOfClass(Villager.class, bounds, AnathemaVillagerFearEvents::isFearfulVillager)) {
			if (!(villager instanceof AnathemaVillagerRuntimeState state))
				continue;
			long previousHideUntil = state.timothatys_trinkets$getAnathemaHideUntil();
			ClericQuestRewardDisplayController.hide(villager);
			if (previousHideUntil <= level.getGameTime())
				clearCurrentIntent(villager);
			state.timothatys_trinkets$setAnathemaHideUntil(hideUntil);
		}
	}

	public static void tickFearState(Villager villager, AnathemaVillagerRuntimeState state) {
		if (!(villager.level() instanceof ServerLevel level))
			return;

		long hideUntil = state.timothatys_trinkets$getAnathemaHideUntil();
		if (hideUntil == 0L)
			return;
		if (villager.isSleeping() || PillagersCoinVillagerSleepEvents.isReturningToSleep(villager)) {
			state.timothatys_trinkets$setAnathemaHideUntil(0L);
			clearCurrentIntent(villager);
			villager.getNavigation().stop();
			villager.getBrain().setActiveActivityIfPossible(Activity.REST);
			return;
		}

		long gameTime = level.getGameTime();
		if (hideUntil > gameTime) {
			if (!villager.getBrain().isActive(Activity.HIDE))
				villager.getBrain().setActiveActivityIfPossible(Activity.HIDE);
		} else {
			state.timothatys_trinkets$setAnathemaHideUntil(0L);
			if (villager.getBrain().isActive(Activity.HIDE) && !PillagersCoinVillagerFearEvents.shouldHideFromCoin(villager, gameTime))
				villager.getBrain().updateActivityFromSchedule(level.getDayTime(), gameTime);
		}
	}

	public static boolean shouldHideFromAnathema(Villager villager, long gameTime) {
		return villager instanceof AnathemaVillagerRuntimeState state
			&& state.timothatys_trinkets$getAnathemaHideUntil() > gameTime;
	}

	private static boolean isFearfulVillager(Villager villager) {
		return villager.isAlive()
			&& !villager.isSleeping()
			&& !PillagersCoinVillagerSleepEvents.isReturningToSleep(villager)
			&& Math.floorMod(villager.getUUID().hashCode(), 100) < 50;
	}

	private static void clearCurrentIntent(Villager villager) {
		villager.getBrain().eraseMemory(MemoryModuleType.PATH);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
	}
}
