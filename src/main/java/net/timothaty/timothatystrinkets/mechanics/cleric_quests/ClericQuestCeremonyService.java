package net.timothaty.timothatystrinkets.mechanics.cleric_quests;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModEntities;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaVillagerBlessingState;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.display.ClericQuestRewardDisplayState;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.display.ClericQuestRewardDisplayController;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility.HumilityQuestService;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.sacrament.SacramentQuestService;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaVillagerFearEvents;
import net.timothaty.timothatystrinkets.mechanics.pillagers_coin.PillagersCoinExtortionManager;
import net.timothaty.timothatystrinkets.mechanics.pillagers_coin.PillagersCoinVillagerFearData;
import net.timothaty.timothatystrinkets.mechanics.pillagers_coin.PillagersCoinVillagerFearEvents;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class ClericQuestCeremonyService {
	private static final double MAX_DISTANCE_SQR = 8.0D * 8.0D;
	private static final Map<UUID, Session> BY_CLERIC = new HashMap<>();
	private static final Map<UUID, UUID> CLERIC_BY_PLAYER = new HashMap<>();

	private ClericQuestCeremonyService() {
	}

	public static boolean begin(ServerLevel level, Villager cleric, ServerPlayer player, CeremonyKind kind) {
		if (level == null || cleric == null || player == null || kind == null
				|| BY_CLERIC.containsKey(cleric.getUUID())
				|| CLERIC_BY_PLAYER.containsKey(player.getUUID())
				|| PillagersCoinExtortionManager.hasSession(cleric)
				|| PillagersCoinVillagerFearData.fears(cleric, player.getUUID())
				|| PillagersCoinVillagerFearEvents.shouldHideFromCoin(cleric, level.getGameTime())
				|| AnathemaVillagerFearEvents.shouldHideFromAnathema(cleric, level.getGameTime())
				|| !(cleric instanceof AnathemaVillagerBlessingState blessingState))
			return false;

		Entity vfx = startBlessingVisuals(level, cleric, player);
		if (vfx == null)
			return false;

		long endAt = level.getGameTime() + AnathemaVillagerBlessingState.BLESSINGS_DURATION_TICKS;
		Session session = new Session(level.dimension(), cleric.getUUID(), player.getUUID(), vfx.getUUID(), kind, endAt);
		BY_CLERIC.put(cleric.getUUID(), session);
		CLERIC_BY_PLAYER.put(player.getUUID(), cleric.getUUID());
		return true;
	}

	public static boolean playStandalone(ServerLevel level, Villager cleric, Player player) {
		return !PillagersCoinExtortionManager.hasSession(cleric) && startBlessingVisuals(level, cleric, player) != null;
	}

	private static Entity startBlessingVisuals(ServerLevel level, Villager cleric, Player player) {
		if (!(cleric instanceof AnathemaVillagerBlessingState blessingState))
			return null;
		Entity vfx = TimothatysTrinketsModEntities.VFX_INDULGENCY_BLESSING.get().create(level);
		if (vfx == null)
			return null;
		vfx.moveTo(player.getX(), player.getY(), player.getZ(), 0.0F, 0.0F);
		if (!level.addFreshEntity(vfx))
			return null;
		ClericQuestRewardDisplayController.hide(cleric);
		cleric.getNavigation().stop();
		cleric.getLookControl().setLookAt(player, 30.0F, 30.0F);
		blessingState.timothatys_trinkets$startBlessingsAnimation(player);
		level.playSound(null, player.blockPosition(), TimothatysTrinketsModSounds.INDULGENCY_USED.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
		return vfx;
	}

	public static void tick(MinecraftServer server) {
		if (BY_CLERIC.isEmpty())
			return;
		Iterator<Map.Entry<UUID, Session>> iterator = BY_CLERIC.entrySet().iterator();
		while (iterator.hasNext()) {
			Session session = iterator.next().getValue();
			ServerLevel level = server.getLevel(session.dimension);
			Entity entity = level == null ? null : level.getEntity(session.clericId);
			ServerPlayer player = server.getPlayerList().getPlayer(session.playerId);
			Villager cleric = entity instanceof Villager villager ? villager : null;
			if (!isValid(level, cleric, player, session)) {
				iterator.remove();
				CLERIC_BY_PLAYER.remove(session.playerId);
				stopVisuals(level, cleric, session.vfxId);
				continue;
			}

			long now = level.getGameTime();
			AnathemaVillagerBlessingState blessingState = (AnathemaVillagerBlessingState) cleric;
			if (now < session.endAt && blessingState.timothatys_trinkets$isBlessingsAnimationActive()) {
				cleric.getNavigation().stop();
				cleric.getLookControl().setLookAt(player, 30.0F, 30.0F);
				continue;
			}
			if (now + 1L < session.endAt) {
				iterator.remove();
				CLERIC_BY_PLAYER.remove(session.playerId);
				stopVisuals(level, cleric, session.vfxId);
				continue;
			}

			iterator.remove();
			CLERIC_BY_PLAYER.remove(session.playerId);
			complete(level, cleric, player, session.kind);
		}
	}

	public static void cancelForCleric(UUID clericId) {
		Session removed = BY_CLERIC.remove(clericId);
		if (removed != null)
			CLERIC_BY_PLAYER.remove(removed.playerId);
	}

	public static void cancelForCleric(ServerLevel level, Villager cleric) {
		Session removed = BY_CLERIC.remove(cleric.getUUID());
		if (removed == null)
			return;
		CLERIC_BY_PLAYER.remove(removed.playerId);
		stopVisuals(level, cleric, removed.vfxId);
	}

	public static void cancelForPlayer(ServerLevel level, UUID playerId) {
		UUID clericId = CLERIC_BY_PLAYER.get(playerId);
		if (clericId == null)
			return;
		Session session = BY_CLERIC.remove(clericId);
		CLERIC_BY_PLAYER.remove(playerId);
		if (session == null)
			return;
		Entity entity = level.dimension().equals(session.dimension) ? level.getEntity(clericId) : null;
		stopVisuals(level.dimension().equals(session.dimension) ? level : null, entity instanceof Villager villager ? villager : null, session.vfxId);
	}

	public static boolean hasCeremony(UUID clericId) {
		return BY_CLERIC.containsKey(clericId);
	}

	public static boolean hasCeremonies() {
		return !BY_CLERIC.isEmpty();
	}

	public static void clear() {
		BY_CLERIC.clear();
		CLERIC_BY_PLAYER.clear();
	}

	private static boolean isValid(ServerLevel level, Villager cleric, ServerPlayer player, Session session) {
		return level != null
			&& cleric != null
			&& cleric.isAlive()
			&& !cleric.isRemoved()
			&& player != null
			&& player.isAlive()
			&& !player.isRemoved()
			&& player.serverLevel() == level
			&& player.distanceToSqr(cleric) <= MAX_DISTANCE_SQR
			&& !PillagersCoinExtortionManager.hasSession(cleric)
			&& !PillagersCoinVillagerFearData.fears(cleric, session.playerId)
			&& !PillagersCoinVillagerFearEvents.shouldHideFromCoin(cleric, level.getGameTime())
			&& !AnathemaVillagerFearEvents.shouldHideFromAnathema(cleric, level.getGameTime())
			&& cleric instanceof AnathemaVillagerBlessingState;
	}

	private static void stopVisuals(ServerLevel level, Villager cleric, UUID vfxId) {
		if (cleric instanceof AnathemaVillagerBlessingState blessingState)
			blessingState.timothatys_trinkets$stopBlessingsAnimation();
		if (level != null && level.getEntity(vfxId) instanceof Entity vfx)
			vfx.discard();
	}

	private static void complete(ServerLevel level, Villager cleric, ServerPlayer player, CeremonyKind kind) {
		switch (kind) {
			case HUMILITY_REWARD -> HumilityQuestService.finishRewardCeremony(level, cleric, player);
			case SACRAMENT_OFFERINGS -> SacramentQuestService.finishOfferingsCeremony(level, cleric, player);
			case SACRAMENT_HUNT_RETURN -> SacramentQuestService.finishHuntReturnCeremony(level, cleric, player);
			case SACRAMENT_RESTART -> SacramentQuestService.finishRestartCeremony(level, cleric, player);
			case SACRAMENT_REWARD -> SacramentQuestService.finishRewardCeremony(level, cleric, player);
		}
	}

	public enum CeremonyKind {
		HUMILITY_REWARD,
		SACRAMENT_OFFERINGS,
		SACRAMENT_HUNT_RETURN,
		SACRAMENT_RESTART,
		SACRAMENT_REWARD
	}

	private record Session(ResourceKey<Level> dimension, UUID clericId, UUID playerId, UUID vfxId, CeremonyKind kind, long endAt) {
	}
}
