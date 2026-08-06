package net.timothaty.timothatystrinkets.mechanics.damnation_altar;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.block.entity.DamnationAltarBlockEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModBlocks;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.mechanics.debtlord.DebtlordSummonManager;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class DamnationAltarSacrificeRouter {
	private static final int ALTAR_RADIUS = 7;
	private static final double ALTAR_RADIUS_SQ = ALTAR_RADIUS * ALTAR_RADIUS;
	private static final String TAG_ACCEPTED_SACRIFICE = "tt_damnation_accepted_sacrifice";

	private DamnationAltarSacrificeRouter() {
	}

	public static void startBloodRitual(ServerLevel level, BlockPos altarPos, int durationTicks) {
		if (!isSacrificeAvailable(level, altarPos)) return;
		if (level.getBlockEntity(altarPos) instanceof DamnationAltarBlockEntity altar) {
			altar.startBloodRitual(level, durationTicks);
		}
	}

	public static boolean tryConsumeSacrifice(ServerLevel level, BlockPos altarPos) {
		if (!isSacrificeAvailable(level, altarPos)) return false;
		return level.getBlockEntity(altarPos) instanceof DamnationAltarBlockEntity altar
				&& altar.tryConsumeSacrifice(level);
	}

	public static void disableAltarUntilNextNight(ServerLevel level, BlockPos altarPos) {
		if (level.getBlockEntity(altarPos) instanceof DamnationAltarBlockEntity altar) {
			altar.disableSacrificesUntilNextNight(level);
		}
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		if (!(event.getEntity().level() instanceof ServerLevel level)) return;
		LivingEntity victim = event.getEntity();
		Entity killer = event.getSource().getEntity();
		if (!(killer instanceof ServerPlayer player)) return;
		if (!player.getOffhandItem().is(TimothatysTrinketsModItems.RITUAL_DAGGER.get())) return;

		BlockPos altarPos = findNearestAltar(level, victim.getX(), victim.getY(), victim.getZ(), ALTAR_RADIUS);
		if (altarPos == null || !isPlayerInAltarRadius(player, altarPos)) return;
		if (DamnationAltarSacrificeHandler.handle(level, player, victim, altarPos)) {
			victim.getPersistentData().putBoolean(TAG_ACCEPTED_SACRIFICE, true);
		}
	}

	@SubscribeEvent
	public static void onLivingDrops(LivingDropsEvent event) {
		if (!(event.getEntity().level() instanceof ServerLevel)) return;
		if (!(event.getEntity() instanceof Animal)) return;
		if (!event.getEntity().getPersistentData().getBoolean(TAG_ACCEPTED_SACRIFICE)) return;
		event.getDrops().clear();
	}

	private static BlockPos findNearestAltar(ServerLevel level, double x, double y, double z, int radius) {
		int cx = Mth.floor(x);
		int cy = Mth.floor(y);
		int cz = Mth.floor(z);
		BlockPos bestPos = null;
		double bestDistSq = Double.MAX_VALUE;
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -radius; dy <= radius; dy++) {
				for (int dz = -radius; dz <= radius; dz++) {
					int bx = cx + dx;
					int by = cy + dy;
					int bz = cz + dz;
					double px = bx + 0.5D;
					double py = by + 0.5D;
					double pz = bz + 0.5D;
					double distSq = (px - x) * (px - x) + (py - y) * (py - y) + (pz - z) * (pz - z);
					if (distSq > (double) radius * radius || distSq >= bestDistSq) continue;

					cursor.set(bx, by, bz);
					if (!level.hasChunkAt(cursor)) continue;
					if (level.getBlockState(cursor).getBlock() != TimothatysTrinketsModBlocks.DAMNATION_ALTAR.get()) continue;
					if (!isSacrificeAvailable(level, cursor) || !hasSacrificeCapacity(level, cursor)) continue;
					bestDistSq = distSq;
					bestPos = cursor.immutable();
				}
			}
		}
		return bestPos;
	}

	public static boolean isAltarStructurallyUsable(ServerLevel level, BlockPos altarPos) {
		if (level.getBlockState(altarPos).getBlock() != TimothatysTrinketsModBlocks.DAMNATION_ALTAR.get()) return false;
		BlockPos above = altarPos.above();
		return !level.getBlockState(above).isCollisionShapeFullBlock(level, above);
	}

	public static boolean isSacrificeAvailable(ServerLevel level, BlockPos altarPos) {
		if (!isAltarStructurallyUsable(level, altarPos)) return false;
		if (DebtlordSummonManager.isAltarActive(level, altarPos)) return false;
		return level.getBlockEntity(altarPos) instanceof DamnationAltarBlockEntity altar
				&& !altar.isBusyForExternalRitual();
	}

	private static boolean hasSacrificeCapacity(ServerLevel level, BlockPos altarPos) {
		return level.getBlockEntity(altarPos) instanceof DamnationAltarBlockEntity altar
				&& altar.hasSacrificeCapacity(level);
	}

	private static boolean isPlayerInAltarRadius(ServerPlayer player, BlockPos altarPos) {
		double ax = altarPos.getX() + 0.5D;
		double ay = altarPos.getY() + 0.5D;
		double az = altarPos.getZ() + 0.5D;
		return player.distanceToSqr(ax, ay, az) <= ALTAR_RADIUS_SQ;
	}

	public static boolean isBloodRitualActive(ServerLevel level, BlockPos altarPos) {
		return level.getBlockEntity(altarPos) instanceof DamnationAltarBlockEntity altar
				&& altar.isBloodRitualActive(level.getGameTime());
	}
}
