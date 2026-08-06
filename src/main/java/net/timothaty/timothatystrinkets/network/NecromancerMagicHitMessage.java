package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.necromancer.NecromancerMagicHitParticles;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public record NecromancerMagicHitMessage(
		int targetEntityId,
		double fallbackX,
		double fallbackY,
		double fallbackZ,
		float targetWidth,
		float targetHeight,
		long seed
) implements CustomPacketPayload {
	public static final Type<NecromancerMagicHitMessage> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "necromancer_magic_hit")
	);
	public static final StreamCodec<FriendlyByteBuf, NecromancerMagicHitMessage> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, NecromancerMagicHitMessage message) -> {
				buffer.writeVarInt(message.targetEntityId());
				buffer.writeDouble(message.fallbackX());
				buffer.writeDouble(message.fallbackY());
				buffer.writeDouble(message.fallbackZ());
				buffer.writeFloat(message.targetWidth());
				buffer.writeFloat(message.targetHeight());
				buffer.writeLong(message.seed());
			},
			(FriendlyByteBuf buffer) -> new NecromancerMagicHitMessage(
					buffer.readVarInt(),
					buffer.readDouble(),
					buffer.readDouble(),
					buffer.readDouble(),
					buffer.readFloat(),
					buffer.readFloat(),
					buffer.readLong()
			)
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(NecromancerMagicHitMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.CLIENTBOUND)
			return;
		context.enqueueWork(() -> NecromancerMagicHitParticles.spawn(message));
	}
}
