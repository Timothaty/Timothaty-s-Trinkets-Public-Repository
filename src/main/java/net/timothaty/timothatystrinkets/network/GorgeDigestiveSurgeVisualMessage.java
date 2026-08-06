package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.gorge.GorgeDigestiveSurgeClient;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public record GorgeDigestiveSurgeVisualMessage(
		int playerEntityId,
		long seed
) implements CustomPacketPayload {
	public static final Type<GorgeDigestiveSurgeVisualMessage> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(
					TimothatysTrinketsMod.MODID,
					"gorge_digestive_surge_visual"
			)
	);
	public static final StreamCodec<
			FriendlyByteBuf,
			GorgeDigestiveSurgeVisualMessage
	> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, GorgeDigestiveSurgeVisualMessage message) -> {
				buffer.writeVarInt(message.playerEntityId());
				buffer.writeLong(message.seed());
			},
			(FriendlyByteBuf buffer) ->
					new GorgeDigestiveSurgeVisualMessage(
							buffer.readVarInt(),
							buffer.readLong()
					)
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(
			GorgeDigestiveSurgeVisualMessage message,
			IPayloadContext context
	) {
		if (context.flow() != PacketFlow.CLIENTBOUND)
			return;
		context.enqueueWork(() -> GorgeDigestiveSurgeClient.handle(message));
	}
}
