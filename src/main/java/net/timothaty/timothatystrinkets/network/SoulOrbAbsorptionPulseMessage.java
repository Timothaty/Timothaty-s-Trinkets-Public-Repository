package net.timothaty.timothatystrinkets.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.soul_empower.SoulOrbAbsorptionFirstPersonAnimation;

public record SoulOrbAbsorptionPulseMessage() implements CustomPacketPayload {
	public static final Type<SoulOrbAbsorptionPulseMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "soul_orb_absorption_pulse"));
	public static final StreamCodec<FriendlyByteBuf, SoulOrbAbsorptionPulseMessage> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, SoulOrbAbsorptionPulseMessage message) -> {
			},
			(FriendlyByteBuf buffer) -> new SoulOrbAbsorptionPulseMessage()
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(SoulOrbAbsorptionPulseMessage message, IPayloadContext context) {
		if (context.flow() == PacketFlow.CLIENTBOUND) {
			context.enqueueWork(SoulOrbAbsorptionFirstPersonAnimation::pulse);
		}
	}
}
