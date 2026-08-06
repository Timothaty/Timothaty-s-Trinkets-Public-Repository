package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.hubris.HubrisClientState;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HubrisVisualStateMessage(
		int entityId,
		long sessionToken,
		long startGameTime,
		long endGameTime,
		int remainingThorns,
		boolean active
) implements CustomPacketPayload {
	public static final Type<HubrisVisualStateMessage> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "hubris_visual_state")
	);
	public static final StreamCodec<FriendlyByteBuf, HubrisVisualStateMessage> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, HubrisVisualStateMessage message) -> {
				buffer.writeVarInt(message.entityId());
				buffer.writeLong(message.sessionToken());
				buffer.writeLong(message.startGameTime());
				buffer.writeLong(message.endGameTime());
				buffer.writeVarInt(message.remainingThorns());
				buffer.writeBoolean(message.active());
			},
			(FriendlyByteBuf buffer) -> new HubrisVisualStateMessage(
					buffer.readVarInt(),
					buffer.readLong(),
					buffer.readLong(),
					buffer.readLong(),
					buffer.readVarInt(),
					buffer.readBoolean()
			)
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(HubrisVisualStateMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.CLIENTBOUND)
			return;
		context.enqueueWork(() -> HubrisClientState.handle(message));
	}
}
