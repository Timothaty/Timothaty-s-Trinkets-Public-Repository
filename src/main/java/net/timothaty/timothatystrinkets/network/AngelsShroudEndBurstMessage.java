package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.angels_shroud.AngelsShroudEndBurstClient;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AngelsShroudEndBurstMessage(
		double x,
		double y,
		double z,
		float bodyHeight,
		float inheritedVelocityX,
		float inheritedVelocityY,
		float inheritedVelocityZ,
		int seed
) implements CustomPacketPayload {
	public static final Type<AngelsShroudEndBurstMessage> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "angels_shroud_end_burst")
	);
	public static final StreamCodec<FriendlyByteBuf, AngelsShroudEndBurstMessage> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, AngelsShroudEndBurstMessage message) -> {
				buffer.writeDouble(message.x());
				buffer.writeDouble(message.y());
				buffer.writeDouble(message.z());
				buffer.writeFloat(message.bodyHeight());
				buffer.writeFloat(message.inheritedVelocityX());
				buffer.writeFloat(message.inheritedVelocityY());
				buffer.writeFloat(message.inheritedVelocityZ());
				buffer.writeInt(message.seed());
			},
			(FriendlyByteBuf buffer) -> new AngelsShroudEndBurstMessage(
					buffer.readDouble(),
					buffer.readDouble(),
					buffer.readDouble(),
					buffer.readFloat(),
					buffer.readFloat(),
					buffer.readFloat(),
					buffer.readFloat(),
					buffer.readInt()
			)
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(AngelsShroudEndBurstMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.CLIENTBOUND)
			return;
		context.enqueueWork(() -> AngelsShroudEndBurstClient.spawn(message));
	}
}
