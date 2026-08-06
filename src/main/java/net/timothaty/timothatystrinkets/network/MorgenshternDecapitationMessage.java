package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.morgenshtern.MorgenshternDecapitationClientState;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MorgenshternDecapitationMessage(
		int entityId
) implements CustomPacketPayload {
	public static final Type<MorgenshternDecapitationMessage> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(
					TimothatysTrinketsMod.MODID,
					"morgenshtern_decapitation"
			)
	);

	public static final StreamCodec<
			FriendlyByteBuf,
			MorgenshternDecapitationMessage
	> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, MorgenshternDecapitationMessage message) ->
					buffer.writeInt(message.entityId()),
			(FriendlyByteBuf buffer) ->
					new MorgenshternDecapitationMessage(buffer.readInt())
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(
			MorgenshternDecapitationMessage message,
			IPayloadContext context
	) {
		if (context.flow() != PacketFlow.CLIENTBOUND)
			return;

		context.enqueueWork(() ->
				MorgenshternDecapitationClientState.mark(message.entityId())
		);
	}
}
