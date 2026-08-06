package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.gorge.GorgeCameraShakeHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public record GorgeCameraShakeMessage(long seed) implements CustomPacketPayload {
	public static final Type<GorgeCameraShakeMessage> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(
					TimothatysTrinketsMod.MODID,
					"gorge_camera_shake"
			)
	);
	public static final StreamCodec<FriendlyByteBuf, GorgeCameraShakeMessage> STREAM_CODEC =
			StreamCodec.of(
					(FriendlyByteBuf buffer, GorgeCameraShakeMessage message) ->
							buffer.writeLong(message.seed()),
					(FriendlyByteBuf buffer) ->
							new GorgeCameraShakeMessage(buffer.readLong())
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(
			GorgeCameraShakeMessage message,
			IPayloadContext context
	) {
		if (context.flow() != PacketFlow.CLIENTBOUND)
			return;
		context.enqueueWork(() -> GorgeCameraShakeHandler.start(message.seed()));
	}
}
