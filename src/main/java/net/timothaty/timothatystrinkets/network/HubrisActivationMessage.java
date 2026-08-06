package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.hubris.HubrisActivationClientState;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HubrisActivationMessage(
		int entityId,
		long sessionToken,
		long startGameTime,
		int mainArmOrdinal,
		int selectedHotbarSlot,
		int variantOrdinal,
		ItemStack weaponSnapshot,
		boolean active
) implements CustomPacketPayload {
	public static final Type<HubrisActivationMessage> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "hubris_activation_state")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, HubrisActivationMessage> STREAM_CODEC = StreamCodec.of(
			HubrisActivationMessage::encode,
			HubrisActivationMessage::decode
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(HubrisActivationMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.CLIENTBOUND)
			return;
		context.enqueueWork(() -> HubrisActivationClientState.handle(message));
	}

	private static void encode(RegistryFriendlyByteBuf buffer, HubrisActivationMessage message) {
		buffer.writeVarInt(message.entityId());
		buffer.writeLong(message.sessionToken());
		buffer.writeLong(message.startGameTime());
		buffer.writeByte(message.mainArmOrdinal());
		buffer.writeByte(message.selectedHotbarSlot());
		buffer.writeByte(message.variantOrdinal());
		ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, message.weaponSnapshot());
		buffer.writeBoolean(message.active());
	}

	private static HubrisActivationMessage decode(RegistryFriendlyByteBuf buffer) {
		return new HubrisActivationMessage(
				buffer.readVarInt(),
				buffer.readLong(),
				buffer.readLong(),
				buffer.readUnsignedByte(),
				buffer.readUnsignedByte(),
				buffer.readUnsignedByte(),
				ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
				buffer.readBoolean()
		);
	}
}
