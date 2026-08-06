package net.timothaty.timothatystrinkets.mechanics.pillagers_coin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;

public final class PillagersCoinVillagerSleepEvents {
	private PillagersCoinVillagerSleepEvents() {
	}

	public static void beginReturnToSleepSettle(ServerLevel level, Villager villager) {
		if (!(villager instanceof PillagersCoinVillagerRuntimeState state))
			return;
		state.timothatys_trinkets$setReturnToSleepSettleUntil(
			level.getGameTime() + PillagersCoinData.RETURN_TO_SLEEP_SETTLE_TICKS
		);
		stabilizeReturnToSleepAttempt(villager);
	}

	public static boolean isReturningToSleep(Villager villager) {
		return villager instanceof PillagersCoinVillagerRuntimeState state
			&& state.timothatys_trinkets$getReturnToSleepSettleUntil() != 0L;
	}

	public static void tickReturnToSleepSettle(Villager villager, PillagersCoinVillagerRuntimeState state) {
		if (!(villager.level() instanceof ServerLevel level))
			return;
		if (villager.isSleeping() && isDisplacedFromBed(villager)) {
			state.timothatys_trinkets$setReturnToSleepSettleUntil(0L);
			villager.stopSleeping();
			return;
		}
		long settleUntil = state.timothatys_trinkets$getReturnToSleepSettleUntil();
		if (settleUntil == 0L)
			return;
		if (villager.isSleeping()) {
			anchorSleepingVillager(villager);
			return;
		}
		if (level.getGameTime() >= settleUntil) {
			state.timothatys_trinkets$setReturnToSleepSettleUntil(0L);
			villager.getBrain().updateActivityFromSchedule(level.getDayTime(), level.getGameTime());
			return;
		}
		stabilizeReturnToSleepAttempt(villager);
	}

	private static void stabilizeReturnToSleepAttempt(Villager villager) {
		villager.getNavigation().stop();
		villager.getBrain().eraseMemory(MemoryModuleType.PATH);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
		if (!villager.getBrain().isActive(Activity.REST))
			villager.getBrain().setActiveActivityIfPossible(Activity.REST);
		villager.setDeltaMovement(Vec3.ZERO);
	}

	private static void anchorSleepingVillager(Villager villager) {
		villager.setDeltaMovement(Vec3.ZERO);
		villager.getSleepingPos().ifPresent(bedPos -> {
			Vec3 bedAnchor = new Vec3(bedPos.getX() + 0.5D, bedPos.getY() + 0.6875D, bedPos.getZ() + 0.5D);
			if (villager.position().distanceToSqr(bedAnchor) > 1.0E-4D)
				villager.setPos(bedAnchor.x, bedAnchor.y, bedAnchor.z);
		});
	}

	private static boolean isDisplacedFromBed(Villager villager) {
		return villager.getSleepingPos()
			.filter(bedPos -> villager.getY() > bedPos.getY() + 0.4D
				&& bedPos.closerToCenterThan(villager.position(), 1.14D))
			.isEmpty();
	}
}
