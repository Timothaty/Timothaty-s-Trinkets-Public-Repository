package net.timothaty.timothatystrinkets.entity.ai;

import net.neoforged.neoforge.common.CommonHooks;

import net.timothaty.timothatystrinkets.entity.UndeadKnightEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

public class UndeadKnightBreakDoorGoal extends Goal {
	private static final int TICKS_PER_HIT = 20;
	private static final int WOODEN_DOOR_HITS = 2;
	private static final int IRON_DOOR_HITS = 3;
	private static final double DOOR_REACH_SQR = 4.0D;

	private final Mob mob;
	private BlockPos doorPos = BlockPos.ZERO;
	private DoorTarget doorTarget = DoorTarget.NONE;
	private int breakTime;
	private int lastBreakProgress = -1;

	public UndeadKnightBreakDoorGoal(Mob mob) {
		this.mob = mob;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		if (mob.getNavigation() instanceof GroundPathNavigation navigation) {
			navigation.setCanOpenDoors(true);
			navigation.setCanPassDoors(true);
		}
	}

	@Override
	public boolean canUse() {
		if (this.isMobBusy() || !this.canBreakDoors() || !this.mob.horizontalCollision) {
			return false;
		}

		BlockPos foundDoor = this.findDoorToBreak();
		if (foundDoor == null || !this.canDestroy(foundDoor)) {
			return false;
		}

		DoorTarget foundTarget = this.classifyDoor(foundDoor);
		if (foundTarget == DoorTarget.NONE) {
			return false;
		}

		this.doorPos = foundDoor;
		this.doorTarget = foundTarget;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return !this.isMobBusy()
				&& this.breakTime < this.getBreakDuration()
				&& this.canBreakDoors()
				&& this.canDestroy(this.doorPos)
				&& this.classifyDoor(this.doorPos) == this.doorTarget
				&& this.isCloseEnough(this.doorPos);
	}

	@Override
	public void start() {
		this.breakTime = 0;
		this.lastBreakProgress = -1;
	}

	@Override
	public void stop() {
		this.mob.level().destroyBlockProgress(this.mob.getId(), this.doorPos, -1);
		this.doorTarget = DoorTarget.NONE;
		this.lastBreakProgress = -1;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		this.mob.getLookControl().setLookAt(
				this.doorPos.getX() + 0.5D,
				this.doorPos.getY() + 0.5D,
				this.doorPos.getZ() + 0.5D
		);

		int duration = this.getBreakDuration();
		this.breakTime++;
		int breakProgress = (int) ((float) this.breakTime / (float) duration * 10.0F);
		if (breakProgress != this.lastBreakProgress) {
			this.mob.level().destroyBlockProgress(this.mob.getId(), this.doorPos, breakProgress);
			this.lastBreakProgress = breakProgress;
		}

		if (this.breakTime < duration) {
			if (this.breakTime % TICKS_PER_HIT == 0) {
				this.mob.level().levelEvent(1019, this.doorPos, 0);
			}
			return;
		}

		this.breakDoor();
	}

	private void breakDoor() {
		Level level = this.mob.level();
		BlockState state = level.getBlockState(this.doorPos);
		level.levelEvent(1021, this.doorPos, 0);
		level.levelEvent(2001, this.doorPos, Block.getId(state));
		level.destroyBlock(this.doorPos, false, this.mob, Block.UPDATE_ALL);
	}

	private int getBreakDuration() {
		if (this.doorTarget == DoorTarget.IRON && this.mob.level().getDifficulty() == Difficulty.HARD) {
			return 1;
		}
		return TICKS_PER_HIT * (this.doorTarget == DoorTarget.WOODEN ? WOODEN_DOOR_HITS : IRON_DOOR_HITS);
	}

	private boolean canBreakDoors() {
		Level level = this.mob.level();
		return !level.isClientSide()
				&& level.getDifficulty() != Difficulty.PEACEFUL
				&& level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
	}

	private boolean isMobBusy() {
		return this.mob instanceof UndeadKnightEntity undeadKnight && (undeadKnight.isSoulAbsorbing() || undeadKnight.isEmpowering() || undeadKnight.isReincarnating());
	}

	private boolean canDestroy(BlockPos pos) {
		return CommonHooks.canEntityDestroy(this.mob.level(), pos, this.mob);
	}

	private BlockPos findDoorToBreak() {
		BlockPos pathDoor = this.findDoorOnPath();
		return pathDoor != null ? pathDoor : this.findNearbyDoor();
	}

	private BlockPos findDoorOnPath() {
		Path path = this.mob.getNavigation().getPath();
		if (path == null || path.isDone()) {
			return null;
		}

		int maxIndex = Math.min(path.getNextNodeIndex() + 2, path.getNodeCount());
		for (int i = 0; i < maxIndex; i++) {
			Node node = path.getNode(i);
			BlockPos nodeDoorPos = this.normalizeDoorPos(new BlockPos(node.x, node.y + 1, node.z));
			if (nodeDoorPos != null && this.classifyDoor(nodeDoorPos) != DoorTarget.NONE && this.isCloseEnough(nodeDoorPos)) {
				return nodeDoorPos;
			}
		}
		return null;
	}

	private BlockPos findNearbyDoor() {
		BlockPos origin = this.mob.blockPosition();
		BlockPos closestDoor = null;
		double closestDistance = Double.MAX_VALUE;

		for (int y = 0; y <= 1; y++) {
			for (int x = -1; x <= 1; x++) {
				for (int z = -1; z <= 1; z++) {
					BlockPos doorCandidate = this.normalizeDoorPos(origin.offset(x, y, z));
					if (doorCandidate == null || this.classifyDoor(doorCandidate) == DoorTarget.NONE) {
						continue;
					}

					double distance = this.distanceToDoorSqr(doorCandidate);
					if (distance < closestDistance && distance <= DOOR_REACH_SQR) {
						closestDoor = doorCandidate;
						closestDistance = distance;
					}
				}
			}
		}

		return closestDoor;
	}

	private BlockPos normalizeDoorPos(BlockPos pos) {
		Level level = this.mob.level();
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof DoorBlock)) {
			pos = pos.below();
			state = level.getBlockState(pos);
		}

		if (!(state.getBlock() instanceof DoorBlock)) {
			return null;
		}

		return state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
	}

	private DoorTarget classifyDoor(BlockPos pos) {
		BlockState state = this.mob.level().getBlockState(pos);
		if (!(state.getBlock() instanceof DoorBlock) || state.getValue(DoorBlock.OPEN)) {
			return DoorTarget.NONE;
		}
		if (state.is(Blocks.IRON_DOOR)) {
			return DoorTarget.IRON;
		}
		if (DoorBlock.isWoodenDoor(state)) {
			return DoorTarget.WOODEN;
		}
		return DoorTarget.NONE;
	}

	private boolean isCloseEnough(BlockPos pos) {
		return this.distanceToDoorSqr(pos) <= DOOR_REACH_SQR;
	}

	private double distanceToDoorSqr(BlockPos pos) {
		return this.mob.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
	}

	private enum DoorTarget {
		NONE,
		WOODEN,
		IRON
	}
}
