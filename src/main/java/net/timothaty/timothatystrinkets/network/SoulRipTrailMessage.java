package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.vfx.soul_rip.SoulRipTrailHandler;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SoulRipTrailMessage(double x, double y, double z, float width, float height, boolean empowered) implements CustomPacketPayload {
	public static final Type<SoulRipTrailMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "soul_rip_trail"));
	public static final StreamCodec<FriendlyByteBuf, SoulRipTrailMessage> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, SoulRipTrailMessage message) -> {
		buffer.writeDouble(message.x());
		buffer.writeDouble(message.y());
		buffer.writeDouble(message.z());
		buffer.writeFloat(message.width());
		buffer.writeFloat(message.height());
		buffer.writeBoolean(message.empowered());
	}, (FriendlyByteBuf buffer) -> new SoulRipTrailMessage(
			buffer.readDouble(),
			buffer.readDouble(),
			buffer.readDouble(),
			buffer.readFloat(),
			buffer.readFloat(),
			buffer.readBoolean()
	));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(SoulRipTrailMessage message, IPayloadContext context) {
		context.enqueueWork(() -> SoulRipTrailHandler.spawn(message.x(), message.y(), message.z(), message.width(), message.height(), message.empowered()));
	}
}
