package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.DuelistGuardClient;
import net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet.DuelistGuardDirection;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DuelistGuardVisualStateMessage(int entityId, boolean guarding, int directionId) implements CustomPacketPayload {
	public static final Type<DuelistGuardVisualStateMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "duelist_guard_visual_state"));
	public static final StreamCodec<FriendlyByteBuf, DuelistGuardVisualStateMessage> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, DuelistGuardVisualStateMessage message) -> {
		buffer.writeInt(message.entityId());
		buffer.writeBoolean(message.guarding());
		buffer.writeInt(message.directionId());
	}, (FriendlyByteBuf buffer) -> new DuelistGuardVisualStateMessage(buffer.readInt(), buffer.readBoolean(), buffer.readInt()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(DuelistGuardVisualStateMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.CLIENTBOUND)
			return;

		context.enqueueWork(() -> DuelistGuardClient.setServerGuardVisualState(message.entityId(), message.guarding(), DuelistGuardDirection.fromNetworkId(message.directionId())));
	}
}
