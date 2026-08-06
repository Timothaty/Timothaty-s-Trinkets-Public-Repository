package net.timothaty.timothatystrinkets.mechanics.anathema;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
public final class AnathemaIronGolemPatrolGoal extends Goal {
	private static final double PLAYER_SENSE_RADIUS = 48.0D;
	private static final int POI_SEARCH_RADIUS = 32;
	private static final double SPEED_MODIFIER = 1.0D;
	private static final int RETRY_TICKS = 40;
	private static final int PATH_REFRESH_TICKS = 30;
	private static final double ARRIVAL_DISTANCE_SQR = 3.0D * 3.0D;

	private final IronGolem golem;
	private BlockPos patrolTarget;
	private int retryTicks;
	private int pathRefreshTicks;

	public AnathemaIronGolemPatrolGoal(IronGolem golem) {
		this.golem = golem;
		setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (golem.getTarget() != null || !(golem.level() instanceof ServerLevel level))
			return false;
		if (retryTicks-- > 0)
			return false;

		retryTicks = RETRY_TICKS + golem.getRandom().nextInt(RETRY_TICKS + 1);
		Player cursedPlayer = findVillageAnathemaPlayer(level);
		if (cursedPlayer == null)
			return false;

		patrolTarget = choosePatrolTarget(level, cursedPlayer.blockPosition());
		return patrolTarget != null;
	}

	@Override
	public boolean canContinueToUse() {
		return patrolTarget != null && golem.getTarget() == null && !golem.getNavigation().isDone();
	}

	@Override
	public void start() {
		pathRefreshTicks = 0;
		moveToTarget();
	}

	@Override
	public void tick() {
		if (patrolTarget == null)
			return;

		golem.getLookControl().setLookAt(patrolTarget.getX() + 0.5D, patrolTarget.getY() + 0.5D, patrolTarget.getZ() + 0.5D, 20.0F, 20.0F);
		if (golem.distanceToSqr(patrolTarget.getX() + 0.5D, patrolTarget.getY() + 0.5D, patrolTarget.getZ() + 0.5D) <= ARRIVAL_DISTANCE_SQR) {
			patrolTarget = null;
			return;
		}

		if (--pathRefreshTicks <= 0) {
			pathRefreshTicks = PATH_REFRESH_TICKS;
			moveToTarget();
		}
	}

	@Override
	public void stop() {
		patrolTarget = null;
		golem.getNavigation().stop();
	}

	private Player findVillageAnathemaPlayer(ServerLevel level) {
		AABB bounds = golem.getBoundingBox().inflate(PLAYER_SENSE_RADIUS);
		return level.getEntitiesOfClass(
			Player.class,
			bounds,
			player -> player.isAlive()
				&& !player.isCreative()
				&& !player.isSpectator()
				&& AnathemaHelper.hasLevel(player, 4)
				&& AnathemaVillageRules.isVillageTerritory(level, player.blockPosition())
		).stream().min(java.util.Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
	}

	private BlockPos choosePatrolTarget(ServerLevel level, BlockPos playerPos) {
		return level.getPoiManager().getRandom(
			type -> type.is(PoiTypeTags.VILLAGE),
			pos -> true,
			PoiManager.Occupancy.ANY,
			playerPos,
			POI_SEARCH_RADIUS,
			golem.getRandom()
		).map(BlockPos::immutable).orElse(null);
	}

	private void moveToTarget() {
		if (patrolTarget != null)
			golem.getNavigation().moveTo(patrolTarget.getX() + 0.5D, patrolTarget.getY(), patrolTarget.getZ() + 0.5D, SPEED_MODIFIER);
	}
}
