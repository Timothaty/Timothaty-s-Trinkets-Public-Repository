package net.timothaty.timothatystrinkets.mechanics.debtlord;

import net.timothaty.timothatystrinkets.mechanics.damnation_altar.DamnationAltarRelationHandler;
import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.block.entity.DamnationAltarBlockEntity;
import net.timothaty.timothatystrinkets.entity.DebtlordEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModEntities;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class DebtlordSummonManager {
	public static final int SUMMON_RELATION = -6;
	public static final int VICTORY_RELATION = 50;
	public static final int DEFEAT_RELATION = 0;
	private static final int REQUIRED_RELATION_FOR_VILLAGER_SACRIFICE = 0;
	private static final double ALTAR_ACTIVE_SEARCH_RADIUS = 96.0D;
	private static final String VILLAGER_KILL_KEY = "altar.timohatys_trinkets.villager_kill";
	private static final Map<AltarKey, UUID> ACTIVE_ALTARS = new HashMap<>();

	private DebtlordSummonManager() {
	}

	public static boolean handleVillagerSacrifice(ServerLevel level, ServerPlayer player, LivingEntity victim, BlockPos altarPos, ItemStack ritualDagger) {
		if (!(victim instanceof Villager))
			return false;
		if (!isRitualDagger(ritualDagger))
			return false;
		if (level.getBlockEntity(altarPos) instanceof DamnationAltarBlockEntity altar && altar.isBusyForExternalRitual())
			return false;
		if (isAltarActive(level, altarPos))
			return true;
		if (DebtlordProgressionHandler.hasDefeatedDebtlord(player))
			return false;

		int relation = DamnationAltarRelationHandler.getOrInitRelation(player);
		if (relation != REQUIRED_RELATION_FOR_VILLAGER_SACRIFICE)
			return true;

		ritualDagger.hurtAndBreak(1, player, EquipmentSlot.OFFHAND);
		DamnationAltarRelationHandler.setRelation(player, SUMMON_RELATION);
		player.displayClientMessage(Component.translatable(VILLAGER_KILL_KEY), true);
		summonDebtlord(level, player, altarPos);
		return true;
	}

	public static void summonDebtlord(ServerLevel level, ServerPlayer summoner, BlockPos altarPos) {
		if (DebtlordProgressionHandler.hasDefeatedDebtlord(summoner))
			return;
		if (isAltarActive(level, altarPos))
			return;
		if (level.getBlockEntity(altarPos) instanceof DamnationAltarBlockEntity altar && altar.isBusyForExternalRitual())
			return;

		DebtlordEntity debtlord = TimothatysTrinketsModEntities.DEBTLORD.get().create(level);
		if (debtlord == null)
			return;

		double x = altarPos.getX() + 0.5D;
		double y = altarPos.getY() + 1.0D;
		double z = altarPos.getZ() + 0.5D;
		float yaw = yawToward(x, z, summoner.getX(), summoner.getZ());
		debtlord.moveTo(x, y, z, yaw, 0.0F);
		debtlord.yBodyRot = yaw;
		debtlord.yHeadRot = yaw;
		debtlord.startAltarSummon(summoner, altarPos);
		level.addFreshEntity(debtlord);
		registerAltarLock(debtlord);
	}

	public static boolean isAltarActive(ServerLevel level, BlockPos altarPos) {
		cleanup(level);
		if (hasRegisteredAltarLock(level, altarPos)) return true;

		AABB searchBox = new AABB(altarPos).inflate(ALTAR_ACTIVE_SEARCH_RADIUS);
		for (DebtlordEntity debtlord : level.getEntitiesOfClass(DebtlordEntity.class, searchBox, DebtlordEntity::blocksBoundAltar)) {
			if (debtlord.isBoundToAltar(altarPos)) {
				registerAltarLock(debtlord);
				return true;
			}
		}
		return false;
	}

	public static boolean hasRegisteredAltarLock(ServerLevel level, BlockPos altarPos) {
		AltarKey key = new AltarKey(level.dimension(), altarPos.immutable());
		UUID bossUuid = ACTIVE_ALTARS.get(key);
		if (bossUuid == null) return false;

		Entity entity = level.getEntity(bossUuid);
		if (entity instanceof DebtlordEntity debtlord
				&& debtlord.blocksBoundAltar()
				&& debtlord.isBoundToAltar(altarPos)) return true;
		ACTIVE_ALTARS.remove(key);
		return false;
	}

	public static void registerAltarLock(DebtlordEntity debtlord) {
		if (!(debtlord.level() instanceof ServerLevel serverLevel) || !debtlord.blocksBoundAltar())
			return;
		BlockPos altarPos = debtlord.getBoundAltarPos();
		if (altarPos == null)
			return;
		ACTIVE_ALTARS.put(new AltarKey(serverLevel.dimension(), altarPos.immutable()), debtlord.getUUID());
	}

	public static void releaseAltarLock(DebtlordEntity debtlord) {
		if (!(debtlord.level() instanceof ServerLevel serverLevel))
			return;
		BlockPos altarPos = debtlord.getBoundAltarPos();
		if (altarPos == null)
			return;

		AltarKey key = new AltarKey(serverLevel.dimension(), altarPos.immutable());
		UUID activeUuid = ACTIVE_ALTARS.get(key);
		if (debtlord.getUUID().equals(activeUuid))
			ACTIVE_ALTARS.remove(key);
	}

	public static void completeWithVictory(DebtlordEntity debtlord) {
		if (debtlord.isAltarOutcomeHandled())
			return;

		debtlord.markAltarOutcomeHandled();
		releaseAltarLock(debtlord);
		ServerPlayer summoner = debtlord.getSummonerPlayer();
		if (summoner != null) {
			DamnationAltarRelationHandler.setRelation(summoner, VICTORY_RELATION);
			DebtlordProgressionHandler.markDebtlordDefeated(summoner);
		}
	}

	public static void completeWithDefeat(DebtlordEntity debtlord, ServerPlayer summoner) {
		if (debtlord.isAltarOutcomeHandled())
			return;

		debtlord.markAltarOutcomeHandled();
		releaseAltarLock(debtlord);
		if (summoner != null)
			DamnationAltarRelationHandler.setRelation(summoner, DEFEAT_RELATION);
		debtlord.startAltarDismissal();
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		if (!(player.level() instanceof ServerLevel level))
			return;

		cleanup(level);
		for (UUID bossUuid : new ArrayList<>(ACTIVE_ALTARS.values())) {
			Entity entity = level.getEntity(bossUuid);
			if (entity instanceof DebtlordEntity debtlord && debtlord.isSummonedBy(player) && debtlord.blocksBoundAltar()) {
				completeWithDefeat(debtlord, player);
				return;
			}
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		ACTIVE_ALTARS.clear();
	}

	private static void cleanup(ServerLevel level) {
		Iterator<Map.Entry<AltarKey, UUID>> iterator = ACTIVE_ALTARS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<AltarKey, UUID> entry = iterator.next();
			if (!entry.getKey().dimension().equals(level.dimension()))
				continue;
			Entity entity = level.getEntity(entry.getValue());
			if (!(entity instanceof DebtlordEntity debtlord) || !debtlord.blocksBoundAltar())
				iterator.remove();
		}
	}

	private static float yawToward(double fromX, double fromZ, double toX, double toZ) {
		double dx = toX - fromX;
		double dz = toZ - fromZ;
		return (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
	}

	private static boolean isRitualDagger(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.getItem() == TimothatysTrinketsModItems.RITUAL_DAGGER.get();
	}

	private record AltarKey(ResourceKey<Level> dimension, BlockPos pos) {
	}
}
