package net.timothaty.timothatystrinkets.mechanics.pillagers_coin;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaCrime;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaCrimes;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaDenunciationManager;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaHelper;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaRaidRules;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaVillagerFearEvents;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.VillagerGiftThrower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class PillagersCoinExtortionManager {
	private static final Map<UUID, Session> ACTIVE_SESSIONS = new HashMap<>();

	private PillagersCoinExtortionManager() {
	}

	public static boolean hasSession(Villager villager) {
		return villager != null && ACTIVE_SESSIONS.containsKey(villager.getUUID());
	}

	public static boolean startAttempt(ServerLevel level, Villager villager, ServerPlayer robber) {
		if (level == null || villager == null || robber == null || ACTIVE_SESSIONS.containsKey(villager.getUUID()))
			return false;

		BlockPos crimePos = villager.blockPosition();
		boolean wasSleeping = villager.isSleeping();
		BlockPos originalBed = villager.getSleepingPos().map(BlockPos::immutable).orElse(null);
		if (wasSleeping) {
			villager.stopSleeping();
			villager.getBrain().stopAll(level, villager);
			villager.getBrain().setActiveActivityIfPossible(Activity.IDLE);
		}

		int availableStock = PillagersCoinVillagerStockData.getAvailableWholeStock(level, villager);
		boolean witnessed = observeCrime(level, villager, robber, crimePos);
		if (availableStock <= 0) {
			finishFailure(level, villager, robber, witnessed);
			return true;
		}

		long now = level.getGameTime();
		ACTIVE_SESSIONS.put(villager.getUUID(), new Session(
			level.dimension(),
			villager.getUUID(),
			robber.getUUID(),
			now,
			now + PillagersCoinData.EXTORTION_DURATION_TICKS,
			witnessed,
			PillagersCoinHelper.calculateSuccessChance(robber, witnessed),
			wasSleeping,
			originalBed
		));
		startVisual(villager);
		maintainVillager(villager, robber);
		return true;
	}

	public static boolean endForIncomingDamage(ServerLevel level, Villager villager, DamageSource source) {
		Session session = ACTIVE_SESSIONS.remove(villager.getUUID());
		if (session == null)
			return false;

		ServerPlayer robber = level.getServer().getPlayerList().getPlayer(session.robberId);
		boolean robberCancelled = source != null
			&& source.getEntity() != null
			&& source.getEntity().getUUID().equals(session.robberId);
		if (robberCancelled && robber != null)
			finishFailure(level, villager, robber, session.witnessed);
		else
			cleanupVillager(level, villager);
		return true;
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (ACTIVE_SESSIONS.isEmpty())
			return;

		MinecraftServer server = event.getServer();
		Iterator<Session> iterator = ACTIVE_SESSIONS.values().iterator();
		while (iterator.hasNext()) {
			Session session = iterator.next();
			ServerLevel level = server.getLevel(session.dimension);
			if (level == null) {
				iterator.remove();
				continue;
			}

			Entity victimEntity = level.getEntity(session.villagerId);
			ServerPlayer robber = server.getPlayerList().getPlayer(session.robberId);
			if (!(victimEntity instanceof Villager villager)
					|| !villager.isAlive()
					|| villager.isRemoved()
					|| robber == null
					|| !robber.isAlive()
					|| robber.isRemoved()
					|| !robber.serverLevel().dimension().equals(session.dimension)) {
				if (victimEntity instanceof Villager villager)
					cleanupVillager(level, villager);
				iterator.remove();
				continue;
			}

			if (!tickSession(level, villager, robber, session))
				iterator.remove();
		}
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		cancelSessionsForRobber(event.getEntity().getServer(), event.getEntity().getUUID());
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		cancelSessionsForRobber(event.getEntity().getServer(), event.getEntity().getUUID());
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		cancelSessionsForRobber(event.getOriginal().getServer(), event.getOriginal().getUUID());
	}

	@SubscribeEvent
	public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
		if (!(event.getEntity() instanceof Villager villager) || !(event.getLevel() instanceof ServerLevel level))
			return;
		Session session = ACTIVE_SESSIONS.remove(villager.getUUID());
		if (session != null)
			cleanupVillager(level, villager);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		ACTIVE_SESSIONS.clear();
	}

	private static boolean tickSession(ServerLevel level, Villager villager, ServerPlayer robber, Session session) {
		maintainVillager(villager, robber);
		emitSweat(level, villager, session);
		return session.phase == Phase.EXTORTING
			? tickExtorting(level, villager, robber, session)
			: tickDispensing(level, villager, robber, session);
	}

	private static boolean tickExtorting(ServerLevel level, Villager villager, ServerPlayer robber, Session session) {
		if (robber.distanceToSqr(villager) > PillagersCoinData.EXTORTION_MAX_DISTANCE_SQR) {
			session.outOfRangeTicks++;
			if (session.outOfRangeTicks >= PillagersCoinData.OUT_OF_RANGE_GRACE_TICKS) {
				finishFailure(level, villager, robber, session.witnessed);
				return false;
			}
		} else {
			session.outOfRangeTicks = 0;
		}

		if (level.getGameTime() < session.endTime)
			return true;
		if (level.getRandom().nextDouble() >= session.successChance) {
			finishFailure(level, villager, robber, session.witnessed);
			return false;
		}
		return beginDispensing(level, villager, session);
	}

	private static boolean beginDispensing(ServerLevel level, Villager villager, Session session) {
		int availableStock = PillagersCoinVillagerStockData.getAvailableWholeStock(level, villager);
		List<ItemStack> loot = PillagersCoinHelper.rollExtortionLoot(level, villager, availableStock);
		if (loot.isEmpty()) {
			ServerPlayer robber = level.getServer().getPlayerList().getPlayer(session.robberId);
			if (robber != null)
				applyCooldown(robber, session.witnessed);
			cleanupVillager(level, villager);
			return false;
		}

		session.phase = Phase.DISPENSING;
		session.pendingLoot = loot;
		session.nextDropTime = level.getGameTime() + 1L;
		level.playSound(
			null,
			villager.blockPosition(),
			TimothatysTrinketsModSounds.EXTORTION_SUCCESS.get(),
			SoundSource.NEUTRAL,
			1.0F,
			1.0F + level.getRandom().nextFloat() * 0.2F
		);
		return true;
	}

	private static boolean tickDispensing(ServerLevel level, Villager villager, ServerPlayer robber, Session session) {
		if (level.getGameTime() < session.nextDropTime)
			return true;
		if (session.nextLootIndex >= session.pendingLoot.size()) {
			finishSuccess(level, villager, robber, session);
			return false;
		}
		if (!PillagersCoinVillagerStockData.tryConsumeOneStock(level, villager)) {
			if (session.emittedDrops > 0)
				finishSuccess(level, villager, robber, session);
			else {
				applyCooldown(robber, session.witnessed);
				cleanupVillager(level, villager);
			}
			return false;
		}

		throwLootStack(level, villager, robber, session.pendingLoot.get(session.nextLootIndex));
		session.nextLootIndex++;
		session.emittedDrops++;
		if (session.nextLootIndex >= session.pendingLoot.size()) {
			finishSuccess(level, villager, robber, session);
			return false;
		}
		session.nextDropTime = level.getGameTime() + PillagersCoinData.DROP_INTERVAL_TICKS;
		return true;
	}

	private static void throwLootStack(ServerLevel level, Villager villager, ServerPlayer robber, ItemStack stack) {
		VillagerGiftThrower.throwStack(level, villager, robber, stack, net.minecraft.sounds.SoundEvents.DYE_USE);
	}

	private static void finishSuccess(ServerLevel level, Villager villager, ServerPlayer robber, Session session) {
		PillagersCoinVillagerFearData.addFear(villager, robber.getUUID());
		boolean returnToSleep = session.wasSleeping && !session.witnessed;
		if (!returnToSleep)
			PillagersCoinVillagerFearEvents.activateFear(villager, robber);
		applyCooldown(robber, session.witnessed);
		cleanupVillager(level, villager);
		if (returnToSleep)
			tryReturnToSleep(level, villager, session.originalBed);
	}

	private static void finishFailure(ServerLevel level, Villager victim, ServerPlayer robber, boolean witnessed) {
		handleFailure(level, victim, robber, witnessed);
		applyCooldown(robber, witnessed);
		cleanupVillager(level, victim);
	}

	private static boolean observeCrime(ServerLevel level, Villager victim, ServerPlayer robber, BlockPos crimePos) {
		AnathemaCrimes.CrimeObservation observation = AnathemaCrimes.observeWitnessedCrime(
			level,
			robber,
			crimePos,
			victim.getUUID()
		);
		if (observation != null)
			AnathemaCrimes.resolveObservedCrime(level, robber, crimePos, AnathemaCrime.EXTORTION, observation);
		return observation != null;
	}

	private static void handleFailure(ServerLevel level, Villager victim, ServerPlayer robber, boolean witnessed) {
		PillagersCoinVillagerFearData.addFear(victim, robber.getUUID());
		PillagersCoinVillagerFearEvents.activateFear(victim, robber);
		if (witnessed)
			return;
		if (AnathemaRaidRules.hasActiveRaid(level, victim.blockPosition())) {
			if (victim.getVillagerData().getProfession() == VillagerProfession.CLERIC
					&& victim.hasLineOfSight(robber))
				AnathemaHelper.applyCrimeLevel(robber);
			return;
		}

		if (victim.getVillagerData().getProfession() == VillagerProfession.CLERIC) {
			AnathemaHelper.applyCrimeLevel(robber);
			return;
		}
		Villager cleric = AnathemaCrimes.findClosestVillageCleric(level, victim.blockPosition(), victim, null);
		if (cleric != null) {
			AnathemaDenunciationManager.startReport(
				level,
				victim.getUUID(),
				cleric.getUUID(),
				robber.getUUID(),
				victim.blockPosition(),
				AnathemaCrime.EXTORTION
			);
		}
	}

	private static void applyCooldown(ServerPlayer robber, boolean witnessed) {
		robber.getCooldowns().addCooldown(
			TimothatysTrinketsModItems.PILLAGERS_COIN.get(),
			witnessed ? PillagersCoinData.WITNESSED_COOLDOWN_TICKS : PillagersCoinData.UNWITNESSED_COOLDOWN_TICKS
		);
	}

	private static void emitSweat(ServerLevel level, Villager villager, Session session) {
		long now = level.getGameTime();
		if (!villager.isSleeping() && now >= session.nextSweatTime) {
			level.broadcastEntityEvent(villager, (byte) 42);
			session.nextSweatTime = now + PillagersCoinData.SWEAT_INTERVAL_TICKS;
		}
	}

	private static void maintainVillager(Villager villager, ServerPlayer robber) {
		villager.getBrain().setActiveActivityIfPossible(Activity.IDLE);
		villager.getNavigation().stop();
		villager.getBrain().eraseMemory(MemoryModuleType.PATH);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(robber, true));
		Vec3 movement = villager.getDeltaMovement();
		villager.setDeltaMovement(0.0D, movement.y, 0.0D);
		villager.getLookControl().setLookAt(robber, 30.0F, 30.0F);
	}

	private static void cleanupVillager(ServerLevel level, Villager villager) {
		stopVisual(villager);
		villager.getNavigation().stop();
		villager.getBrain().eraseMemory(MemoryModuleType.PATH);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
		long now = level.getGameTime();
		if (PillagersCoinVillagerFearEvents.applyActiveFear(villager, now)) {
			return;
		}
		if (AnathemaVillagerFearEvents.shouldHideFromAnathema(villager, now))
			villager.getBrain().setActiveActivityIfPossible(Activity.HIDE);
		else
			villager.getBrain().updateActivityFromSchedule(level.getDayTime(), now);
	}

	private static boolean tryReturnToSleep(ServerLevel level, Villager villager, BlockPos bedPos) {
		if (bedPos == null || !level.hasChunkAt(bedPos) || !bedPos.closerToCenterThan(villager.position(), 3.0D))
			return false;
		BlockState state = level.getBlockState(bedPos);
		if (!state.isBed(level, bedPos, villager)
				|| state.hasProperty(BedBlock.OCCUPIED) && state.getValue(BedBlock.OCCUPIED))
			return false;

		villager.getBrain().stopAll(level, villager);
		villager.getBrain().setMemory(MemoryModuleType.HOME, GlobalPos.of(level.dimension(), bedPos));
		villager.getBrain().eraseMemory(MemoryModuleType.LAST_WOKEN);
		villager.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
		villager.getBrain().eraseMemory(MemoryModuleType.PATH);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		PillagersCoinVillagerSleepEvents.beginReturnToSleepSettle(level, villager);
		return true;
	}

	private static void startVisual(Villager villager) {
		if (villager instanceof PillagersCoinVillagerRuntimeState state)
			state.timothatys_trinkets$startExtortionVisual();
	}

	private static void stopVisual(Villager villager) {
		if (villager instanceof PillagersCoinVillagerRuntimeState state)
			state.timothatys_trinkets$stopExtortionVisual();
	}

	private static void cancelSessionsForRobber(MinecraftServer server, UUID robberId) {
		if (server == null)
			return;
		Iterator<Session> iterator = ACTIVE_SESSIONS.values().iterator();
		while (iterator.hasNext()) {
			Session session = iterator.next();
			if (!session.robberId.equals(robberId))
				continue;
			ServerLevel level = server.getLevel(session.dimension);
			if (level != null && level.getEntity(session.villagerId) instanceof Villager villager)
				cleanupVillager(level, villager);
			iterator.remove();
		}
	}

	private enum Phase {
		EXTORTING,
		DISPENSING
	}

	private static final class Session {
		private final ResourceKey<Level> dimension;
		private final UUID villagerId;
		private final UUID robberId;
		@SuppressWarnings("unused")
		private final long startTime;
		private final long endTime;
		private final boolean witnessed;
		private final double successChance;
		private final boolean wasSleeping;
		private final BlockPos originalBed;
		private Phase phase = Phase.EXTORTING;
		private int outOfRangeTicks;
		private long nextSweatTime;
		private List<ItemStack> pendingLoot = List.of();
		private int nextLootIndex;
		private int emittedDrops;
		private long nextDropTime;

		private Session(ResourceKey<Level> dimension, UUID villagerId, UUID robberId, long startTime, long endTime, boolean witnessed, double successChance, boolean wasSleeping, BlockPos originalBed) {
			this.dimension = dimension;
			this.villagerId = villagerId;
			this.robberId = robberId;
			this.startTime = startTime;
			this.endTime = endTime;
			this.witnessed = witnessed;
			this.successChance = successChance;
			this.wasSleeping = wasSleeping;
			this.originalBed = originalBed;
			this.nextSweatTime = startTime + PillagersCoinData.SWEAT_INTERVAL_TICKS;
		}
	}
}
