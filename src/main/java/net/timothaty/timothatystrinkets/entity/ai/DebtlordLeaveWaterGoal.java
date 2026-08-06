package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.DebtlordEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public final class DebtlordLeaveWaterGoal extends Goal {
	private static final int SEARCH_RADIUS = 12;
	private static final int MIN_Y_OFFSET = -2;
	private static final int MAX_Y_OFFSET = 4;
	private static final int RECALCULATION_INTERVAL_TICKS = 10;

	private final DebtlordEntity debtlord;
	private final double speedModifier;
	private BlockPos escapePos;
	private int recalculationTicks;

	public DebtlordLeaveWaterGoal(DebtlordEntity debtlord, double speedModifier) {
		this.debtlord = debtlord;
		this.speedModifier = speedModifier;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return debtlord.isAlive()
			&& !debtlord.isUsingAbility()
			&& debtlord.isTouchingWaterForBossLogic();
	}

	@Override
	public boolean canContinueToUse() {
		return debtlord.isAlive()
			&& !debtlord.isUsingAbility()
			&& debtlord.isTouchingWaterForBossLogic();
	}

	@Override
	public void start() {
		recalculationTicks = 0;
		escapePos = null;
		tryMoveToDryLand();
	}

	@Override
	public void tick() {
		if (--recalculationTicks <= 0 || debtlord.getNavigation().isDone()) {
			recalculationTicks = RECALCULATION_INTERVAL_TICKS;
			tryMoveToDryLand();
		}
		if (escapePos != null)
			debtlord.getLookControl().setLookAt(escapePos.getX() + 0.5D, escapePos.getY() + debtlord.getBbHeight() * 0.5D, escapePos.getZ() + 0.5D, 20.0F, 20.0F);
		debtlord.getJumpControl().jump();
	}

	@Override
	public void stop() {
		escapePos = null;
		debtlord.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	private void tryMoveToDryLand() {
		escapePos = findNearestDryStandPosition();
		if (escapePos != null)
			debtlord.getNavigation().moveTo(escapePos.getX() + 0.5D, escapePos.getY(), escapePos.getZ() + 0.5D, speedModifier);
	}

	private BlockPos findNearestDryStandPosition() {
		Level level = debtlord.level();
		BlockPos origin = debtlord.blockPosition();
		BlockPos bestPos = null;
		double bestDistanceSqr = Double.MAX_VALUE;
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

		for (int radius = 1; radius <= SEARCH_RADIUS; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (Math.abs(dx) != radius && Math.abs(dz) != radius)
						continue;

					for (int dy = MIN_Y_OFFSET; dy <= MAX_Y_OFFSET; dy++) {
						cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
						if (!isDryStandPosition(level, cursor))
							continue;

						double distanceSqr = cursor.distToCenterSqr(debtlord.position());
						if (distanceSqr < bestDistanceSqr) {
							bestDistanceSqr = distanceSqr;
							bestPos = cursor.immutable();
						}
					}
				}
			}
			if (bestPos != null)
				return bestPos;
		}
		return null;
	}

	private boolean isDryStandPosition(Level level, BlockPos feetPos) {
		BlockPos groundPos = feetPos.below();
		BlockState groundState = level.getBlockState(groundPos);
		if (groundState.isAir() || !groundState.isFaceSturdy(level, groundPos, Direction.UP))
			return false;
		if (!level.getFluidState(groundPos).isEmpty())
			return false;

		int clearance = Math.max(2, Mth.ceil(debtlord.getBbHeight()));
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int y = 0; y < clearance; y++) {
			cursor.set(feetPos.getX(), feetPos.getY() + y, feetPos.getZ());
			if (!level.getFluidState(cursor).isEmpty())
				return false;
			if (!level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty())
				return false;
		}
		return true;
	}
}
