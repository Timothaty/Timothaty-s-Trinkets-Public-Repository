package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.animation.PlayerCastHandDustVisuals;
import net.timothaty.timothatystrinkets.client.animation.PlayerCastHandDustVisuals.CastProfile;
import net.timothaty.timothatystrinkets.client.beatific_pallium.BeatificPalliumPlayerAnimation;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BeatificPalliumCastMessage(int casterEntityId) implements CustomPacketPayload {
	public static final Type<BeatificPalliumCastMessage> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "beatific_pallium_cast")
	);
	public static final StreamCodec<FriendlyByteBuf, BeatificPalliumCastMessage> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, BeatificPalliumCastMessage message) ->
					buffer.writeVarInt(message.casterEntityId()),
			(FriendlyByteBuf buffer) -> new BeatificPalliumCastMessage(buffer.readVarInt())
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(BeatificPalliumCastMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.CLIENTBOUND)
			return;
		context.enqueueWork(() -> {
			if (BeatificPalliumPlayerAnimation.start(message.casterEntityId())) {
				PlayerCastHandDustVisuals.start(
						message.casterEntityId(),
						CastProfile.BEATIFIC_PALLIUM
				);
			}
		});
	}
}
