package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.wrath_of_the_wicked.WrathOfTheWickedClientState;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WrathOfTheWickedVisualStateMessage(
		int entityId,
		long startGameTime,
		float initialYaw,
		boolean rotationLocked,
		boolean active
) implements CustomPacketPayload {
	public static final Type<WrathOfTheWickedVisualStateMessage> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(
					TimothatysTrinketsMod.MODID,
					"wrath_of_the_wicked_visual_state"
			)
	);
	public static final StreamCodec<FriendlyByteBuf, WrathOfTheWickedVisualStateMessage> STREAM_CODEC =
			StreamCodec.of(
					(FriendlyByteBuf buffer, WrathOfTheWickedVisualStateMessage message) -> {
						buffer.writeInt(message.entityId());
						buffer.writeLong(message.startGameTime());
						buffer.writeFloat(message.initialYaw());
						buffer.writeBoolean(message.rotationLocked());
						buffer.writeBoolean(message.active());
					},
					(FriendlyByteBuf buffer) -> new WrathOfTheWickedVisualStateMessage(
							buffer.readInt(),
							buffer.readLong(),
							buffer.readFloat(),
							buffer.readBoolean(),
							buffer.readBoolean()
					)
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(WrathOfTheWickedVisualStateMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.CLIENTBOUND) {
			return;
		}

		context.enqueueWork(() -> WrathOfTheWickedClientState.setVisualState(
				message.entityId(),
				message.startGameTime(),
				message.initialYaw(),
				message.rotationLocked(),
				message.active()
		));
	}
}
