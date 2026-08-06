package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.gorge;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.network.GorgeCameraShakeMessage;
import net.timothaty.timothatystrinkets.network.GorgeConsumptionVisualMessage;
import net.timothaty.timothatystrinkets.network.GorgeDigestiveSurgeVisualMessage;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;

public final class GorgeVisuals {
	private GorgeVisuals() {
	}

	static ConsumptionSnapshot capture(Animal target) {
		AABB bounds = target.getBoundingBox();
		Vec3 center = bounds.getCenter();
		return new ConsumptionSnapshot(
				target.getId(),
				center,
				bounds,
				target.chunkPosition()
		);
	}

	static void emitSuccessfulConsumption(
			ServerPlayer player,
			ConsumptionSnapshot snapshot
	) {
		if (player == null || snapshot == null)
			return;

		AABB bounds = snapshot.bounds();
		Vec3 center = snapshot.center();
		long seed = player.getRandom().nextLong();
		GorgeConsumptionVisualMessage visualMessage = new GorgeConsumptionVisualMessage(
				snapshot.targetEntityId(),
				player.getId(),
				center.x,
				center.y,
				center.z,
				bounds.minX,
				bounds.minY,
				bounds.minZ,
				bounds.maxX,
				bounds.maxY,
				bounds.maxZ,
				seed
		);

		Set<ServerPlayer> recipients = new HashSet<>(
				player.serverLevel()
						.getChunkSource()
						.chunkMap
						.getPlayers(snapshot.trackingChunk(), false)
		);
		recipients.addAll(
				player.serverLevel()
						.getChunkSource()
						.chunkMap
						.getPlayers(player.chunkPosition(), false)
		);
		recipients.add(player);
		for (ServerPlayer recipient : recipients) {
			PacketDistributor.sendToPlayer(recipient, visualMessage);
		}
		player.serverLevel().playSound(
				null,
				center.x,
				center.y,
				center.z,
				TimothatysTrinketsModSounds.GORGE_EAT.get(),
				SoundSource.PLAYERS,
				1.0F,
				1.0F + player.getRandom().nextFloat() * 0.2F
		);
		PacketDistributor.sendToPlayer(
				player,
				new GorgeCameraShakeMessage(seed ^ 0x6A09E667F3BCC909L)
		);
	}

	static void emitDigestiveSurge(ServerPlayer player) {
		if (player == null)
			return;
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(
				player,
				new GorgeDigestiveSurgeVisualMessage(
						player.getId(),
						player.getRandom().nextLong()
				)
		);
	}

	record ConsumptionSnapshot(
			int targetEntityId,
			Vec3 center,
			AABB bounds,
			ChunkPos trackingChunk
	) {
	}
}
