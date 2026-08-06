package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.pagans_charm.PaganCharmMeditationInterrupts;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PaganCharmMeditationInterruptMessage() implements CustomPacketPayload {
	public static final PaganCharmMeditationInterruptMessage INSTANCE = new PaganCharmMeditationInterruptMessage();
	public static final Type<PaganCharmMeditationInterruptMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "pagan_charm_meditation_interrupt"));
	public static final StreamCodec<FriendlyByteBuf, PaganCharmMeditationInterruptMessage> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(PaganCharmMeditationInterruptMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.SERVERBOUND)
			return;

		context.enqueueWork(() -> PaganCharmMeditationInterrupts.interrupt(context.player()));
	}
}
