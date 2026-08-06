package net.timothaty.timothatystrinkets.mechanics.cleric_quests;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClericQuestSavedData extends SavedData {
	private static final String DATA_NAME = "timothatys_trinkets_cleric_quests";
	private static final String PROGRESS_LIST = "Players";
	private static final String PLAYER_ID = "Player";
	private static final String PROGRESS = "Progress";
	private static final Factory<ClericQuestSavedData> FACTORY = new Factory<>(ClericQuestSavedData::new, ClericQuestSavedData::load);

	private final Map<UUID, ClericQuestProgress> progressByPlayer = new HashMap<>();

	public static ClericQuestSavedData get(ServerLevel level) {
		ServerLevel storageLevel = level.getServer().overworld();
		ClericQuestSavedData data = storageLevel.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
		ClericQuestRuntimeManager.ensureIndexed(data);
		return data;
	}

	public ClericQuestProgress get(UUID playerId) {
		return progressByPlayer.get(playerId);
	}

	public ClericQuestProgress getOrCreate(UUID playerId) {
		return progressByPlayer.computeIfAbsent(playerId, ignored -> new ClericQuestProgress());
	}

	public Collection<Map.Entry<UUID, ClericQuestProgress>> entries() {
		return progressByPlayer.entrySet();
	}

	public void changed(UUID playerId) {
		setDirty();
		ClericQuestProgress progress = progressByPlayer.get(playerId);
		if (progress != null)
			ClericQuestRuntimeManager.updatePlayerIndex(playerId, progress);
	}

	public void remove(UUID playerId) {
		if (progressByPlayer.remove(playerId) != null) {
			setDirty();
			ClericQuestRuntimeManager.removePlayerIndex(playerId);
		}
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag players = new ListTag();
		for (Map.Entry<UUID, ClericQuestProgress> entry : progressByPlayer.entrySet()) {
			CompoundTag playerTag = new CompoundTag();
			playerTag.putUUID(PLAYER_ID, entry.getKey());
			playerTag.put(PROGRESS, entry.getValue().save());
			players.add(playerTag);
		}
		tag.put(PROGRESS_LIST, players);
		return tag;
	}

	private static ClericQuestSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
		ClericQuestSavedData data = new ClericQuestSavedData();
		ListTag players = tag.getList(PROGRESS_LIST, Tag.TAG_COMPOUND);
		for (int index = 0; index < players.size(); index++) {
			CompoundTag playerTag = players.getCompound(index);
			if (playerTag.hasUUID(PLAYER_ID) && playerTag.contains(PROGRESS, Tag.TAG_COMPOUND))
				data.progressByPlayer.put(playerTag.getUUID(PLAYER_ID), ClericQuestProgress.load(playerTag.getCompound(PROGRESS)));
		}
		return data;
	}
}
