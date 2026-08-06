package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.handler.WrathOfTheWickedCameraHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WrathOfTheWickedCameraShakeMessage() implements CustomPacketPayload {
	public static final WrathOfTheWickedCameraShakeMessage INSTANCE =
			new WrathOfTheWickedCameraShakeMessage();
	public static final Type<WrathOfTheWickedCameraShakeMessage> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(
					TimothatysTrinketsMod.MODID,
					"wrath_of_the_wicked_camera_shake"
			)
	);
	public static final StreamCodec<FriendlyByteBuf, WrathOfTheWickedCameraShakeMessage> STREAM_CODEC =
			StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(
			WrathOfTheWickedCameraShakeMessage message,
			IPayloadContext context
	) {
		if (context.flow() != PacketFlow.CLIENTBOUND)
			return;
		context.enqueueWork(WrathOfTheWickedCameraHandler::startLaserImpulse);
	}
}
