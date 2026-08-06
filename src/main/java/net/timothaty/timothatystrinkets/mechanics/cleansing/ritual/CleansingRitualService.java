package net.timothaty.timothatystrinkets.mechanics.cleansing.ritual;

import net.timothaty.timothatystrinkets.entity.CleansingRitualControllerEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public final class CleansingRitualService {
	private static final double CONTROLLER_SEARCH_RADIUS = 3.25D;

	private CleansingRitualService() {
	}

	public static boolean tryStart(net.minecraft.world.level.Level level, BlockPos clickedPos, Player player,
			InteractionHand hand, ItemStack ignitionStack) {
		if (!(level instanceof ServerLevel serverLevel)) return false;
		CleansingRitualValidator.Match match = CleansingRitualValidator.findFreshPattern(serverLevel, clickedPos);
		if (match == null || hasActiveController(serverLevel, match.center())) return false;

		DecoratedPotBlockEntity pot = CleansingRitualValidator.getPot(serverLevel, match.center());
		if (pot == null) return false;
		ItemStack consumed = pot.splitTheItem(4);
		if (consumed.getCount() != 4 || !pot.getTheItem().isEmpty()) {
			if (!consumed.isEmpty()) pot.setTheItem(consumed);
			syncPot(serverLevel, match.center(), pot);
			return false;
		}
		syncPot(serverLevel, match.center(), pot);

		CleansingRitualControllerEntity controller = new CleansingRitualControllerEntity(
				TimothatysTrinketsModEntities.CLEANSING_RITUAL_CONTROLLER.get(), serverLevel);
		controller.configure(match.center(), player.getUUID(), match.startRouteIndex());
		if (!serverLevel.addFreshEntity(controller)) {
			pot.setTheItem(consumed);
			syncPot(serverLevel, match.center(), pot);
			return false;
		}

		boolean fireCharge = ignitionStack.is(Items.FIRE_CHARGE);
		if (!player.getAbilities().instabuild) {
			if (fireCharge) ignitionStack.shrink(1);
			else ignitionStack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
		}
		CleansingRitualSounds.ignition(serverLevel, clickedPos, player, fireCharge);
		serverLevel.gameEvent(player, GameEvent.BLOCK_CHANGE, clickedPos);
		return true;
	}

	public static boolean hasActiveController(ServerLevel level, BlockPos center) {
		AABB bounds = new AABB(center).inflate(CONTROLLER_SEARCH_RADIUS, 2.0D, CONTROLLER_SEARCH_RADIUS);
		return !level.getEntitiesOfClass(CleansingRitualControllerEntity.class, bounds,
				controller -> controller.isActiveAt(center)).isEmpty();
	}

	public static boolean isOnlyActiveController(ServerLevel level, BlockPos center, CleansingRitualControllerEntity expected) {
		AABB bounds = new AABB(center).inflate(CONTROLLER_SEARCH_RADIUS, 2.0D, CONTROLLER_SEARCH_RADIUS);
		int count = 0;
		for (CleansingRitualControllerEntity controller : level.getEntitiesOfClass(CleansingRitualControllerEntity.class, bounds,
				candidate -> candidate.isActiveAt(center))) {
			if (controller != expected) return false;
			count++;
		}
		return count == 1;
	}

	private static void syncPot(ServerLevel level, BlockPos center, DecoratedPotBlockEntity pot) {
		pot.setChanged();
		BlockState state = level.getBlockState(center);
		level.sendBlockUpdated(center, state, state, 3);
	}
}
