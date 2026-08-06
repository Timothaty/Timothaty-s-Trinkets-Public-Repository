package net.timothaty.timothatystrinkets.mechanics.anathema;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class AnathemaVillageClaims extends SavedData {
	public static final String PLAYER_PLACED_ARMOR_STAND_KEY = "ttr_anathema_player_placed_armor_stand";

	private static final String DATA_NAME = "timothatys_trinkets_anathema_village_claims";
	private static final String BLOCKS_KEY = "PlayerPlacedBlocks";
	private static final Factory<AnathemaVillageClaims> FACTORY = new Factory<>(AnathemaVillageClaims::new, AnathemaVillageClaims::load);

	private final LongSet playerPlacedBlocks = new LongOpenHashSet();

	public static AnathemaVillageClaims get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	public boolean isPlayerPlaced(BlockPos pos) {
		return pos != null && playerPlacedBlocks.contains(pos.asLong());
	}

	public void markPlayerPlaced(BlockPos pos) {
		if (pos != null && playerPlacedBlocks.add(pos.asLong()))
			setDirty();
	}

	public void unmark(BlockPos pos) {
		if (pos != null && playerPlacedBlocks.remove(pos.asLong()))
			setDirty();
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		tag.putLongArray(BLOCKS_KEY, playerPlacedBlocks.toLongArray());
		return tag;
	}

	private static AnathemaVillageClaims load(CompoundTag tag, HolderLookup.Provider registries) {
		AnathemaVillageClaims data = new AnathemaVillageClaims();
		for (long packedPos : tag.getLongArray(BLOCKS_KEY))
			data.playerPlacedBlocks.add(packedPos);
		return data;
	}
}
