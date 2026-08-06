package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.confession;

import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaVillagerBlessingState;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.behavior.VillagerPanicTrigger;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

public final class ConfessionClericHealingGoal extends Goal {
	private final Villager cleric;
	private ServerPlayer target;
	private Path path;
	private int castTicks;
	private boolean healCommitted;
	private boolean finished;

	public ConfessionClericHealingGoal(Villager cleric) {
		this.cleric = cleric;
		setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		target = ConfessionHealingCoordinator.getAssignedTarget(cleric);
		if (target == null || !isEligibleForAssignment(cleric, ConfessionHealingCoordinator.isEmergency(target)))
			return false;

		path = cleric.distanceToSqr(target) <= ConfessionData.HEAL_DISTANCE_SQR
				? null
				: cleric.getNavigation().createPath(target, 0);
		if (cleric.distanceToSqr(target) > ConfessionData.HEAL_DISTANCE_SQR && path == null) {
			ConfessionHealingCoordinator.releaseForCleric(cleric);
			target = null;
			return false;
		}
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		if (finished || target == null)
			return false;
		if (healCommitted) {
			return castTicks > 0
					&& ConfessionHealingCoordinator.ownsCommittedReservation(cleric, target)
					&& isSafeToContinueCommittedCast(cleric);
		}
		if (!ConfessionHealingCoordinator.ownsReservation(cleric, target)
				|| !isEligibleForAssignment(cleric, ConfessionHealingCoordinator.isEmergency(target)))
			return false;
		if (castTicks > 0 || cleric.distanceToSqr(target) <= ConfessionData.HEAL_DISTANCE_SQR)
			return true;
		return !cleric.getNavigation().isDone();
	}

	@Override
	public void start() {
		finished = false;
		castTicks = 0;
		healCommitted = false;
		if (cleric.level() instanceof ServerLevel level)
			cleric.getBrain().stopAll(level, cleric);
		suppressScheduledBrainIntent();
		if (path != null)
			cleric.getNavigation().moveTo(path, ConfessionData.WALK_SPEED);
	}

	@Override
	public void tick() {
		if (target == null)
			return;
		suppressScheduledBrainIntent();
		cleric.getLookControl().setLookAt(target, 30.0F, 30.0F);

		if (healCommitted) {
			cleric.getNavigation().stop();
			if (castTicks > 0)
				castTicks--;
			if (castTicks == 0)
				finishCommittedHealing();
			return;
		}

		if (cleric.distanceToSqr(target) <= ConfessionData.HEAL_DISTANCE_SQR) {
			cleric.getNavigation().stop();
			beginCommittedHealing();
			return;
		}

		boolean pathActive = !cleric.getNavigation().isDone() && navigationTargets(target);
		if (!pathActive && !cleric.getNavigation().moveTo(target, ConfessionData.WALK_SPEED))
			finished = true;
	}

	@Override
	public void stop() {
		if (cleric.level() instanceof ServerLevel level && isSafeForHealing(cleric)) {
			cleric.getNavigation().stop();
			clearBrainIntent();
			cleric.getBrain().updateActivityFromSchedule(level.getDayTime(), level.getGameTime());
		}
		ConfessionHealingCoordinator.releaseForCleric(cleric);
		target = null;
		path = null;
		castTicks = 0;
		healCommitted = false;
		finished = false;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	static boolean isEligibleForAssignment(Villager cleric, boolean emergency) {
		return cleric != null
				&& cleric.isAlive()
				&& !cleric.isDeadOrDying()
				&& !cleric.isRemoved()
				&& !cleric.isBaby()
				&& !cleric.isSleeping()
				&& cleric.getVillagerData().getProfession() == VillagerProfession.CLERIC
				&& cleric.getHealth() > cleric.getMaxHealth() * ConfessionData.CLERIC_SAFETY_HEALTH_RATIO
				&& isSafeForHealing(cleric)
				&& (emergency || !isImportantScheduledActivity(cleric));
	}

	static boolean isSafeToContinueCommittedCast(Villager cleric) {
		return cleric != null
				&& cleric.isAlive()
				&& !cleric.isDeadOrDying()
				&& !cleric.isRemoved()
				&& !cleric.isBaby()
				&& !cleric.isSleeping()
				&& cleric.getVillagerData().getProfession() == VillagerProfession.CLERIC
				&& cleric.getHealth() > cleric.getMaxHealth() * ConfessionData.CLERIC_SAFETY_HEALTH_RATIO
				&& isSafeForHealing(cleric);
	}

	private static boolean isSafeForHealing(Villager cleric) {
		return cleric.getTradingPlayer() == null
				&& !cleric.isPassenger()
				&& !VillagerPanicTrigger.isHurt(cleric)
				&& !VillagerPanicTrigger.hasHostile(cleric)
				&& !isSafetyActivityActive(cleric);
	}

	private static boolean isSafetyActivityActive(Villager cleric) {
		return cleric.getBrain().isActive(Activity.PANIC)
				|| cleric.getBrain().isActive(Activity.HIDE)
				|| cleric.getBrain().isActive(Activity.RAID)
				|| cleric.getBrain().isActive(Activity.PRE_RAID)
				|| cleric.getBrain().isActive(Activity.REST)
				|| cleric.getBrain().isActive(Activity.FIGHT)
				|| cleric.getBrain().isActive(Activity.AVOID)
				|| cleric.getBrain().isActive(Activity.CELEBRATE);
	}

	private static boolean isImportantScheduledActivity(Villager cleric) {
		return cleric.getBrain().isActive(Activity.WORK)
				|| cleric.getBrain().isActive(Activity.MEET);
	}

	private void suppressScheduledBrainIntent() {
		cleric.getBrain().setActiveActivityIfPossible(Activity.IDLE);
		clearBrainIntent();
	}

	private void clearBrainIntent() {
		cleric.getBrain().eraseMemory(MemoryModuleType.PATH);
		cleric.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		cleric.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
		cleric.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
	}

	private boolean navigationTargets(ServerPlayer player) {
		BlockPos targetPos = cleric.getNavigation().getTargetPos();
		return targetPos != null && targetPos.closerToCenterThan(player.position(), 2.0D);
	}

	private void beginCommittedHealing() {
		if (!(cleric.level() instanceof ServerLevel level)
				|| target == null
				|| !ConfessionHealingCoordinator.ownsReservation(cleric, target)) {
			finished = true;
			ConfessionHealingCoordinator.releaseForCleric(cleric);
			return;
		}

		float beforeHealth = target.getHealth();
		target.heal(ConfessionData.HEAL_BASE + target.getMaxHealth() * ConfessionData.HEAL_MAX_HEALTH_RATIO);
		if (target.getHealth() <= beforeHealth) {
			finished = true;
			ConfessionHealingCoordinator.releaseForCleric(cleric);
			return;
		}

		if (!ConfessionHealingCoordinator.commitSuccessfulHeal(target, cleric)) {
			target.setHealth(beforeHealth);
			finished = true;
			ConfessionHealingCoordinator.releaseForCleric(cleric);
			return;
		}

		healCommitted = true;
		castTicks = AnathemaVillagerBlessingState.BLESSINGS_DURATION_TICKS;
		ConfessionHealingEffects.startBlessing(level, cleric, target);
	}

	private void finishCommittedHealing() {
		finished = true;
		if (!(cleric.level() instanceof ServerLevel level)
				|| target == null
				|| !healCommitted
				|| !ConfessionHealingCoordinator.completeCommittedHeal(target, cleric)) {
			return;
		}
		ConfessionHealingEffects.finishHealing(level, target);
	}
}
