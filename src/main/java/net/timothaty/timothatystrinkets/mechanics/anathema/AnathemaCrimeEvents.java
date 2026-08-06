package net.timothaty.timothatystrinkets.mechanics.anathema;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class AnathemaCrimeEvents {
	private static final Map<UUID, ChestSession> PENDING_CHESTS = new HashMap<>();
	private static final Map<UUID, ChestSession> OPEN_CHESTS = new HashMap<>();

	private AnathemaCrimeEvents() {
	}

	@SubscribeEvent
	public static void onVillagerKilled(LivingDeathEvent event) {
		if (!(event.getSource().getEntity() instanceof Player player))
			return;
		if (!(event.getEntity().level() instanceof ServerLevel level))
			return;

		if (event.getEntity() instanceof Villager victim) {
			if (victim.getVillagerData().getProfession() == VillagerProfession.CLERIC
					&& AnathemaVillageRules.isVillageTerritory(level, victim.blockPosition())) {
				if (victim.hasLineOfSight(player)) {
					AnathemaHelper.applyCrimeLevel(player);
					return;
				}
				if (AnathemaRaidRules.hasActiveRaid(level, victim.blockPosition()))
					return;
				if (AnathemaCrimes.findVillageClerics(level, victim.blockPosition()).isEmpty()) {
					AnathemaHelper.applyCrimeLevel(player);
					return;
				}
			}

			AnathemaCrimes.reportCrime(level, player, victim.blockPosition(), AnathemaCrime.VILLAGER_MURDER);
			return;
		}

		if (event.getEntity() instanceof IronGolem golem && !golem.isPlayerCreated())
			AnathemaCrimes.reportCrime(level, player, golem.blockPosition(), AnathemaCrime.IRON_GOLEM_MURDER);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onBlockBroken(BlockEvent.BreakEvent event) {
		if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level))
			return;

		BlockPos pos = event.getPos();
		BlockState state = event.getState();
		boolean trackableBlock = state.getBlock() instanceof ChestBlock
			|| state.is(AnathemaVillageRules.VILLAGE_WORKSTATIONS)
			|| state.isFlammable(level, pos, Direction.UP);
		if (!trackableBlock)
			return;
		if (!AnathemaVillageRules.isVillageTerritory(level, pos))
			return;
		Player player = event.getPlayer();
		AnathemaVillageClaims claims = AnathemaVillageClaims.get(level);
		boolean playerPlaced = claims.isPlayerPlaced(pos);

		if (!playerPlaced && AnathemaVillageRules.isVillageWorkstation(level, pos, state)) {
			Villager owner = findJobSiteOwner(level, pos);
			AnathemaCrimes.CrimeObservation observation = AnathemaCrimes.observeWorkstationCrime(level, player, pos, owner);
			if (observation != null) {
				if (observation.directClericWitness()) {
					AnathemaCrimes.resolveObservedCrime(level, player, pos, AnathemaCrime.WORKSTATION_DESTRUCTION, observation);
				} else {
					AnathemaDenunciationManager.scheduleJobSiteReport(
						level,
						observation.witnessId(),
						observation.clericId(),
						player.getUUID(),
						pos
					);
				}
			}
		}

		claims.unmark(pos);
	}

	@SubscribeEvent
	public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
		if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Player))
			return;

		BlockPos pos = event.getPos();
		BlockState state = event.getPlacedBlock();
		boolean tracked = state.getBlock() instanceof ChestBlock
			|| state.is(AnathemaVillageRules.VILLAGE_WORKSTATIONS)
			|| state.isFlammable(level, pos, Direction.UP);
		if (!tracked || !AnathemaVillageRules.isVillageTerritory(level, pos))
			return;
		AnathemaVillageClaims.get(level).markPlayerPlaced(pos);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onVillageChestClicked(PlayerInteractEvent.RightClickBlock event) {
		if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level))
			return;

		Player player = event.getEntity();
		PENDING_CHESTS.remove(player.getUUID());
		BlockPos pos = event.getPos();
		if (!AnathemaVillageRules.isVillageChest(level, pos))
			return;
		AnathemaCrimes.CrimeObservation observation = AnathemaCrimes.observeWitnessedCrime(level, player, pos);
		if (observation == null)
			return;

		Container container = getChestContainer(level, pos);
		if (container != null)
			PENDING_CHESTS.put(player.getUUID(), new ChestSession(level, pos.immutable(), snapshot(container), level.getGameTime(), observation));
	}

	@SubscribeEvent
	public static void onContainerOpened(PlayerContainerEvent.Open event) {
		Player player = event.getEntity();
		ChestSession pending = PENDING_CHESTS.remove(player.getUUID());
		if (pending != null && pending.level == player.level() && pending.openedAt + 2L >= player.level().getGameTime())
			OPEN_CHESTS.put(player.getUUID(), pending);
		else
			OPEN_CHESTS.remove(player.getUUID());
	}

	@SubscribeEvent
	public static void onContainerClosed(PlayerContainerEvent.Close event) {
		Player player = event.getEntity();
		ChestSession session = OPEN_CHESTS.remove(player.getUUID());
		if (session == null || session.level != player.level())
			return;

		Container current = getChestContainer(session.level, session.pos);
		if (current == null || !hasRemovedOriginalItems(session.originalItems, current))
			return;

		AnathemaCrimes.resolveObservedCrime(session.level, player, session.pos, AnathemaCrime.THEFT, session.observation);
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		UUID playerId = event.getEntity().getUUID();
		PENDING_CHESTS.remove(playerId);
		OPEN_CHESTS.remove(playerId);
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		UUID playerId = event.getOriginal().getUUID();
		PENDING_CHESTS.remove(playerId);
		OPEN_CHESTS.remove(playerId);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		PENDING_CHESTS.clear();
		OPEN_CHESTS.clear();
	}

	private static Container getChestContainer(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof ChestBlock chestBlock))
			return null;
		return ChestBlock.getContainer(chestBlock, state, level, pos, false);
	}

	private static List<ItemStack> snapshot(Container container) {
		List<ItemStack> result = new ArrayList<>(container.getContainerSize());
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (!stack.isEmpty())
				result.add(stack.copy());
		}
		return result;
	}

	private static boolean hasRemovedOriginalItems(List<ItemStack> original, Container current) {
		for (ItemStack originalStack : original) {
			int currentCount = 0;
			for (int slot = 0; slot < current.getContainerSize(); slot++) {
				ItemStack currentStack = current.getItem(slot);
				if (ItemStack.isSameItemSameComponents(originalStack, currentStack))
					currentCount += currentStack.getCount();
			}
			if (currentCount < totalOriginalCount(original, originalStack))
				return true;
		}
		return false;
	}

	private static int totalOriginalCount(List<ItemStack> original, ItemStack needle) {
		int count = 0;
		for (ItemStack stack : original) {
			if (ItemStack.isSameItemSameComponents(needle, stack))
				count += stack.getCount();
		}
		return count;
	}

	private static Villager findJobSiteOwner(ServerLevel level, BlockPos jobSitePos) {
		AABB bounds = new AABB(jobSitePos).inflate(64.0D, 32.0D, 64.0D);
		return level.getEntitiesOfClass(
			Villager.class,
			bounds,
			villager -> villager.isAlive()
				&& villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
					.map(jobSite -> jobSite.dimension().equals(level.dimension()) && jobSite.pos().equals(jobSitePos))
					.orElse(false)
		).stream().min(java.util.Comparator.comparingDouble(villager -> villager.distanceToSqr(
			jobSitePos.getX() + 0.5D,
			jobSitePos.getY() + 0.5D,
			jobSitePos.getZ() + 0.5D
		))).orElse(null);
	}

	private record ChestSession(ServerLevel level, BlockPos pos, List<ItemStack> originalItems, long openedAt, AnathemaCrimes.CrimeObservation observation) {
	}
}
