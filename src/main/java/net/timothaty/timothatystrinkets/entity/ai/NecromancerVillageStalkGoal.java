package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.NecromancerEntity;
import net.timothaty.timothatystrinkets.mechanics.necromancer.NecromancerConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;

public class NecromancerVillageStalkGoal extends Goal {
	private static final int PATROL_TARGET_LIMIT = 32;
	private static final int SEARCH_RETRY_TICKS = 40;
	private static final int RANDOM_EXTRA_SEARCH_RETRY_TICKS = 40;
	private static final int PATH_RECALCULATE_TICKS = 30;
	private static final int MAX_PATH_FAILURES = 3;
	private static final int MAX_TIME_ON_TARGET_TICKS = 20 * 12;
	private static final double ARRIVAL_DISTANCE_SQR = 4.0D * 4.0D;
	private static final double MIN_NEXT_TARGET_DISTANCE_SQR = 7.0D * 7.0D;

	private final NecromancerEntity necromancer;
	private final NecromancerVillageSense.Cache villageSense;
	private final double speedModifier;
	private BlockPos patrolPos;
	private BlockPos patrolVillageCenter;
	private BlockPos previousPatrolPos;
	private int targetTicks;
	private int pathRecalculateTicks;
	private int pathFailures;
	private long nextSearchGameTime;

	public NecromancerVillageStalkGoal(NecromancerEntity necromancer, double speedModifier, NecromancerVillageSense.Cache villageSense) {
		this.necromancer = necromancer;
		this.speedModifier = speedModifier;
		this.villageSense = villageSense;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (!canPatrolVillage() || !(necromancer.level() instanceof ServerLevel serverLevel)) {
			return false;
		}

		long gameTime = serverLevel.getGameTime();
		if (gameTime < nextSearchGameTime) {
			return false;
		}

		nextSearchGameTime = gameTime + SEARCH_RETRY_TICKS + necromancer.getRandom().nextInt(RANDOM_EXTRA_SEARCH_RETRY_TICKS + 1);
		return selectNextPatrolTarget(serverLevel);
	}

	@Override
	public boolean canContinueToUse() {
		if (patrolPos == null || patrolVillageCenter == null || !canPatrolVillage() || !(necromancer.level() instanceof ServerLevel serverLevel)) {
			return false;
		}

		return villageSense.getCurrentVillageCenter(serverLevel, necromancer.blockPosition(), necromancer.getRandom())
			.filter(patrolVillageCenter::equals)
			.isPresent();
	}

	@Override
	public void start() {
		targetTicks = MAX_TIME_ON_TARGET_TICKS;
		pathRecalculateTicks = PATH_RECALCULATE_TICKS;
		pathFailures = 0;
		if (!moveToPatrolTarget()) {
			pathFailures = 1;
		}
	}

	@Override
	public void stop() {
		previousPatrolPos = patrolPos;
		patrolPos = null;
		patrolVillageCenter = null;
		targetTicks = 0;
		pathRecalculateTicks = 0;
		pathFailures = 0;
		necromancer.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (!(necromancer.level() instanceof ServerLevel serverLevel) || patrolPos == null) {
			return;
		}

		targetTicks--;

		if (shouldPickNewPatrolTarget()) {
			if (!selectNextPatrolTarget(serverLevel)) {
				patrolPos = null;
				return;
			}
			moveOrChooseAnother(serverLevel);
		}

		necromancer.getLookControl().setLookAt(patrolPos.getX() + 0.5D, patrolPos.getY() + 0.5D, patrolPos.getZ() + 0.5D, 20.0F, 20.0F);
		pathRecalculateTicks--;
		if (pathRecalculateTicks <= 0) {
			pathRecalculateTicks = PATH_RECALCULATE_TICKS;
			moveOrChooseAnother(serverLevel);
		}
	}

	private boolean canPatrolVillage() {
		return necromancer.isAlive()
			&& necromancer.getTarget() == null
			&& !necromancer.shouldRetreat()
			&& !necromancer.isCastingAnySpell();
	}

	private boolean shouldPickNewPatrolTarget() {
		return patrolPos == null
			|| isCloseTo(patrolPos)
			|| targetTicks <= 0;
	}

	private boolean selectNextPatrolTarget(ServerLevel serverLevel) {
		BlockPos villageCenter = villageSense.getCurrentVillageCenter(serverLevel, necromancer.blockPosition(), necromancer.getRandom()).orElse(null);
		if (villageCenter == null) {
			return false;
		}

		List<BlockPos> targets = villageSense.findVillagePatrolTargets(serverLevel, necromancer.blockPosition(), PATROL_TARGET_LIMIT, necromancer.getRandom());
		if (targets.isEmpty()) {
			return false;
		}

		List<BlockPos> reachableTargets = targets.stream()
			.filter(pos -> Math.abs(pos.getY() - necromancer.blockPosition().getY()) <= NecromancerConfig.VILLAGE_MAX_APPROACH_VERTICAL_DIFFERENCE)
			.toList();
		if (reachableTargets.isEmpty()) {
			return false;
		}

		List<BlockPos> preferredTargets = reachableTargets.stream()
			.filter(pos -> !pos.equals(patrolPos))
			.filter(pos -> !pos.equals(previousPatrolPos))
			.filter(pos -> necromancer.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) >= MIN_NEXT_TARGET_DISTANCE_SQR)
			.toList();

		List<BlockPos> candidates = preferredTargets.isEmpty()
			? reachableTargets.stream().filter(pos -> !pos.equals(patrolPos)).toList()
			: preferredTargets;
		if (candidates.isEmpty()) {
			candidates = reachableTargets;
		}

		previousPatrolPos = patrolPos;
		patrolPos = candidates.get(necromancer.getRandom().nextInt(candidates.size()));
		patrolVillageCenter = villageCenter;
		targetTicks = MAX_TIME_ON_TARGET_TICKS;
		pathRecalculateTicks = PATH_RECALCULATE_TICKS;
		pathFailures = 0;
		return true;
	}

	private boolean moveOrChooseAnother(ServerLevel serverLevel) {
		if (moveToPatrolTarget()) {
			pathFailures = 0;
			return true;
		}

		pathFailures++;
		if (pathFailures < MAX_PATH_FAILURES) {
			return false;
		}

		pathFailures = 0;
		return selectNextPatrolTarget(serverLevel) && moveToPatrolTarget();
	}

	private boolean moveToPatrolTarget() {
		if (patrolPos == null) {
			return false;
		}

		return necromancer.getNavigation().moveTo(patrolPos.getX() + 0.5D, patrolPos.getY(), patrolPos.getZ() + 0.5D, speedModifier);
	}

	private boolean isCloseTo(BlockPos pos) {
		return necromancer.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= ARRIVAL_DISTANCE_SQR;
	}
}
