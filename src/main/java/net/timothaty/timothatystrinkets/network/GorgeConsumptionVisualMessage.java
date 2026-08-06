package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.gorge.GorgeConsumptionClient;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public record GorgeConsumptionVisualMessage(
		int targetEntityId,
		int consumerEntityId,
		double centerX,
		double centerY,
		double centerZ,
		double minX,
		double minY,
		double minZ,
		double maxX,
		double maxY,
		double maxZ,
		long seed
) implements CustomPacketPayload {
	public static final Type<GorgeConsumptionVisualMessage> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(
					TimothatysTrinketsMod.MODID,
					"gorge_consumption_visual"
			)
	);
	public static final StreamCodec<FriendlyByteBuf, GorgeConsumptionVisualMessage> STREAM_CODEC =
			StreamCodec.of(
					GorgeConsumptionVisualMessage::encode,
					GorgeConsumptionVisualMessage::decode
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(
			GorgeConsumptionVisualMessage message,
			IPayloadContext context
	) {
		if (context.flow() != PacketFlow.CLIENTBOUND)
			return;
		context.enqueueWork(() -> GorgeConsumptionClient.handle(message));
	}

	private static void encode(
			FriendlyByteBuf buffer,
			GorgeConsumptionVisualMessage message
	) {
		buffer.writeVarInt(message.targetEntityId());
		buffer.writeVarInt(message.consumerEntityId());
		buffer.writeDouble(message.centerX());
		buffer.writeDouble(message.centerY());
		buffer.writeDouble(message.centerZ());
		buffer.writeDouble(message.minX());
		buffer.writeDouble(message.minY());
		buffer.writeDouble(message.minZ());
		buffer.writeDouble(message.maxX());
		buffer.writeDouble(message.maxY());
		buffer.writeDouble(message.maxZ());
		buffer.writeLong(message.seed());
	}

	private static GorgeConsumptionVisualMessage decode(FriendlyByteBuf buffer) {
		return new GorgeConsumptionVisualMessage(
				buffer.readVarInt(),
				buffer.readVarInt(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readDouble(),
				buffer.readLong()
		);
	}
}
