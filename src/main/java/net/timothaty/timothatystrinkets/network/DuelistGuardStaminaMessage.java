package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.DuelistGuardClient;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DuelistGuardStaminaMessage(float stamina) implements CustomPacketPayload {
	public static final Type<DuelistGuardStaminaMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "duelist_guard_stamina"));
	public static final StreamCodec<FriendlyByteBuf, DuelistGuardStaminaMessage> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, DuelistGuardStaminaMessage message) -> buffer.writeFloat(message.stamina()),
			(FriendlyByteBuf buffer) -> new DuelistGuardStaminaMessage(buffer.readFloat()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(DuelistGuardStaminaMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.CLIENTBOUND)
			return;

		context.enqueueWork(() -> DuelistGuardClient.setServerStamina(message.stamina()));
	}
}
