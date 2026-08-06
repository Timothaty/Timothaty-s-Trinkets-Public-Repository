package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.NecromancerEntity;
import net.timothaty.timothatystrinkets.entity.UndeadKnightEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class UndeadKnightFollowNecromancerGoal extends Goal {
	private static final double SEARCH_RADIUS = 12.0D;
	private static final int MAX_FORMATION_SLOTS = 6;
	private static final double FORMATION_RADIUS = 3.6D;
	private static final double FORMATION_CLAIM_RADIUS = 48.0D;
	private static final double SLOT_REACHED_DISTANCE_SQR = 1.35D * 1.35D;
	private static final int SEARCH_RETRY_TICKS = 160;
	private static final int PATH_RECALCULATE_TICKS = 10;

	private final UndeadKnightEntity knight;
	private final double speedModifier;
	private NecromancerEntity owner;
	private int nextSearchTick;
	private int pathRecalculateTicks;

	public UndeadKnightFollowNecromancerGoal(UndeadKnightEntity knight, double speedModifier) {
		this.knight = knight;
		this.speedModifier = speedModifier;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (!canFollow()) {
			return false;
		}

		owner = getSavedOwner();
		if (owner == null) {
			if (knight.getNecromancerOwnerUuid() != null) {
				return false;
			}

			if (nextSearchTick > 0) {
				nextSearchTick--;
				return false;
			}

			nextSearchTick = SEARCH_RETRY_TICKS;
			OwnerSlot assignment = findNearestAvailableOwner();
			if (assignment != null) {
				owner = assignment.owner;
				knight.setNecromancerOwner(owner, assignment.slot);
			}
		}

		return owner != null && !isAtFormationSlot(owner);
	}

	@Override
	public boolean canContinueToUse() {
		owner = getSavedOwner();
		return canFollow()
			&& owner != null
			&& owner.isAlive()
			&& !isAtFormationSlot(owner);
	}

	@Override
	public void start() {
		pathRecalculateTicks = 0;
	}

	@Override
	public void stop() {
		owner = null;
		pathRecalculateTicks = 0;
		knight.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (owner == null || !owner.isAlive()) {
			knight.clearNecromancerOwner();
			knight.getNavigation().stop();
			return;
		}

		knight.getLookControl().setLookAt(owner, 20.0F, 20.0F);
		if (--pathRecalculateTicks <= 0) {
			pathRecalculateTicks = PATH_RECALCULATE_TICKS;
			Vec3 formationPos = getFormationPosition(owner, knight.getNecromancerFormationSlot());
			knight.getNavigation().moveTo(formationPos.x, formationPos.y, formationPos.z, speedModifier);
		}
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	private boolean canFollow() {
		return knight.isAlive()
			&& !hasCombatTarget()
			&& !knight.isDeathAnimationActive()
			&& !knight.isSoulAbsorbing()
			&& !knight.isEmpowering()
			&& !knight.isReincarnating();
	}

	private boolean hasCombatTarget() {
		LivingEntity target = knight.getTarget();
		return target != null && target.isAlive();
	}

	private NecromancerEntity getSavedOwner() {
		UUID ownerUuid = knight.getNecromancerOwnerUuid();
		if (ownerUuid == null || !(knight.level() instanceof ServerLevel serverLevel)) {
			return null;
		}

		Entity entity = serverLevel.getEntity(ownerUuid);
		if (entity instanceof NecromancerEntity necromancer) {
			if (necromancer.isAlive()) {
				if (ownsUsableFormationSlot(necromancer)) {
					return necromancer;
				}

				int slot = findAvailableSlot(necromancer);
				if (slot != UndeadKnightEntity.NO_NECROMANCER_FORMATION_SLOT) {
					knight.setNecromancerOwner(necromancer, slot);
					return necromancer;
				}
			}
			knight.clearNecromancerOwner();
		}
		return null;
	}

	private OwnerSlot findNearestAvailableOwner() {
		if (!(knight.level() instanceof ServerLevel serverLevel)) {
			return null;
		}

		List<NecromancerEntity> necromancers = serverLevel.getEntitiesOfClass(
			NecromancerEntity.class,
			knight.getBoundingBox().inflate(SEARCH_RADIUS),
			NecromancerEntity::isAlive
		);

		OwnerSlot closest = null;
		double closestDistanceSqr = SEARCH_RADIUS * SEARCH_RADIUS;
		for (NecromancerEntity necromancer : necromancers) {
			double distanceSqr = knight.distanceToSqr(necromancer);
			if (distanceSqr <= closestDistanceSqr) {
				int slot = findAvailableSlot(necromancer);
				if (slot == UndeadKnightEntity.NO_NECROMANCER_FORMATION_SLOT) {
					continue;
				}

				closest = new OwnerSlot(necromancer, slot);
				closestDistanceSqr = distanceSqr;
			}
		}
		return closest;
	}

	private boolean ownsUsableFormationSlot(NecromancerEntity necromancer) {
		int slot = knight.getNecromancerFormationSlot();
		return isValidSlot(slot) && !isSlotTakenByHigherPriorityKnight(necromancer, slot);
	}

	private int findAvailableSlot(NecromancerEntity necromancer) {
		boolean[] occupied = getOccupiedSlots(necromancer);
		int bestSlot = UndeadKnightEntity.NO_NECROMANCER_FORMATION_SLOT;
		double bestDistanceSqr = Double.MAX_VALUE;
		for (int slot = 0; slot < MAX_FORMATION_SLOTS; slot++) {
			if (occupied[slot]) {
				continue;
			}

			double distanceSqr = knight.position().distanceToSqr(getFormationPosition(necromancer, slot));
			if (distanceSqr < bestDistanceSqr) {
				bestSlot = slot;
				bestDistanceSqr = distanceSqr;
			}
		}
		return bestSlot;
	}

	private boolean[] getOccupiedSlots(NecromancerEntity necromancer) {
		boolean[] occupied = new boolean[MAX_FORMATION_SLOTS];
		if (!(knight.level() instanceof ServerLevel serverLevel)) {
			return occupied;
		}

		UUID ownerUuid = necromancer.getUUID();
		List<UndeadKnightEntity> escorts = serverLevel.getEntitiesOfClass(
			UndeadKnightEntity.class,
			necromancer.getBoundingBox().inflate(FORMATION_CLAIM_RADIUS),
			candidate -> candidate != knight && candidate.isAlive() && ownerUuid.equals(candidate.getNecromancerOwnerUuid())
		);

		for (UndeadKnightEntity escort : escorts) {
			int slot = escort.getNecromancerFormationSlot();
			if (isValidSlot(slot)) {
				occupied[slot] = true;
			}
		}
		return occupied;
	}

	private boolean isSlotTakenByHigherPriorityKnight(NecromancerEntity necromancer, int slot) {
		if (!(knight.level() instanceof ServerLevel serverLevel)) {
			return false;
		}

		UUID ownerUuid = necromancer.getUUID();
		List<UndeadKnightEntity> escorts = serverLevel.getEntitiesOfClass(
			UndeadKnightEntity.class,
			necromancer.getBoundingBox().inflate(FORMATION_CLAIM_RADIUS),
			candidate -> candidate != knight
				&& candidate.isAlive()
				&& ownerUuid.equals(candidate.getNecromancerOwnerUuid())
				&& candidate.getNecromancerFormationSlot() == slot
		);

		for (UndeadKnightEntity escort : escorts) {
			if (escort.getUUID().compareTo(knight.getUUID()) < 0) {
				return true;
			}
		}
		return false;
	}

	private boolean isAtFormationSlot(NecromancerEntity necromancer) {
		int slot = knight.getNecromancerFormationSlot();
		return isValidSlot(slot) && knight.position().distanceToSqr(getFormationPosition(necromancer, slot)) <= SLOT_REACHED_DISTANCE_SQR;
	}

	private static Vec3 getFormationPosition(NecromancerEntity necromancer, int slot) {
		double angle = getFormationAngle(necromancer, slot);
		return new Vec3(
			necromancer.getX() + Math.cos(angle) * FORMATION_RADIUS,
			necromancer.getY(),
			necromancer.getZ() + Math.sin(angle) * FORMATION_RADIUS
		);
	}

	private static double getFormationAngle(NecromancerEntity necromancer, int slot) {
		long ownerSeed = necromancer.getUUID().getLeastSignificantBits();
		double ownerOffset = ((ownerSeed & 65535L) / 65536.0D) * Math.PI * 2.0D;
		return ownerOffset + (Math.PI * 2.0D * slot) / MAX_FORMATION_SLOTS;
	}

	private static boolean isValidSlot(int slot) {
		return slot >= 0 && slot < MAX_FORMATION_SLOTS;
	}

	private static final class OwnerSlot {
		private final NecromancerEntity owner;
		private final int slot;

		private OwnerSlot(NecromancerEntity owner, int slot) {
			this.owner = owner;
			this.slot = slot;
		}
	}
}
