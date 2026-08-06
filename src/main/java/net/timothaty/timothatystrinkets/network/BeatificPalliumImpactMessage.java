package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.beatific_pallium.BeatificPalliumClientState;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BeatificPalliumImpactMessage(
		int palliumEntityId,
		int face,
		float u,
		float v,
		float absorbedDamage,
		int seed
) implements CustomPacketPayload {
	public static final Type<BeatificPalliumImpactMessage> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "beatific_pallium_impact")
	);
	public static final StreamCodec<FriendlyByteBuf, BeatificPalliumImpactMessage> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, BeatificPalliumImpactMessage message) -> {
				buffer.writeVarInt(message.palliumEntityId());
				buffer.writeByte(message.face());
				buffer.writeShort(Math.round(Mth.clamp(message.u(), 0.0F, 1.0F) * 65535.0F));
				buffer.writeShort(Math.round(Mth.clamp(message.v(), 0.0F, 1.0F) * 65535.0F));
				buffer.writeFloat(message.absorbedDamage());
				buffer.writeInt(message.seed());
			},
			(FriendlyByteBuf buffer) -> new BeatificPalliumImpactMessage(
					buffer.readVarInt(),
					buffer.readUnsignedByte(),
					buffer.readUnsignedShort() / 65535.0F,
					buffer.readUnsignedShort() / 65535.0F,
					buffer.readFloat(),
					buffer.readInt()
			)
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(BeatificPalliumImpactMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.CLIENTBOUND)
			return;
		context.enqueueWork(() -> BeatificPalliumClientState.addImpact(message));
	}
}
