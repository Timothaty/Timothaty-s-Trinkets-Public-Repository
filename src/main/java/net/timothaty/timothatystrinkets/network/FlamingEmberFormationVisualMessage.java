package net.timothaty.timothatystrinkets.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.flaming_ember.formation.FlamingEmberFormationData;

public record FlamingEmberFormationVisualMessage(String token, int progress, boolean active) implements CustomPacketPayload {
	private static final int MAX_TOKEN_LENGTH = 64;

	public static final Type<FlamingEmberFormationVisualMessage> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "flaming_ember_formation_visual")
	);
	public static final StreamCodec<FriendlyByteBuf, FlamingEmberFormationVisualMessage> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, FlamingEmberFormationVisualMessage message) -> {
				buffer.writeUtf(message.token(), MAX_TOKEN_LENGTH);
				buffer.writeInt(message.progress());
				buffer.writeBoolean(message.active());
			},
			(FriendlyByteBuf buffer) -> new FlamingEmberFormationVisualMessage(
					buffer.readUtf(MAX_TOKEN_LENGTH), buffer.readInt(), buffer.readBoolean()
			)
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(FlamingEmberFormationVisualMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.CLIENTBOUND)
			return;

		context.enqueueWork(() -> FlamingEmberFormationData.receiveVisualState(
				context.player(), message.token(), message.progress(), message.active()
		));
	}
}
