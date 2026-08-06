package net.timothaty.timothatystrinkets.mechanics.striker_acquisition;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.phys.AABB;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.Comparator;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class StrikerAcquisitionEvents {
	private static final double WEAPONSMITH_SEARCH_RADIUS = 64.0D;
	private static final String PLAYER_RAID_ID_KEY = "ttr_striker_reward_raid_id";
	private static final String PLAYER_RAID_DIMENSION_KEY = "ttr_striker_reward_raid_dimension";

	private StrikerAcquisitionEvents() {
	}

	@SubscribeEvent
	public static void onRavagerDeath(LivingDeathEvent event) {
		if (!(event.getEntity() instanceof Ravager ravager)
				|| !(ravager.level() instanceof ServerLevel level)
				|| ravager.getWave() <= 0)
			return;

		ServerPlayer killer = resolvePlayerKiller(event);
		if (killer == null || killer.isSpectator())
			return;

		Raid raid = ravager.getCurrentRaid();
		if (raid == null)
			raid = level.getRaidAt(ravager.blockPosition());
		if (raid == null || !raid.isActive() || raid.isStopped() || raid.isOver())
			return;
		if (hasRewardAssignment(killer, level, raid))
			return;

		Villager weaponsmith = findNearestEligibleWeaponsmith(level, raid, ravager);
		if (weaponsmith == null
				|| !StrikerCommissionData.assignRaidCommission(level, weaponsmith, killer.getUUID(), raid))
			return;

		markRewardAssignment(killer, level, raid);
	}

	private static ServerPlayer resolvePlayerKiller(LivingDeathEvent event) {
		Entity attacker = event.getSource().getEntity();
		if (attacker instanceof ServerPlayer player)
			return player;
		if (event.getSource().getDirectEntity() instanceof Projectile projectile
				&& projectile.getOwner() instanceof ServerPlayer player)
			return player;
		return null;
	}

	private static Villager findNearestEligibleWeaponsmith(ServerLevel level, Raid raid, Ravager ravager) {
		double minX = Math.min(raid.getCenter().getX(), ravager.getX()) - WEAPONSMITH_SEARCH_RADIUS;
		double minY = Math.min(raid.getCenter().getY(), ravager.getY()) - WEAPONSMITH_SEARCH_RADIUS;
		double minZ = Math.min(raid.getCenter().getZ(), ravager.getZ()) - WEAPONSMITH_SEARCH_RADIUS;
		double maxX = Math.max(raid.getCenter().getX(), ravager.getX()) + WEAPONSMITH_SEARCH_RADIUS;
		double maxY = Math.max(raid.getCenter().getY(), ravager.getY()) + WEAPONSMITH_SEARCH_RADIUS;
		double maxZ = Math.max(raid.getCenter().getZ(), ravager.getZ()) + WEAPONSMITH_SEARCH_RADIUS;
		AABB bounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

		return level.getEntitiesOfClass(
				Villager.class,
				bounds,
				villager -> StrikerCommissionData.isEligibleForAssignment(level, villager)
		).stream().min(
				Comparator.comparingInt((Villager villager) -> villageAffinity(level, raid, villager))
						.thenComparingDouble(villager -> villager.distanceToSqr(ravager))
		).orElse(null);
	}

	private static int villageAffinity(ServerLevel level, Raid raid, Villager villager) {
		if (level.getRaidAt(villager.blockPosition()) == raid)
			return 0;
		return level.isVillage(villager.blockPosition()) ? 1 : 2;
	}

	private static boolean hasRewardAssignment(ServerPlayer player, ServerLevel level, Raid raid) {
		return player.getPersistentData().getInt(PLAYER_RAID_ID_KEY) == raid.getId()
				&& player.getPersistentData().getString(PLAYER_RAID_DIMENSION_KEY)
						.equals(level.dimension().location().toString());
	}

	private static void markRewardAssignment(ServerPlayer player, ServerLevel level, Raid raid) {
		player.getPersistentData().putInt(PLAYER_RAID_ID_KEY, raid.getId());
		player.getPersistentData().putString(PLAYER_RAID_DIMENSION_KEY, level.dimension().location().toString());
	}
}
