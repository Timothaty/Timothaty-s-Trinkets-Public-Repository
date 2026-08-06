package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.wrath_of_the_wicked.WrathOfTheWickedClientParticles;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WrathOfTheWickedLaserMessage(
		double startX,
		double startY,
		double startZ,
		double targetX,
		double targetY,
		double targetZ
) implements CustomPacketPayload {
	public static final Type<WrathOfTheWickedLaserMessage> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(
					TimothatysTrinketsMod.MODID,
					"wrath_of_the_wicked_laser"
			)
	);
	public static final StreamCodec<FriendlyByteBuf, WrathOfTheWickedLaserMessage> STREAM_CODEC =
			StreamCodec.of(
					(FriendlyByteBuf buffer, WrathOfTheWickedLaserMessage message) -> {
						buffer.writeDouble(message.startX());
						buffer.writeDouble(message.startY());
						buffer.writeDouble(message.startZ());
						buffer.writeDouble(message.targetX());
						buffer.writeDouble(message.targetY());
						buffer.writeDouble(message.targetZ());
					},
					(FriendlyByteBuf buffer) -> new WrathOfTheWickedLaserMessage(
							buffer.readDouble(),
							buffer.readDouble(),
							buffer.readDouble(),
							buffer.readDouble(),
							buffer.readDouble(),
							buffer.readDouble()
					)
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(WrathOfTheWickedLaserMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.CLIENTBOUND)
			return;

		context.enqueueWork(() -> WrathOfTheWickedClientParticles.emitLaser(
				message.startX(),
				message.startY(),
				message.startZ(),
				message.targetX(),
				message.targetY(),
				message.targetZ()
		));
	}
}
