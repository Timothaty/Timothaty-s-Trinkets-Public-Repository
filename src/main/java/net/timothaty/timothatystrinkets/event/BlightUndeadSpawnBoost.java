package net.timothaty.timothatystrinkets.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.event.entity.living.SpawnClusterSizeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModBlocks;
import net.timothaty.timothatystrinkets.mechanics.blight.BlightConfig;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsDebug;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public class BlightUndeadSpawnBoost {
	private static final Map<DimChunkKey, BoostMarker> markers = new HashMap<>();
	private static long lastCleanupTime = -1L;

	private static class BoostMarker {
		int remaining;
		long expiresAt;

		BoostMarker(int remaining, long expiresAt) {
			this.remaining = remaining;
			this.expiresAt = expiresAt;
		}
	}

	private static class DimChunkKey {
		final ResourceLocation dim;
		final long chunkLong;

		DimChunkKey(ResourceLocation dim, long chunkLong) {
			this.dim = dim;
			this.chunkLong = chunkLong;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof DimChunkKey k)) return false;
			return chunkLong == k.chunkLong && dim.equals(k.dim);
		}

		@Override
		public int hashCode() {
			int r = dim.hashCode();
			r = 31 * r + (int) (chunkLong ^ (chunkLong >>> 32));
			return r;
		}
	}

	@SubscribeEvent
	public static void onSpawnClusterSize(SpawnClusterSizeEvent event) {
		Mob mob = event.getEntity();
		if (mob == null) return;
		if (!(mob.level() instanceof ServerLevel level)) return;

		if (!mob.isInvertedHealAndHarm()) return;

		BlockPos ground = mob.blockPosition().below();
		if (!level.hasChunkAt(ground)) return;
		if (!level.getBlockState(ground).is(TimothatysTrinketsModBlocks.BLOCK_OF_BLIGHT.get())) return;

		int blightCount = countBlightAround(level, ground, BlightConfig.SPAWN_BOOST_BLIGHT_RADIUS, BlightConfig.SPAWN_BOOST_COUNT_CAP);
		if (blightCount < 10) return;

		int bonus;
		if (blightCount >= 200) bonus = 14;
		else if (blightCount >= 160) bonus = 12;
		else if (blightCount >= 130) bonus = 10;
		else if (blightCount >= 100) bonus = 8;
		else if (blightCount >= 70) bonus = 6;
		else if (blightCount >= 45) bonus = 5;
		else if (blightCount >= 25) bonus = 4;
		else bonus = 3;

		if (bonus >= 8 && level.random.nextFloat() < 0.55F) bonus += 1;
		if (bonus >= 10 && level.random.nextFloat() < 0.30F) bonus += 1;

		int oldSize = event.getSize();
		int newSize = oldSize + bonus;
		if (newSize > BlightConfig.SPAWN_BOOST_MAX_CLUSTER_SIZE) newSize = BlightConfig.SPAWN_BOOST_MAX_CLUSTER_SIZE;

		if (newSize > oldSize) {
			event.setSize(newSize);

			if (TimothatysTrinketsDebug.BLIGHT_SPAWN_DEBUG) {
				markChunkForHighlight(level, mob.blockPosition(), newSize - oldSize);
			}
		}
	}

	@SubscribeEvent
	public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
		if (!TimothatysTrinketsDebug.BLIGHT_SPAWN_DEBUG) return;

		Entity e = event.getEntity();
		if (!(e instanceof Mob mob)) return;
		if (!(mob.level() instanceof ServerLevel level)) return;

		if (!mob.isInvertedHealAndHarm()) return;

		BlockPos ground = mob.blockPosition().below();
		if (!level.hasChunkAt(ground)) return;
		if (!level.getBlockState(ground).is(TimothatysTrinketsModBlocks.BLOCK_OF_BLIGHT.get())) return;

		cleanupMarkers(level);

		DimChunkKey key = new DimChunkKey(level.dimension().location(), new ChunkPos(mob.blockPosition()).toLong());
		BoostMarker m = markers.get(key);
		if (m == null) return;

		long t = level.getGameTime();
		if (t > m.expiresAt || m.remaining <= 0) {
			markers.remove(key);
			return;
		}

		mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, BlightConfig.SPAWN_BOOST_DEBUG_GLOW_TICKS, 0, false, false));
		m.remaining--;
		if (m.remaining <= 0) markers.remove(key);
	}

	private static void markChunkForHighlight(ServerLevel level, BlockPos pos, int extra) {
		cleanupMarkers(level);

		DimChunkKey key = new DimChunkKey(level.dimension().location(), new ChunkPos(pos).toLong());
		long expires = level.getGameTime() + BlightConfig.SPAWN_BOOST_MARKER_TTL_TICKS;

		BoostMarker m = markers.get(key);
		if (m == null) {
			markers.put(key, new BoostMarker(extra, expires));
		} else {
			m.remaining += extra;
			if (expires > m.expiresAt) m.expiresAt = expires;
		}
	}

	private static void cleanupMarkers(ServerLevel level) {
		long t = level.getGameTime();
		if (lastCleanupTime != -1L && (t - lastCleanupTime) < BlightConfig.SPAWN_BOOST_CLEANUP_INTERVAL_TICKS) return;
		lastCleanupTime = t;

		Iterator<Map.Entry<DimChunkKey, BoostMarker>> it = markers.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<DimChunkKey, BoostMarker> en = it.next();
			if (t > en.getValue().expiresAt || en.getValue().remaining <= 0) it.remove();
		}
	}

	private static int countBlightAround(ServerLevel level, BlockPos center, int radius, int stopAt) {
		int count = 0;
		int cx = center.getX();
		int cy = center.getY();
		int cz = center.getZ();

		outer:
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				BlockPos p = new BlockPos(cx + dx, cy, cz + dz);
				if (!level.hasChunkAt(p)) continue;

				if (level.getBlockState(p).is(TimothatysTrinketsModBlocks.BLOCK_OF_BLIGHT.get())) {
					count++;
					if (count >= stopAt) break outer;
				}
			}
		}
		return count;
	}
}
