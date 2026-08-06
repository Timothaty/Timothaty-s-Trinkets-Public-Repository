package net.timothaty.timothatystrinkets.mechanics.blight.storage;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

final class BlightChunkData {
	private final Long2ObjectOpenHashMap<BlightedBlockSnapshot> snapshots = new Long2ObjectOpenHashMap<>();

	boolean remember(long packedPos, BlightedBlockSnapshot snapshot) {
		if (snapshots.containsKey(packedPos)) {
			return false;
		}
		snapshots.put(packedPos, snapshot);
		return true;
	}

	BlightedBlockSnapshot get(long packedPos) {
		return snapshots.get(packedPos);
	}

	BlightedBlockSnapshot remove(long packedPos) {
		return snapshots.remove(packedPos);
	}

	boolean isEmpty() {
		return snapshots.isEmpty();
	}

	Iterable<Long2ObjectMap.Entry<BlightedBlockSnapshot>> entries() {
		return snapshots.long2ObjectEntrySet();
	}
}
