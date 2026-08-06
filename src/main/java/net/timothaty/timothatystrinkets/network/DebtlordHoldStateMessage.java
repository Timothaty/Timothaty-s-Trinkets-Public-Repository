package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.debtlord.DebtlordHoldClientState;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DebtlordHoldStateMessage(boolean active) implements CustomPacketPayload {
	public static final Type<DebtlordHoldStateMessage> TYPE = new Type<>(
		ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "debtlord_hold_state")
	);
	public static final StreamCodec<FriendlyByteBuf, DebtlordHoldStateMessage> STREAM_CODEC = StreamCodec.of(
		(FriendlyByteBuf buffer, DebtlordHoldStateMessage message) -> buffer.writeBoolean(message.active()),
		(FriendlyByteBuf buffer) -> new DebtlordHoldStateMessage(buffer.readBoolean())
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(DebtlordHoldStateMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.CLIENTBOUND)
			return;
		context.enqueueWork(() -> DebtlordHoldClientState.setActive(message.active()));
	}
}
