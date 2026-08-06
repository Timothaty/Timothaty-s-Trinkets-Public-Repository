package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.beatific_pallium.BeatificPalliumClientState;
import net.timothaty.timothatystrinkets.client.beatific_pallium.BeatificPalliumShatterVisuals;
import net.timothaty.timothatystrinkets.client.sound.BeatificPalliumLoopSoundManager;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BeatificPalliumShatterMessage(
		int palliumEntityId,
		double originX,
		double originY,
		double originZ,
		float bodyYaw,
		float inheritedVelocityX,
		float inheritedVelocityY,
		float inheritedVelocityZ,
		int rgb,
		int seed
) implements CustomPacketPayload {
	public static final Type<BeatificPalliumShatterMessage> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "beatific_pallium_shatter")
	);
	public static final StreamCodec<FriendlyByteBuf, BeatificPalliumShatterMessage> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, BeatificPalliumShatterMessage message) -> {
				buffer.writeVarInt(message.palliumEntityId());
				buffer.writeDouble(message.originX());
				buffer.writeDouble(message.originY());
				buffer.writeDouble(message.originZ());
				buffer.writeFloat(message.bodyYaw());
				buffer.writeFloat(message.inheritedVelocityX());
				buffer.writeFloat(message.inheritedVelocityY());
				buffer.writeFloat(message.inheritedVelocityZ());
				buffer.writeInt(message.rgb());
				buffer.writeInt(message.seed());
			},
			(FriendlyByteBuf buffer) -> new BeatificPalliumShatterMessage(
					buffer.readVarInt(),
					buffer.readDouble(),
					buffer.readDouble(),
					buffer.readDouble(),
					buffer.readFloat(),
					buffer.readFloat(),
					buffer.readFloat(),
					buffer.readFloat(),
					buffer.readInt(),
					buffer.readInt()
			)
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(BeatificPalliumShatterMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.CLIENTBOUND)
			return;
		context.enqueueWork(() -> {
			BeatificPalliumClientState.clearForPallium(message.palliumEntityId());
			BeatificPalliumLoopSoundManager.stop(message.palliumEntityId());
			BeatificPalliumShatterVisuals.spawn(message);
		});
	}
}
