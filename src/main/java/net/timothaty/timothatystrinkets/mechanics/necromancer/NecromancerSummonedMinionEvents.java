package net.timothaty.timothatystrinkets.mechanics.necromancer;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.entity.NecromancerEntity;
import net.timothaty.timothatystrinkets.entity.UndeadKnightEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.UUID;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class NecromancerSummonedMinionEvents {
	private static final String NBT_OWNER_UUID = "TimothatysTrinketsNecromancerOwnerUuid";
	private static final int UPDATE_INTERVAL_TICKS = 40;
	private static final double FOLLOW_START_DISTANCE_SQR = 8.0D * 8.0D;
	private static final double MAX_OWNER_DISTANCE_SQR = 48.0D * 48.0D;
	private static final double FOLLOW_SPEED = 1.1D;

	private NecromancerSummonedMinionEvents() {
	}

	public static void markSummonedMob(Mob summoned, NecromancerEntity owner) {
		summoned.getPersistentData().putUUID(NBT_OWNER_UUID, owner.getUUID());
		if (summoned instanceof UndeadKnightEntity knight) {
			knight.setNecromancerOwner(owner);
		}
	}

	public static boolean hasSameNecromancerOwner(LivingEntity first, LivingEntity second) {
		if (first == null || second == null || !first.getPersistentData().hasUUID(NBT_OWNER_UUID) || !second.getPersistentData().hasUUID(NBT_OWNER_UUID)) {
			return false;
		}

		return first.getPersistentData().getUUID(NBT_OWNER_UUID).equals(second.getPersistentData().getUUID(NBT_OWNER_UUID));
	}

	public static boolean isNecromancerOwnerPair(LivingEntity first, LivingEntity second) {
		if (first instanceof NecromancerEntity necromancer)
			return isOwnedByNecromancer(second, necromancer.getUUID());
		if (second instanceof NecromancerEntity necromancer)
			return isOwnedByNecromancer(first, necromancer.getUUID());
		return false;
	}

	public static boolean isOwnedByNecromancer(Entity entity, UUID ownerUuid) {
		return entity != null
			&& ownerUuid != null
			&& entity.getPersistentData().hasUUID(NBT_OWNER_UUID)
			&& ownerUuid.equals(entity.getPersistentData().getUUID(NBT_OWNER_UUID));
	}

	public static int countActiveMinions(NecromancerEntity owner, ServerLevel serverLevel) {
		UUID ownerUuid = owner.getUUID();
		return serverLevel.getEntitiesOfClass(
			Mob.class,
			owner.getBoundingBox().inflate(NecromancerConfig.SUMMONED_MINION_SCAN_RANGE),
			minion -> minion.isAlive() && isOwnedByNecromancer(minion, ownerUuid)
		).size();
	}

	@SubscribeEvent
	public static void onEntityTickPost(EntityTickEvent.Post event) {
		if (!(event.getEntity() instanceof Mob minion)) {
			return;
		}

		Level level = minion.level();
		if (level.isClientSide()
			|| Math.floorMod(minion.tickCount + minion.getId(), UPDATE_INTERVAL_TICKS) != 0
			|| !minion.getPersistentData().hasUUID(NBT_OWNER_UUID)) {
			return;
		}

		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		UUID ownerUuid = minion.getPersistentData().getUUID(NBT_OWNER_UUID);
		Entity ownerEntity = serverLevel.getEntity(ownerUuid);
		if (!(ownerEntity instanceof NecromancerEntity owner) || !owner.isAlive()) {
			minion.getPersistentData().remove(NBT_OWNER_UUID);
			return;
		}

		if (owner.getTarget() == null) {
			standDown(minion);
			followOwnerWhenIdle(minion, owner);
			return;
		}

		shareOwnerTarget(minion, owner);
		followOwnerWhenIdle(minion, owner);
	}

	private static void shareOwnerTarget(Mob minion, NecromancerEntity owner) {
		LivingEntity ownerTarget = owner.getTarget();
		if (!canAssistAgainst(minion, ownerTarget)) {
			return;
		}

		LivingEntity currentTarget = minion.getTarget();
		if (currentTarget == null || !currentTarget.isAlive()) {
			minion.setTarget(ownerTarget);
		}
	}

	private static boolean canAssistAgainst(Mob minion, LivingEntity target) {
		return target != null
			&& target.isAlive()
			&& target != minion
			&& !target.hasEffect(TimothatysTrinketsModMobEffects.UNDEADIFICATION)
			&& !NecromancerAllyHelper.shouldUndeadIgnoreTarget(minion, target)
			&& minion.distanceToSqr(target) <= MAX_OWNER_DISTANCE_SQR
			&& minion.canAttack(target)
			&& minion.canAttackType(target.getType())
			&& !minion.isAlliedTo(target);
	}

	private static void followOwnerWhenIdle(Mob minion, NecromancerEntity owner) {
		if (minion instanceof UndeadKnightEntity knight && knight.getNecromancerOwnerUuid() != null) {
			return;
		}

		LivingEntity currentTarget = minion.getTarget();
		if (currentTarget != null && currentTarget.isAlive()) {
			return;
		}

		double distanceToOwnerSqr = minion.distanceToSqr(owner);
		if (distanceToOwnerSqr > FOLLOW_START_DISTANCE_SQR && distanceToOwnerSqr <= MAX_OWNER_DISTANCE_SQR) {
			minion.getNavigation().moveTo(owner, FOLLOW_SPEED);
		}
	}

	private static void standDown(Mob minion) {
		boolean hadCombatState = minion.getTarget() != null || minion.getLastHurtByMob() != null || minion.isAggressive() || minion.isUsingItem();
		if (!hadCombatState) {
			return;
		}

		minion.setTarget(null);
		minion.setLastHurtByMob(null);
		minion.setAggressive(false);
		if (minion.isUsingItem()) {
			minion.stopUsingItem();
		}
	}
}
