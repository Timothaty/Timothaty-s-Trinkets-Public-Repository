package net.timothaty.timothatystrinkets.mechanics.pillagers_coin;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaHelper;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaVillageRules;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaVillagerFearEvents;
import net.timothaty.timothatystrinkets.mechanics.bloodstained.BloodstainedHelper;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.display.ClericQuestRewardDisplayController;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.behavior.VillagerPanicTrigger;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.UUID;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class PillagersCoinVillagerFearEvents {
	private static final double FEAR_RADIUS = PillagersCoinData.EXTORTION_MAX_DISTANCE;
	private static final int SCAN_INTERVAL_TICKS = 15;
	private static final long HIDE_GRACE_TICKS = SCAN_INTERVAL_TICKS + 1L;
	private static final float OPEN_FIELD_PANIC_SPEED = 0.75F;
	private static final int OPEN_FIELD_ESCAPE_HORIZONTAL_RANGE = 16;
	private static final int OPEN_FIELD_ESCAPE_VERTICAL_RANGE = 7;
	private static final int OPEN_FIELD_ESCAPE_ATTEMPTS = 10;

	private PillagersCoinVillagerFearEvents() {
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (!(player.level() instanceof ServerLevel level)
				|| Math.floorMod(player.tickCount + player.getId(), SCAN_INTERVAL_TICKS) != 0)
			return;

		AABB bounds = player.getBoundingBox().inflate(FEAR_RADIUS);
		for (Villager villager : level.getEntitiesOfClass(
			Villager.class,
			bounds,
			candidate -> candidate.isAlive()
				&& !PillagersCoinVillagerSleepEvents.isReturningToSleep(candidate)
				&& candidate.distanceToSqr(player) <= PillagersCoinData.EXTORTION_MAX_DISTANCE_SQR
				&& PillagersCoinVillagerFearData.fears(candidate, player.getUUID())
		)) {
			if (villager.isSleeping())
				continue;
			activateFear(villager, player);
			if (villager instanceof PillagersCoinVillagerRuntimeState state)
				state.timothatys_trinkets$startFearVisualPulse();
			level.broadcastEntityEvent(villager, (byte) 42);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onVillagerInteract(PlayerInteractEvent.EntityInteract event) {
		Player player = event.getEntity();
		if (!(event.getTarget() instanceof Villager villager)
				|| !(player.level() instanceof ServerLevel level)
				|| event.getHand() != InteractionHand.MAIN_HAND
				|| !PillagersCoinVillagerFearData.fears(villager, player.getUUID())
				|| isIndulgencyUse(event, player, villager))
			return;

		if (player.isCrouching() && event.getItemStack().is(Items.EMERALD))
			tryForgiveness(event, level, player, villager);
		else
			refuseInteraction(event, villager);
	}

	public static void activateFear(Villager villager) {
		if (!(villager.level() instanceof ServerLevel level) || !(villager instanceof PillagersCoinVillagerRuntimeState state))
			return;
		Player fearedPlayer = findNearestFearedPlayer(level, villager);
		if (fearedPlayer == null)
			return;
		activateFear(villager, state, level, fearedPlayer);
	}

	public static void activateFear(Villager villager, Player fearedPlayer) {
		if (!(villager.level() instanceof ServerLevel level)
				|| !(villager instanceof PillagersCoinVillagerRuntimeState state)
				|| !isValidFearedPlayer(villager, fearedPlayer))
			return;
		activateFear(villager, state, level, fearedPlayer);
	}

	private static void activateFear(
			Villager villager,
			PillagersCoinVillagerRuntimeState state,
			ServerLevel level,
			Player fearedPlayer
	) {
		ClericQuestRewardDisplayController.hide(villager);
		Player cachedPlayer = findCachedFearedPlayer(level, villager, state);
		if (cachedPlayer != null
				&& villager.distanceToSqr(cachedPlayer) < villager.distanceToSqr(fearedPlayer))
			fearedPlayer = cachedPlayer;

		long now = level.getGameTime();
		if (state.timothatys_trinkets$getPillagersCoinHideUntil() <= now)
			clearCurrentIntent(villager);
		state.timothatys_trinkets$setPillagersCoinHideUntil(now + HIDE_GRACE_TICKS);
		state.timothatys_trinkets$setPillagersCoinFearedPlayer(fearedPlayer.getUUID());
		applyFearActivity(level, villager, fearedPlayer);
	}

	public static void tickFearState(Villager villager, PillagersCoinVillagerRuntimeState state) {
		if (!(villager.level() instanceof ServerLevel level))
			return;
		long hideUntil = state.timothatys_trinkets$getPillagersCoinHideUntil();
		if (hideUntil == 0L)
			return;

		long now = level.getGameTime();
		if (villager.isSleeping() || PillagersCoinVillagerSleepEvents.isReturningToSleep(villager)) {
			state.timothatys_trinkets$setPillagersCoinHideUntil(0L);
			state.timothatys_trinkets$setPillagersCoinFearedPlayer(null);
			clearCurrentIntent(villager);
			villager.getBrain().setActiveActivityIfPossible(Activity.REST);
			return;
		}
		if (hideUntil > now && applyActiveFear(villager, now))
			return;
		clearFearActivity(level, villager, state, now);
	}

	public static boolean shouldHideFromCoin(Villager villager, long gameTime) {
		return villager instanceof PillagersCoinVillagerRuntimeState state
			&& state.timothatys_trinkets$getPillagersCoinHideUntil() > gameTime;
	}

	public static boolean applyActiveFear(Villager villager, long gameTime) {
		if (!(villager.level() instanceof ServerLevel level)
				|| !(villager instanceof PillagersCoinVillagerRuntimeState state)
				|| state.timothatys_trinkets$getPillagersCoinHideUntil() <= gameTime)
			return false;
		Player fearedPlayer = findCachedFearedPlayer(level, villager, state);
		if (fearedPlayer == null)
			fearedPlayer = findNearestFearedPlayer(level, villager);
		if (fearedPlayer == null)
			return false;
		state.timothatys_trinkets$setPillagersCoinFearedPlayer(fearedPlayer.getUUID());
		applyFearActivity(level, villager, fearedPlayer);
		return true;
	}

	private static void tryForgiveness(PlayerInteractEvent.EntityInteract event, ServerLevel level, Player player, Villager villager) {
		if (AnathemaHelper.getLevel(player) > 0 || BloodstainedHelper.hasBloodstained(player)) {
			refuseInteraction(event, villager);
			return;
		}

		int cost = PillagersCoinVillagerFearData.getForgivenessCost(villager, player.getUUID());
		if (event.getItemStack().getCount() < cost) {
			player.displayClientMessage(Component.translatable("message.timothatys_trinkets.pillagers_coin.forgiveness_cost", cost), true);
			refuseInteraction(event, villager);
			return;
		}

		if (!player.getAbilities().instabuild)
			event.getItemStack().shrink(cost);
		PillagersCoinVillagerFearData.forgive(villager, player.getUUID());
		villager.setUnhappyCounter(0);
		level.broadcastEntityEvent(villager, (byte) 14);
		villager.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
		cancel(event, player);
	}

	private static void refuseInteraction(PlayerInteractEvent.EntityInteract event, Villager villager) {
		villager.setUnhappyCounter(40);
		villager.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
		cancel(event, event.getEntity());
	}

	private static void cancel(PlayerInteractEvent.EntityInteract event, Player player) {
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide()));
	}

	private static boolean isIndulgencyUse(PlayerInteractEvent.EntityInteract event, Player player, Villager villager) {
		return player.isCrouching()
			&& villager.getVillagerData().getProfession() == VillagerProfession.CLERIC
			&& event.getItemStack().is(TimothatysTrinketsModItems.INDULGENCY.get());
	}

	private static Player findNearestFearedPlayer(ServerLevel level, Villager villager) {
		Player nearest = null;
		double nearestDistance = PillagersCoinData.EXTORTION_MAX_DISTANCE_SQR;
		for (Player player : level.players()) {
			double distance = villager.distanceToSqr(player);
			if (distance <= nearestDistance && isFearedPlayer(villager, player)) {
				nearest = player;
				nearestDistance = distance;
			}
		}
		return nearest;
	}

	@Nullable
	private static Player findCachedFearedPlayer(
			ServerLevel level,
			Villager villager,
			PillagersCoinVillagerRuntimeState state
	) {
		UUID playerId = state.timothatys_trinkets$getPillagersCoinFearedPlayer();
		if (playerId != null
				&& level.getEntity(playerId) instanceof Player player
				&& isValidFearedPlayer(villager, player))
			return player;

		state.timothatys_trinkets$setPillagersCoinFearedPlayer(null);
		return null;
	}

	private static boolean isValidFearedPlayer(Villager villager, @Nullable Player player) {
		return player != null
				&& isFearedPlayer(villager, player)
				&& villager.distanceToSqr(player) <= PillagersCoinData.EXTORTION_MAX_DISTANCE_SQR;
	}

	private static boolean isFearedPlayer(Villager villager, Player player) {
		return player.isAlive()
				&& PillagersCoinVillagerFearData.fears(villager, player.getUUID());
	}

	private static void applyFearActivity(ServerLevel level, Villager villager, Player fearedPlayer) {
		if (AnathemaVillageRules.isVillageTerritory(level, villager.blockPosition())) {
			if (!villager.getBrain().isActive(Activity.HIDE)) {
				clearCurrentIntent(villager);
				villager.getBrain().setActiveActivityIfPossible(Activity.HIDE);
			}
			return;
		}

		if (villager.getBrain().isActive(Activity.HIDE))
			clearCurrentIntent(villager);
		if (!villager.getBrain().isActive(Activity.PANIC))
			villager.getBrain().setActiveActivityIfPossible(Activity.PANIC);
		ensureOpenFieldEscapeTarget(villager, fearedPlayer);
	}

	private static void ensureOpenFieldEscapeTarget(Villager villager, Player fearedPlayer) {
		Vec3 villagerPosition = villager.position();
		Vec3 toFearedPlayer = fearedPlayer.position().subtract(villagerPosition);
		boolean alreadyEscaping = villager.getBrain().getMemory(MemoryModuleType.WALK_TARGET)
			.map(target -> target.getTarget().currentPosition().subtract(villagerPosition).dot(toFearedPlayer) < 0.0D)
			.orElse(false);
		if (alreadyEscaping)
			return;

		villager.getNavigation().stop();
		villager.getBrain().eraseMemory(MemoryModuleType.PATH);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		for (int attempt = 0; attempt < OPEN_FIELD_ESCAPE_ATTEMPTS; attempt++) {
			Vec3 escapeTarget = LandRandomPos.getPosAway(
				villager,
				OPEN_FIELD_ESCAPE_HORIZONTAL_RANGE,
				OPEN_FIELD_ESCAPE_VERTICAL_RANGE,
				fearedPlayer.position()
			);
			if (escapeTarget != null) {
				villager.getBrain().setMemory(
					MemoryModuleType.WALK_TARGET,
					new WalkTarget(escapeTarget, OPEN_FIELD_PANIC_SPEED, 0)
				);
				return;
			}
		}
	}

	private static void clearFearActivity(ServerLevel level, Villager villager, PillagersCoinVillagerRuntimeState state, long now) {
		state.timothatys_trinkets$setPillagersCoinHideUntil(0L);
		state.timothatys_trinkets$setPillagersCoinFearedPlayer(null);
		clearCurrentIntent(villager);
		if (AnathemaVillagerFearEvents.shouldHideFromAnathema(villager, now)) {
			villager.getBrain().setActiveActivityIfPossible(Activity.HIDE);
		} else if (VillagerPanicTrigger.isHurt(villager) || VillagerPanicTrigger.hasHostile(villager)) {
			villager.getBrain().setActiveActivityIfPossible(Activity.PANIC);
		} else {
			villager.getBrain().updateActivityFromSchedule(level.getDayTime(), now);
		}
	}

	private static void clearCurrentIntent(Villager villager) {
		villager.getNavigation().stop();
		villager.getBrain().eraseMemory(MemoryModuleType.PATH);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
	}
}
