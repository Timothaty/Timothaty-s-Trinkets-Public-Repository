package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.morgenshtern.MorgenshternClientEffects;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MorgenshternStrikeMessage(
		int attackerEntityId,
		int targetEntityId
) implements CustomPacketPayload {
	public static final Type<MorgenshternStrikeMessage> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(
					TimothatysTrinketsMod.MODID,
					"morgenshtern_strike"
			)
	);

	public static final StreamCodec<FriendlyByteBuf, MorgenshternStrikeMessage>
			STREAM_CODEC = StreamCodec.of(
					(FriendlyByteBuf buffer, MorgenshternStrikeMessage message) -> {
						buffer.writeInt(message.attackerEntityId());
						buffer.writeInt(message.targetEntityId());
					},
					(FriendlyByteBuf buffer) -> new MorgenshternStrikeMessage(
							buffer.readInt(),
							buffer.readInt()
					)
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(
			MorgenshternStrikeMessage message,
			IPayloadContext context
	) {
		if (context.flow() != PacketFlow.CLIENTBOUND)
			return;

		context.enqueueWork(() -> MorgenshternClientEffects.onStrike(
				message.attackerEntityId(),
				message.targetEntityId()
		));
	}
}
