package net.timothaty.timothatystrinkets.mechanics.blight.storage;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;

public final class BlightSavedData extends SavedData {
	private static final String DATA_NAME = "timothatys_trinkets_blight";
	private static final int CURRENT_DATA_VERSION = 1;
	private static final String DATA_VERSION = "DataVersion";
	private static final String CHUNKS = "Chunks";
	private static final String CHUNK_POS = "ChunkPos";
	private static final String BLOCKS = "Blocks";
	private static final String BLOCK_POS = "Pos";
	private static final String ORIGINAL_STATE = "OriginalState";
	private static final String ORIGIN_Y = "OriginY";
	private static final Factory<BlightSavedData> FACTORY = new Factory<>(BlightSavedData::new, BlightSavedData::load);

	private final Long2ObjectOpenHashMap<BlightChunkData> chunks = new Long2ObjectOpenHashMap<>();

	public static BlightSavedData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	public boolean remember(BlockPos pos, BlockState originalState, int originY) {
		long chunkKey = chunkKey(pos);
		BlightChunkData chunk = chunks.computeIfAbsent(chunkKey, ignored -> new BlightChunkData());
		if (!chunk.remember(pos.asLong(), new BlightedBlockSnapshot(originalState, originY))) {
			return false;
		}
		setDirty();
		return true;
	}

	@Nullable
	public BlightedBlockSnapshot getSnapshot(BlockPos pos) {
		BlightChunkData chunk = chunks.get(chunkKey(pos));
		return chunk == null ? null : chunk.get(pos.asLong());
	}

	@Nullable
	public BlightedBlockSnapshot remove(BlockPos pos) {
		long chunkKey = chunkKey(pos);
		BlightChunkData chunk = chunks.get(chunkKey);
		if (chunk == null) {
			return null;
		}

		BlightedBlockSnapshot removed = chunk.remove(pos.asLong());
		if (removed == null) {
			return null;
		}
		if (chunk.isEmpty()) {
			chunks.remove(chunkKey);
		}
		setDirty();
		return removed;
	}

	public int getOriginYOrDefault(BlockPos pos) {
		BlightedBlockSnapshot snapshot = getSnapshot(pos);
		return snapshot == null ? pos.getY() : snapshot.originY();
	}

	public boolean contains(BlockPos pos) {
		return getSnapshot(pos) != null;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		tag.putInt(DATA_VERSION, CURRENT_DATA_VERSION);
		ListTag chunkList = new ListTag();
		for (Long2ObjectMap.Entry<BlightChunkData> chunkEntry : chunks.long2ObjectEntrySet()) {
			CompoundTag chunkTag = new CompoundTag();
			chunkTag.putLong(CHUNK_POS, chunkEntry.getLongKey());
			ListTag blockList = new ListTag();
			for (Long2ObjectMap.Entry<BlightedBlockSnapshot> blockEntry : chunkEntry.getValue().entries()) {
				BlightedBlockSnapshot snapshot = blockEntry.getValue();
				CompoundTag blockTag = new CompoundTag();
				blockTag.putLong(BLOCK_POS, blockEntry.getLongKey());
				blockTag.put(ORIGINAL_STATE, NbtUtils.writeBlockState(snapshot.originalState()));
				blockTag.putInt(ORIGIN_Y, snapshot.originY());
				blockList.add(blockTag);
			}
			chunkTag.put(BLOCKS, blockList);
			chunkList.add(chunkTag);
		}
		tag.put(CHUNKS, chunkList);
		return tag;
	}

	private static BlightSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
		BlightSavedData data = new BlightSavedData();
		ListTag chunkList = tag.getList(CHUNKS, Tag.TAG_COMPOUND);
		for (int chunkIndex = 0; chunkIndex < chunkList.size(); chunkIndex++) {
			CompoundTag chunkTag = chunkList.getCompound(chunkIndex);
			ListTag blockList = chunkTag.getList(BLOCKS, Tag.TAG_COMPOUND);
			for (int blockIndex = 0; blockIndex < blockList.size(); blockIndex++) {
				CompoundTag blockTag = blockList.getCompound(blockIndex);
				if (!blockTag.contains(BLOCK_POS, Tag.TAG_LONG) || !blockTag.contains(ORIGINAL_STATE, Tag.TAG_COMPOUND)) {
					continue;
				}

				long packedPos = blockTag.getLong(BLOCK_POS);
				BlockPos pos = BlockPos.of(packedPos);
				BlockState originalState = NbtUtils.readBlockState(
						registries.lookupOrThrow(Registries.BLOCK),
						blockTag.getCompound(ORIGINAL_STATE));
				int originY = blockTag.contains(ORIGIN_Y, Tag.TAG_INT) ? blockTag.getInt(ORIGIN_Y) : pos.getY();
				data.chunks
						.computeIfAbsent(chunkKey(pos), ignored -> new BlightChunkData())
						.remember(packedPos, new BlightedBlockSnapshot(originalState, originY));
			}
		}
		return data;
	}

	private static long chunkKey(BlockPos pos) {
		return ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
	}
}
