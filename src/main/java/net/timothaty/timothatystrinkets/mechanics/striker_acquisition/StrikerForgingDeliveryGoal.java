package net.timothaty.timothatystrinkets.mechanics.striker_acquisition;

import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaHelper;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaVillagerFearEvents;
import net.timothaty.timothatystrinkets.mechanics.pillagers_coin.PillagersCoinVillagerFearData;
import net.timothaty.timothatystrinkets.mechanics.pillagers_coin.PillagersCoinVillagerFearEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.VillagerPanicTrigger;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

public final class StrikerForgingDeliveryGoal extends Goal {
	private static final int RAID_CHECK_INTERVAL_TICKS = 20;
	private static final int WORKSTATION_RETRY_TICKS = 40;
	private static final int RECIPIENT_SCAN_INTERVAL_TICKS = 10;
	private static final int STUCK_TIMEOUT_TICKS = 100;
	private static final int DELIVERY_TIMEOUT_TICKS = 20 * 20;
	private static final double WORKSTATION_WALK_SPEED = 0.65D;
	private static final double DELIVERY_WALK_SPEED = 0.70D;
	private static final double RECIPIENT_MAX_DISTANCE_SQR = 16.0D * 16.0D;
	private static final double HANDOFF_DISTANCE_SQR = 2.5D * 2.5D;

	private final Villager villager;
	private Path plannedPath;
	private BlockPos workstationPos;
	private ServerPlayer deliveryTarget;
	private Vec3 workstationLookTarget;
	private int forgeTicks;
	private long nextRaidCheck;
	private long nextWorkstationAttempt;
	private long lastProgressAt;
	private long deliveryStartedAt;
	private double bestDistanceSqr = Double.MAX_VALUE;

	public StrikerForgingDeliveryGoal(Villager villager) {
		this.villager = villager;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (!(villager.level() instanceof ServerLevel level))
			return false;
		StrikerCommissionStage stage = StrikerCommissionData.getStage(villager);
		if (!stage.isActive()) {
			StrikerCommissionData.clearVisualItem(villager);
			return false;
		}
		if (!StrikerCommissionData.isQualifiedWeaponsmith(villager)) {
			StrikerCommissionData.clearCommission(level, villager);
			return false;
		}
		if (stage == StrikerCommissionStage.WAITING_FOR_RAID_VICTORY
				|| stage == StrikerCommissionStage.FORGING_PENDING)
			StrikerCommissionData.clearVisualItem(villager);

		if (stage == StrikerCommissionStage.WAITING_FOR_RAID_VICTORY) {
			checkRaidResult(level);
			stage = StrikerCommissionData.getStage(villager);
			if (stage != StrikerCommissionStage.FORGING_PENDING)
				return false;
		}

		if (stage == StrikerCommissionStage.WALKING_TO_WORKSTATION
				|| stage == StrikerCommissionStage.FORGING) {
			StrikerCommissionData.setStage(villager, StrikerCommissionStage.FORGING_PENDING);
			StrikerCommissionData.clearVisualItem(villager);
			stage = StrikerCommissionStage.FORGING_PENDING;
		} else if (stage == StrikerCommissionStage.DELIVERING) {
			StrikerCommissionData.setStage(villager, StrikerCommissionStage.DELIVERY_PENDING);
			stage = StrikerCommissionStage.DELIVERY_PENDING;
		}

		return switch (stage) {
			case FORGING_PENDING -> prepareWorkstationPath(level);
			case DELIVERY_PENDING -> prepareDeliveryPath(level);
			default -> false;
		};
	}

	@Override
	public boolean canContinueToUse() {
		if (!(villager.level() instanceof ServerLevel level))
			return false;
		if (!StrikerCommissionData.isQualifiedWeaponsmith(villager)) {
			StrikerCommissionData.clearCommission(level, villager);
			return false;
		}

		return switch (StrikerCommissionData.getStage(villager)) {
			case WALKING_TO_WORKSTATION -> canContinueWalkingToWorkstation(level);
			case FORGING -> canContinueForging(level);
			case DELIVERING -> canContinueDelivering(level);
			default -> false;
		};
	}

	@Override
	public void start() {
		if (!(villager.level() instanceof ServerLevel level))
			return;
		villager.getBrain().stopAll(level, villager);
		clearBrainMovementIntent();
		suppressScheduledBrainIntent();
		lastProgressAt = level.getGameTime();
		bestDistanceSqr = currentTravelDistanceSqr();
		if (plannedPath != null) {
			double speed = StrikerCommissionData.getStage(villager) == StrikerCommissionStage.DELIVERING
					? DELIVERY_WALK_SPEED
					: WORKSTATION_WALK_SPEED;
			villager.getNavigation().moveTo(plannedPath, speed);
		}
		synchronizeNavigationPathMemory();
	}

	@Override
	public void tick() {
		if (!(villager.level() instanceof ServerLevel level))
			return;
		suppressScheduledBrainIntent();
		switch (StrikerCommissionData.getStage(villager)) {
			case WALKING_TO_WORKSTATION -> tickWalkingToWorkstation(level);
			case FORGING -> tickForging(level);
			case DELIVERING -> tickDelivering(level);
			default -> {
			}
		}
		synchronizeNavigationPathMemory();
	}

	@Override
	public void stop() {
		ServerLevel level = villager.level() instanceof ServerLevel serverLevel ? serverLevel : null;
		StrikerCommissionStage stage = StrikerCommissionData.getStage(villager);
		villager.getNavigation().stop();
		clearBrainMovementIntent();

		if (stage == StrikerCommissionStage.WALKING_TO_WORKSTATION || stage == StrikerCommissionStage.FORGING) {
			StrikerCommissionData.setStage(villager, StrikerCommissionStage.FORGING_PENDING);
			StrikerCommissionData.clearVisualItem(villager);
		} else if (stage == StrikerCommissionStage.FORGING_PENDING) {
			StrikerCommissionData.clearVisualItem(villager);
		} else if (stage == StrikerCommissionStage.DELIVERING) {
			StrikerCommissionData.setStage(villager, StrikerCommissionStage.DELIVERY_PENDING);
			StrikerCommissionData.ensureDeliveryVisual(villager);
			if (level != null)
				scheduleNextRecipientScan(level, villager);
		} else if (stage == StrikerCommissionStage.DELIVERY_PENDING) {
			StrikerCommissionData.ensureDeliveryVisual(villager);
		}
		if (level != null && isSafeForCommission(level))
			villager.getBrain().updateActivityFromSchedule(level.getDayTime(), level.getGameTime());

		plannedPath = null;
		workstationPos = null;
		deliveryTarget = null;
		workstationLookTarget = null;
		forgeTicks = 0;
		bestDistanceSqr = Double.MAX_VALUE;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	private void checkRaidResult(ServerLevel level) {
		long now = level.getGameTime();
		if (now < nextRaidCheck)
			return;
		nextRaidCheck = now + RAID_CHECK_INTERVAL_TICKS;

		StrikerCommissionData.RaidIdentity identity = StrikerCommissionData.getRaidIdentity(villager).orElse(null);
		if (identity == null || !identity.dimension().equals(level.dimension().location().toString())) {
			StrikerCommissionData.clearCommission(level, villager);
			return;
		}
		Raid raid = level.getRaids().get(identity.id());
		if (raid == null || !StrikerCommissionData.raidMatches(level, villager, raid)) {
			StrikerCommissionData.clearCommission(level, villager);
			return;
		}
		if (raid.isVictory()) {
			StrikerCommissionData.setStage(villager, StrikerCommissionStage.FORGING_PENDING);
			return;
		}
		if (raid.isLoss() || raid.isStopped())
			StrikerCommissionData.clearCommission(level, villager);
	}

	private boolean prepareWorkstationPath(ServerLevel level) {
		long now = level.getGameTime();
		if (now < nextWorkstationAttempt || !isSafeForCommission(level) || cancelIfRecipientRejected(level))
			return false;
		nextWorkstationAttempt = now + WORKSTATION_RETRY_TICKS;

		BlockPos jobSite = StrikerCommissionData.getLoadedGrindstone(level, villager).orElse(null);
		if (jobSite == null)
			return false;
		workstationPos = jobSite.immutable();
		plannedPath = null;
		if (!canBeginForging(level, villager, workstationPos)) {
			plannedPath = villager.getNavigation().createPath(workstationPos, 0);
			if (plannedPath == null || !plannedPath.canReach()) {
				plannedPath = null;
				return false;
			}
		}

		StrikerCommissionData.setStage(villager, StrikerCommissionStage.WALKING_TO_WORKSTATION);
		return true;
	}

	private boolean prepareDeliveryPath(ServerLevel level) {
		StrikerCommissionData.ensureDeliveryVisual(villager);
		UUID recipientId = StrikerCommissionData.getRecipientId(villager).orElse(null);
		if (recipientId == null) {
			StrikerCommissionData.clearCommission(level, villager);
			return false;
		}
		if (PillagersCoinVillagerFearData.fears(villager, recipientId)) {
			StrikerCommissionData.clearCommission(level, villager);
			return false;
		}

		long now = level.getGameTime();
		if (now < StrikerCommissionData.getNextRecipientScan(villager))
			return false;
		scheduleNextRecipientScan(level, villager);

		ServerPlayer recipient = level.getServer().getPlayerList().getPlayer(recipientId);
		if (recipient == null || !isValidDeliveryTarget(level, recipient) || !isSafeForCommission(level))
			return false;
		if (AnathemaHelper.hasLevel(recipient, 2)) {
			StrikerCommissionData.clearCommission(level, villager);
			return false;
		}

		deliveryTarget = recipient;
		plannedPath = null;
		if (!canHandOff(recipient)) {
			plannedPath = villager.getNavigation().createPath(recipient, 0);
			if (plannedPath == null || !plannedPath.canReach()) {
				plannedPath = null;
				deliveryTarget = null;
				return false;
			}
		}

		deliveryStartedAt = now;
		StrikerCommissionData.setStage(villager, StrikerCommissionStage.DELIVERING);
		return true;
	}

	private boolean canContinueWalkingToWorkstation(ServerLevel level) {
		if (workstationPos == null
				|| !isSafeForCommission(level)
				|| cancelIfRecipientRejected(level)
				|| !StrikerCommissionData.jobSiteMatches(level, villager, workstationPos))
			return false;
		return true;
	}

	private boolean canContinueForging(ServerLevel level) {
		return workstationPos != null
				&& isSafeForCommission(level)
				&& !cancelIfRecipientRejected(level)
				&& StrikerCommissionData.jobSiteMatches(level, villager, workstationPos)
				&& canBeginForging(level, villager, workstationPos);
	}

	private boolean canContinueDelivering(ServerLevel level) {
		if (deliveryTarget == null || cancelIfRecipientRejected(level)
				|| !isSafeForCommission(level)
				|| !isValidDeliveryTarget(level, deliveryTarget))
			return false;
		return !villager.getNavigation().isDone() || canHandOff(deliveryTarget);
	}

	private void tickWalkingToWorkstation(ServerLevel level) {
		if (workstationPos == null)
			return;
		villager.getLookControl().setLookAt(Vec3.atCenterOf(workstationPos));
		if (canBeginForging(level, villager, workstationPos)) {
			beginForging(level);
			return;
		}

		boolean pathActive = plannedPath != null
				&& villager.getNavigation().moveTo(plannedPath, WORKSTATION_WALK_SPEED);
		if (!pathActive || villager.getNavigation().isDone()
				|| updateProgressAndIsStuck(level, currentTravelDistanceSqr()))
			StrikerCommissionData.setStage(villager, StrikerCommissionStage.FORGING_PENDING);
	}

	private void beginForging(ServerLevel level) {
		villager.getNavigation().stop();
		forgeTicks = 0;
		workstationLookTarget = Vec3.atCenterOf(workstationPos).add(0.0D, 0.15D, 0.0D);
		StrikerCommissionData.setStage(villager, StrikerCommissionStage.FORGING);
		tickForging(level);
	}

	private void tickForging(ServerLevel level) {
		villager.getNavigation().stop();
		villager.getBrain().eraseMemory(MemoryModuleType.PATH);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
		Vec3 movement = villager.getDeltaMovement();
		villager.setDeltaMovement(0.0D, movement.y, 0.0D);

		if (forgeTicks == 0 || forgeTicks == 60 || forgeTicks == 120 || forgeTicks == 170)
			workstationLookTarget = StrikerForgingEffects.runForgeImpulse(level, villager, workstationPos, forgeTicks);
		else if (forgeTicks == 190)
			StrikerForgingEffects.showFinishedStriker(villager);
		else if (forgeTicks == 200) {
			if (cancelIfRecipientRejected(level))
				return;
			clearBrainMovementIntent();
			StrikerForgingEffects.finishForging(level, workstationPos);
			StrikerCommissionData.setStage(villager, StrikerCommissionStage.DELIVERY_PENDING);
			scheduleNextRecipientScan(level, villager);
			return;
		}
		lockForgingAttention();
		forgeTicks++;
	}

	private void tickDelivering(ServerLevel level) {
		if (deliveryTarget == null)
			return;
		StrikerCommissionData.ensureDeliveryVisual(villager);
		villager.getLookControl().setLookAt(deliveryTarget, 30.0F, 30.0F);
		if (canHandOff(deliveryTarget)) {
			if (!cancelIfRecipientRejected(level)
					&& StrikerForgingEffects.throwStriker(level, villager, deliveryTarget))
				StrikerCommissionData.clearCommission(level, villager);
			return;
		}

		boolean pathActive = !villager.getNavigation().isDone() && navigationTargets(deliveryTarget);
		if (!pathActive)
			pathActive = villager.getNavigation().moveTo(deliveryTarget, DELIVERY_WALK_SPEED);
		if (!pathActive
				|| level.getGameTime() - deliveryStartedAt >= DELIVERY_TIMEOUT_TICKS
				|| updateProgressAndIsStuck(level, currentTravelDistanceSqr())) {
			StrikerCommissionData.setStage(villager, StrikerCommissionStage.DELIVERY_PENDING);
			scheduleNextRecipientScan(level, villager);
		}
	}

	private boolean cancelIfRecipientRejected(ServerLevel level) {
		UUID recipientId = StrikerCommissionData.getRecipientId(villager).orElse(null);
		if (recipientId == null || PillagersCoinVillagerFearData.fears(villager, recipientId)) {
			StrikerCommissionData.clearCommission(level, villager);
			return true;
		}
		ServerPlayer onlineRecipient = level.getServer().getPlayerList().getPlayer(recipientId);
		if (onlineRecipient != null && AnathemaHelper.hasLevel(onlineRecipient, 2)) {
			StrikerCommissionData.clearCommission(level, villager);
			return true;
		}
		return false;
	}

	private boolean isValidDeliveryTarget(ServerLevel level, ServerPlayer player) {
		return player.isAlive()
				&& !player.isRemoved()
				&& !player.isSpectator()
				&& player.serverLevel() == level
				&& villager.distanceToSqr(player) <= RECIPIENT_MAX_DISTANCE_SQR;
	}

	private boolean canHandOff(ServerPlayer player) {
		return villager.distanceToSqr(player) <= HANDOFF_DISTANCE_SQR && villager.hasLineOfSight(player);
	}

	private boolean navigationTargets(ServerPlayer player) {
		BlockPos targetPos = villager.getNavigation().getTargetPos();
		return targetPos != null && targetPos.closerToCenterThan(player.position(), 2.0D);
	}

	private boolean isSafeForCommission(ServerLevel level) {
		long now = level.getGameTime();
		Raid localRaid = level.getRaidAt(villager.blockPosition());
		return villager.isAlive()
				&& !villager.isSleeping()
				&& villager.getTradingPlayer() == null
				&& !villager.isPassenger()
				&& !VillagerPanicTrigger.isHurt(villager)
				&& !VillagerPanicTrigger.hasHostile(villager)
				&& !PillagersCoinVillagerFearEvents.shouldHideFromCoin(villager, now)
				&& !AnathemaVillagerFearEvents.shouldHideFromAnathema(villager, now)
				&& !isSafetyActivityActive()
				&& (localRaid == null || !localRaid.isActive() || localRaid.isOver() || localRaid.isStopped());
	}

	private boolean isSafetyActivityActive() {
		return villager.getBrain().isActive(Activity.PANIC)
				|| villager.getBrain().isActive(Activity.HIDE)
				|| villager.getBrain().isActive(Activity.RAID)
				|| villager.getBrain().isActive(Activity.PRE_RAID)
				|| villager.getBrain().isActive(Activity.REST)
				|| villager.getBrain().isActive(Activity.FIGHT)
				|| villager.getBrain().isActive(Activity.AVOID)
				|| villager.getBrain().isActive(Activity.CELEBRATE);
	}

	static boolean canBeginForging(ServerLevel level, Villager villager, BlockPos jobSitePos) {
		return StrikerCommissionData.jobSiteMatches(level, villager, jobSitePos)
				&& new AABB(jobSitePos).distanceToSqr(villager.position()) <= 1.0D
				&& hasClearWorkstationView(level, villager, jobSitePos);
	}

	static void scheduleNextRecipientScan(ServerLevel level, Villager villager) {
		StrikerCommissionData.setNextRecipientScan(villager, level.getGameTime() + RECIPIENT_SCAN_INTERVAL_TICKS);
	}

	static boolean hasClearWorkstationView(ServerLevel level, Villager villager, BlockPos jobSitePos) {
		Vec3 from = villager.getEyePosition();
		Vec3 to = Vec3.atCenterOf(jobSitePos).add(0.0D, 0.15D, 0.0D);
		BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, villager));
		return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(jobSitePos);
	}

	private boolean updateProgressAndIsStuck(ServerLevel level, double distanceSqr) {
		long now = level.getGameTime();
		if (distanceSqr + 0.25D < bestDistanceSqr) {
			bestDistanceSqr = distanceSqr;
			lastProgressAt = now;
		}
		return now - lastProgressAt >= STUCK_TIMEOUT_TICKS;
	}

	private double currentTravelDistanceSqr() {
		if (StrikerCommissionData.getStage(villager) == StrikerCommissionStage.DELIVERING && deliveryTarget != null)
			return villager.distanceToSqr(deliveryTarget);
		return workstationPos == null ? Double.MAX_VALUE : villager.distanceToSqr(Vec3.atCenterOf(workstationPos));
	}

	private void lockForgingAttention() {
		if (workstationLookTarget == null)
			return;
		StrikerForgingAttentionEvents.track(villager, workstationLookTarget);
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(workstationLookTarget));
		villager.getLookControl().setLookAt(
				workstationLookTarget.x,
				workstationLookTarget.y,
				workstationLookTarget.z,
				90.0F,
				90.0F
		);
	}

	private void suppressScheduledBrainIntent() {
		villager.getBrain().setActiveActivityIfPossible(Activity.IDLE);
		synchronizeNavigationPathMemory();
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
	}

	private void synchronizeNavigationPathMemory() {
		StrikerCommissionStage stage = StrikerCommissionData.getStage(villager);
		Path navigationPath = villager.getNavigation().getPath();
		if ((stage == StrikerCommissionStage.WALKING_TO_WORKSTATION
				|| stage == StrikerCommissionStage.DELIVERING)
				&& navigationPath != null
				&& !navigationPath.isDone()) {
			villager.getBrain().setMemory(MemoryModuleType.PATH, navigationPath);
		} else {
			villager.getBrain().eraseMemory(MemoryModuleType.PATH);
		}
	}

	private void clearBrainMovementIntent() {
		StrikerForgingAttentionEvents.clear(villager);
		villager.getNavigation().stop();
		villager.getBrain().eraseMemory(MemoryModuleType.PATH);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
	}
}
