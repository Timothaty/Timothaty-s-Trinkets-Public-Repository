package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.vfx.refreshing_chalice.RefreshingChaliceVfxHandler;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RefreshingChaliceVfxMessage(int entityId) implements CustomPacketPayload {
	public static final Type<RefreshingChaliceVfxMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "refreshing_chalice_vfx"));
	public static final StreamCodec<FriendlyByteBuf, RefreshingChaliceVfxMessage> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, RefreshingChaliceVfxMessage message) -> {
		buffer.writeVarInt(message.entityId());
	}, (FriendlyByteBuf buffer) -> new RefreshingChaliceVfxMessage(buffer.readVarInt()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(RefreshingChaliceVfxMessage message, IPayloadContext context) {
		context.enqueueWork(() -> RefreshingChaliceVfxHandler.spawn(message.entityId()));
	}
}
