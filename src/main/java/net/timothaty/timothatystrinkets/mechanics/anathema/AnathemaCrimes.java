package net.timothaty.timothatystrinkets.mechanics.anathema;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class AnathemaCrimes {
	private static final double WITNESS_RADIUS_SQR = AnathemaData.CLERIC_WITNESS_RADIUS * AnathemaData.CLERIC_WITNESS_RADIUS;

	private AnathemaCrimes() {
	}

	public static boolean reportCrime(ServerLevel level, Player player, BlockPos crimePos, AnathemaCrime crime) {
		if (!isValidCrime(level, player, crimePos, crime))
			return false;

		List<Villager> clerics = findVillageClerics(level, crimePos);
		if (clerics.isEmpty())
			return false;

		if (!crime.requiresLineOfSight()) {
			if (AnathemaRaidRules.hasActiveRaid(level, crimePos)
					&& findDirectClericWitness(clerics, player, crimePos) == null)
				return false;
			AnathemaHelper.applyCrimeLevel(player);
			return true;
		}

		CrimeObservation observation = observeWitnessedCrime(level, player, crimePos, clerics, null);
		return observation != null && resolveObservedCrime(level, player, crimePos, crime, observation);
	}

	public static CrimeObservation observeWitnessedCrime(ServerLevel level, Player player, BlockPos crimePos) {
		return observeWitnessedCrime(level, player, crimePos, null);
	}

	public static CrimeObservation observeWitnessedCrime(ServerLevel level, Player player, BlockPos crimePos, UUID excludedWitnessId) {
		if (level == null || player == null || crimePos == null || !AnathemaVillageRules.isVillageTerritory(level, crimePos))
			return null;
		return observeWitnessedCrime(level, player, crimePos, findVillageClerics(level, crimePos), excludedWitnessId);
	}

	public static CrimeObservation observeWorkstationCrime(ServerLevel level, Player player, BlockPos crimePos, Villager jobSiteOwner) {
		if (level == null || player == null || crimePos == null || !AnathemaVillageRules.isVillageTerritory(level, crimePos))
			return null;

		List<Villager> clerics = findVillageClerics(level, crimePos);
		if (clerics.isEmpty())
			return null;

		Villager directCleric = findDirectClericWitness(clerics, player, crimePos);
		if (directCleric != null)
			return CrimeObservation.direct(directCleric.getUUID());
		if (AnathemaRaidRules.hasActiveRaid(level, crimePos))
			return null;
		if (jobSiteOwner == null || !jobSiteOwner.isAlive() || jobSiteOwner.isBaby())
			return null;

		Villager recipient = clerics.stream()
			.filter(cleric -> cleric != jobSiteOwner)
			.min(Comparator.comparingDouble(jobSiteOwner::distanceToSqr))
			.orElse(null);
		return recipient == null ? null : CrimeObservation.denunciation(jobSiteOwner.getUUID(), recipient.getUUID());
	}

	public static boolean resolveObservedCrime(ServerLevel level, Player player, BlockPos crimePos, AnathemaCrime crime, CrimeObservation observation) {
		if (!isValidCrime(level, player, crimePos, crime) || observation == null)
			return false;
		if (observation.directClericWitness()) {
			AnathemaHelper.applyCrimeLevel(player);
			return true;
		}
		if (AnathemaRaidRules.hasActiveRaid(level, crimePos))
			return false;

		return AnathemaDenunciationManager.startReport(
			level,
			observation.witnessId(),
			observation.clericId(),
			player.getUUID(),
			crimePos,
			crime
		);
	}

	public static List<Villager> findVillageClerics(ServerLevel level, BlockPos villagePos) {
		if (level == null || villagePos == null || !AnathemaVillageRules.isVillageTerritory(level, villagePos))
			return List.of();

		double radius = AnathemaData.VILLAGE_CLERIC_SEARCH_RADIUS;
		AABB bounds = new AABB(villagePos).inflate(radius, radius * 0.5D, radius);
		return level.getEntitiesOfClass(
			Villager.class,
			bounds,
			villager -> isCleric(villager) && AnathemaVillageRules.isVillageTerritory(level, villager.blockPosition())
		);
	}

	public static Villager findClosestVillageCleric(ServerLevel level, BlockPos villagePos, Villager from, UUID excludedCleric) {
		return findVillageClerics(level, villagePos).stream()
			.filter(cleric -> excludedCleric == null || !cleric.getUUID().equals(excludedCleric))
			.min(Comparator.comparingDouble(cleric -> from == null ? cleric.distanceToSqr(
				villagePos.getX() + 0.5D,
				villagePos.getY() + 0.5D,
				villagePos.getZ() + 0.5D
			) : cleric.distanceToSqr(from)))
			.orElse(null);
	}

	private static CrimeObservation observeWitnessedCrime(ServerLevel level, Player player, BlockPos crimePos, List<Villager> clerics, UUID excludedWitnessId) {
		if (clerics.isEmpty())
			return null;

		Villager directCleric = findDirectClericWitness(clerics, player, crimePos);
		if (directCleric != null)
			return CrimeObservation.direct(directCleric.getUUID());
		if (AnathemaRaidRules.hasActiveRaid(level, crimePos))
			return null;

		AABB witnessBounds = new AABB(crimePos).inflate(AnathemaData.CLERIC_WITNESS_RADIUS);
		List<Villager> witnesses = level.getEntitiesOfClass(
			Villager.class,
			witnessBounds,
			villager -> villager.isAlive()
				&& !villager.isBaby()
				&& (excludedWitnessId == null || !villager.getUUID().equals(excludedWitnessId))
				&& villager.getVillagerData().getProfession() != VillagerProfession.CLERIC
				&& isWithinWitnessRadius(villager, crimePos)
				&& villager.hasLineOfSight(player)
				&& !AnathemaDenunciationManager.isWitnessBusy(villager.getUUID())
		);
		if (witnesses.isEmpty())
			return null;

		Villager witness = witnesses.get(level.getRandom().nextInt(witnesses.size()));
		Villager recipient = clerics.stream().min(Comparator.comparingDouble(witness::distanceToSqr)).orElse(null);
		return recipient == null ? null : CrimeObservation.denunciation(witness.getUUID(), recipient.getUUID());
	}

	private static Villager findDirectClericWitness(List<Villager> clerics, Player player, BlockPos crimePos) {
		return clerics.stream()
			.filter(cleric -> isWithinWitnessRadius(cleric, crimePos) && cleric.hasLineOfSight(player))
			.min(Comparator.comparingDouble(cleric -> cleric.distanceToSqr(player)))
			.orElse(null);
	}

	private static boolean isValidCrime(ServerLevel level, Player player, BlockPos crimePos, AnathemaCrime crime) {
		return level != null
			&& player != null
			&& player.isAlive()
			&& crimePos != null
			&& crime != null
			&& AnathemaVillageRules.isVillageTerritory(level, crimePos);
	}

	private static boolean isCleric(Villager villager) {
		return villager != null
			&& villager.isAlive()
			&& villager.getVillagerData().getProfession() == VillagerProfession.CLERIC;
	}

	private static boolean isWithinWitnessRadius(Villager villager, BlockPos crimePos) {
		return villager.distanceToSqr(
			crimePos.getX() + 0.5D,
			crimePos.getY() + 0.5D,
			crimePos.getZ() + 0.5D
		) <= WITNESS_RADIUS_SQR;
	}

	public record CrimeObservation(UUID witnessId, UUID clericId, boolean directClericWitness) {
		private static CrimeObservation direct(UUID clericId) {
			return new CrimeObservation(null, clericId, true);
		}

		private static CrimeObservation denunciation(UUID witnessId, UUID clericId) {
			return new CrimeObservation(witnessId, clericId, false);
		}
	}
}
