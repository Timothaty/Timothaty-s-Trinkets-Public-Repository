package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.animation.PlayerCastHandDustVisuals;
import net.timothaty.timothatystrinkets.client.animation.PlayerCastHandDustVisuals.CastProfile;
import net.timothaty.timothatystrinkets.client.cherubims_wisdom.CherubimsWisdomActivationVisuals;
import net.timothaty.timothatystrinkets.client.cherubims_wisdom.CherubimsWisdomPlayerAnimation;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CherubimsWisdomActivationMessage(int entityId, int castingArmOrdinal)
		implements CustomPacketPayload {
	public static final Type<CherubimsWisdomActivationMessage> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "cherubims_wisdom_activation")
	);
	public static final StreamCodec<FriendlyByteBuf, CherubimsWisdomActivationMessage> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, CherubimsWisdomActivationMessage message) -> {
				buffer.writeVarInt(message.entityId());
				buffer.writeByte(message.castingArmOrdinal());
			},
			(FriendlyByteBuf buffer) -> new CherubimsWisdomActivationMessage(
					buffer.readVarInt(),
					buffer.readUnsignedByte()
			)
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(CherubimsWisdomActivationMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.CLIENTBOUND)
			return;
		context.enqueueWork(() -> {
			if (CherubimsWisdomPlayerAnimation.start(message.entityId(), message.castingArmOrdinal())) {
				CherubimsWisdomActivationVisuals.start(message.entityId());
				PlayerCastHandDustVisuals.start(message.entityId(), CastProfile.CHERUBIMS_WISDOM);
			}
		});
	}
}
