package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet.DuelistGuardDirection;
import net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet.DuelistGuardState;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DuelistGuardStateMessage(boolean guarding, int directionId) implements CustomPacketPayload {
	public static final Type<DuelistGuardStateMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "duelist_guard_state"));
	public static final StreamCodec<FriendlyByteBuf, DuelistGuardStateMessage> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, DuelistGuardStateMessage message) -> {
		buffer.writeBoolean(message.guarding());
		buffer.writeInt(message.directionId());
	}, (FriendlyByteBuf buffer) -> new DuelistGuardStateMessage(buffer.readBoolean(), buffer.readInt()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(DuelistGuardStateMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.SERVERBOUND)
			return;

		context.enqueueWork(() -> DuelistGuardState.receiveClientState(context.player(), message.guarding(), DuelistGuardDirection.fromNetworkId(message.directionId())));
	}
}
