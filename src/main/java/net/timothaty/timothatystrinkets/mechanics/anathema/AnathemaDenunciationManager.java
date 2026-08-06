package net.timothaty.timothatystrinkets.mechanics.anathema;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class AnathemaDenunciationManager {
	private static final int REPORT_TIMEOUT_TICKS = 30 * 20;
	private static final int CONVERSATION_TICKS = 3 * 20;
	private static final int PATH_REFRESH_TICKS = 10;
	private static final int STUCK_REASSIGN_TICKS = 5 * 20;
	private static final double TALK_DISTANCE_SQR = 2.5D * 2.5D;
	private static final float WALK_SPEED = 0.7F;

	private static final Map<UUID, Report> ACTIVE_REPORTS = new HashMap<>();
	private static final Map<UUID, PendingJobSiteReport> PENDING_JOB_SITE_REPORTS = new HashMap<>();

	private AnathemaDenunciationManager() {
	}

	public static boolean isWitnessBusy(UUID witnessId) {
		return witnessId != null && (ACTIVE_REPORTS.containsKey(witnessId) || PENDING_JOB_SITE_REPORTS.containsKey(witnessId));
	}

	public static boolean scheduleJobSiteReport(ServerLevel level, UUID ownerId, UUID clericId, UUID offenderId, BlockPos crimePos) {
		if (level == null || ownerId == null || clericId == null || offenderId == null || crimePos == null)
			return false;
		if (AnathemaRaidRules.hasActiveRaid(level, crimePos))
			return false;
		if (PENDING_JOB_SITE_REPORTS.containsKey(ownerId))
			return false;

		long now = level.getGameTime();
		PENDING_JOB_SITE_REPORTS.put(ownerId, new PendingJobSiteReport(
			level.dimension(),
			ownerId,
			clericId,
			offenderId,
			crimePos.immutable(),
			now + REPORT_TIMEOUT_TICKS
		));
		return true;
	}

	public static boolean startReport(ServerLevel level, UUID witnessId, UUID clericId, UUID offenderId, BlockPos crimePos, AnathemaCrime crime) {
		if (level == null || witnessId == null || clericId == null || offenderId == null || crimePos == null || crime == null)
			return false;
		if (AnathemaRaidRules.hasActiveRaid(level, crimePos))
			return false;
		Report existingReport = findReportForOffender(level.dimension(), offenderId);
		if (existingReport != null) {
			existingReport.pendingCrimeCount = Math.min(AnathemaData.MAX_LEVEL, existingReport.pendingCrimeCount + 1);
			return true;
		}
		if (ACTIVE_REPORTS.containsKey(witnessId))
			return false;

		Entity witnessEntity = level.getEntity(witnessId);
		if (!(witnessEntity instanceof Villager witness) || !witness.isAlive())
			return false;

		long now = level.getGameTime();
		ACTIVE_REPORTS.put(witnessId, new Report(
			level.dimension(),
			witnessId,
			clericId,
			offenderId,
			crimePos.immutable(),
			crime,
			now + REPORT_TIMEOUT_TICKS
		));
		return true;
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (ACTIVE_REPORTS.isEmpty() && PENDING_JOB_SITE_REPORTS.isEmpty())
			return;

		MinecraftServer server = event.getServer();
		tickPendingJobSiteReports(server);
		Iterator<Report> iterator = ACTIVE_REPORTS.values().iterator();
		while (iterator.hasNext()) {
			Report report = iterator.next();
			if (!tickReport(server, report)) {
				cleanupReport(server, report);
				iterator.remove();
			}
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		ACTIVE_REPORTS.clear();
		PENDING_JOB_SITE_REPORTS.clear();
	}

	private static void tickPendingJobSiteReports(MinecraftServer server) {
		Iterator<PendingJobSiteReport> iterator = PENDING_JOB_SITE_REPORTS.values().iterator();
		while (iterator.hasNext()) {
			PendingJobSiteReport pending = iterator.next();
			ServerLevel level = server.getLevel(pending.dimension);
			if (level == null || level.getGameTime() > pending.expiresAt) {
				iterator.remove();
				continue;
			}
			if (AnathemaRaidRules.hasActiveRaid(level, pending.crimePos)) {
				cleanupVillager(level.getEntity(pending.ownerId), level);
				cleanupVillager(level.getEntity(pending.clericId), level);
				iterator.remove();
				continue;
			}

			Entity ownerEntity = level.getEntity(pending.ownerId);
			if (ownerEntity == null)
				continue;
			if (!(ownerEntity instanceof Villager owner) || !owner.isAlive()) {
				iterator.remove();
				continue;
			}
			if (owner.getVillagerData().getProfession() != VillagerProfession.NONE)
				continue;

			if (startReport(level, pending.ownerId, pending.clericId, pending.offenderId, pending.crimePos, AnathemaCrime.WORKSTATION_DESTRUCTION))
				iterator.remove();
		}
	}

	private static boolean tickReport(MinecraftServer server, Report report) {
		ServerLevel level = server.getLevel(report.dimension);
		if (level == null || level.getGameTime() > report.expiresAt)
			return false;
		if (AnathemaRaidRules.hasActiveRaid(level, report.crimePos))
			return false;

		Entity witnessEntity = level.getEntity(report.witnessId);
		ServerPlayer offender = server.getPlayerList().getPlayer(report.offenderId);
		if (!(witnessEntity instanceof Villager witness) || !witness.isAlive() || offender == null)
			return false;

		Villager cleric = resolveCleric(level, witness, report);
		if (cleric == null)
			return false;

		long now = level.getGameTime();
		double distanceSqr = witness.distanceToSqr(cleric);
		if (distanceSqr > TALK_DISTANCE_SQR) {
			report.conversationStartedAt = -1L;
			if (distanceSqr + 1.0D < report.bestDistanceSqr) {
				report.bestDistanceSqr = distanceSqr;
				report.lastProgressAt = now;
			} else if (now - report.lastProgressAt >= STUCK_REASSIGN_TICKS) {
				Villager replacement = AnathemaCrimes.findClosestVillageCleric(level, report.crimePos, witness, cleric.getUUID());
				report.lastProgressAt = now;
				report.bestDistanceSqr = Double.MAX_VALUE;
				if (replacement != null) {
					report.clericId = replacement.getUUID();
					return true;
				}
			}
			if (now >= report.nextPathRefreshAt) {
				report.nextPathRefreshAt = now + PATH_REFRESH_TICKS;
				guideWitnessToCleric(witness, cleric);
			}
			return true;
		}

		if (report.conversationStartedAt < 0L) {
			report.conversationStartedAt = now;
			clearMovement(witness);
			clearMovement(cleric);
		}
		maintainConversation(level, witness, cleric, now - report.conversationStartedAt);
		if (now - report.conversationStartedAt < CONVERSATION_TICKS)
			return true;

		AnathemaHelper.setLevel(offender, AnathemaHelper.getLevel(offender) + report.pendingCrimeCount);
		level.broadcastEntityEvent(witness, (byte) 13);
		level.playSound(null, cleric.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.9F, 1.0F);
		return false;
	}

	private static Villager resolveCleric(ServerLevel level, Villager witness, Report report) {
		Entity entity = level.getEntity(report.clericId);
		if (entity instanceof Villager cleric
				&& cleric.isAlive()
				&& cleric.getVillagerData().getProfession() == VillagerProfession.CLERIC)
			return cleric;

		Villager replacement = AnathemaCrimes.findClosestVillageCleric(level, report.crimePos, witness, report.clericId);
		if (replacement != null)
			report.clericId = replacement.getUUID();
		return replacement;
	}

	private static void guideWitnessToCleric(Villager witness, Villager cleric) {
		witness.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(cleric, WALK_SPEED, 2));
		witness.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(cleric, true));
		witness.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, cleric);
	}

	private static void maintainConversation(ServerLevel level, Villager witness, Villager cleric, long conversationTicks) {
		clearMovement(witness);
		clearMovement(cleric);
		if (conversationTicks % 5L == 0L) {
			witness.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(cleric, true));
			cleric.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(witness, true));
			witness.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, cleric);
			cleric.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, witness);
		}
		witness.getLookControl().setLookAt(cleric, 30.0F, 30.0F);
		cleric.getLookControl().setLookAt(witness, 30.0F, 30.0F);

		if (conversationTicks % 20L == 0L) {
			Villager speaker = (conversationTicks / 20L) % 2L == 0L ? witness : cleric;
			level.playSound(null, speaker.blockPosition(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 0.6F, 0.95F + level.getRandom().nextFloat() * 0.1F);
		}
	}

	private static void clearMovement(Villager villager) {
		villager.getNavigation().stop();
		villager.getBrain().eraseMemory(MemoryModuleType.PATH);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
	}

	private static void cleanupReport(MinecraftServer server, Report report) {
		ServerLevel level = server.getLevel(report.dimension);
		if (level == null)
			return;
		cleanupVillager(level.getEntity(report.witnessId), level);
		if (!isClericHandlingAnotherReport(report))
			cleanupVillager(level.getEntity(report.clericId), level);
	}

	private static boolean isClericHandlingAnotherReport(Report completedReport) {
		for (Report report : ACTIVE_REPORTS.values()) {
			if (report != completedReport && report.clericId.equals(completedReport.clericId))
				return true;
		}
		return false;
	}

	private static Report findReportForOffender(ResourceKey<Level> dimension, UUID offenderId) {
		for (Report report : ACTIVE_REPORTS.values()) {
			if (report.dimension.equals(dimension) && report.offenderId.equals(offenderId))
				return report;
		}
		return null;
	}

	private static void cleanupVillager(Entity entity, ServerLevel level) {
		if (!(entity instanceof Villager villager))
			return;
		villager.getNavigation().stop();
		villager.getBrain().eraseMemory(MemoryModuleType.PATH);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
		villager.getBrain().updateActivityFromSchedule(level.getDayTime(), level.getGameTime());
	}

	private static final class Report {
		private final ResourceKey<Level> dimension;
		private final UUID witnessId;
		private UUID clericId;
		private final UUID offenderId;
		private final BlockPos crimePos;
		@SuppressWarnings("unused")
		private final AnathemaCrime crime;
		private final long expiresAt;
		private long nextPathRefreshAt;
		private long conversationStartedAt = -1L;
		private long lastProgressAt;
		private double bestDistanceSqr = Double.MAX_VALUE;
		private int pendingCrimeCount = 1;

		private Report(ResourceKey<Level> dimension, UUID witnessId, UUID clericId, UUID offenderId, BlockPos crimePos, AnathemaCrime crime, long expiresAt) {
			this.dimension = dimension;
			this.witnessId = witnessId;
			this.clericId = clericId;
			this.offenderId = offenderId;
			this.crimePos = crimePos;
			this.crime = crime;
			this.expiresAt = expiresAt;
			this.lastProgressAt = expiresAt - REPORT_TIMEOUT_TICKS;
		}
	}

	private record PendingJobSiteReport(
		ResourceKey<Level> dimension,
		UUID ownerId,
		UUID clericId,
		UUID offenderId,
		BlockPos crimePos,
		long expiresAt
	) {
	}
}
