package net.timothaty.timothatystrinkets.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.armlet_gauntlet_synergy.PlayerSoulOrbAbsorption;

public record SoulOrbAbsorptionStateMessage(boolean holding) implements CustomPacketPayload {
	public static final Type<SoulOrbAbsorptionStateMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "soul_orb_absorption_state"));
	public static final StreamCodec<FriendlyByteBuf, SoulOrbAbsorptionStateMessage> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, SoulOrbAbsorptionStateMessage message) -> buffer.writeBoolean(message.holding()),
			(FriendlyByteBuf buffer) -> new SoulOrbAbsorptionStateMessage(buffer.readBoolean())
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(SoulOrbAbsorptionStateMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.SERVERBOUND) {
			return;
		}
		context.enqueueWork(() -> {
			if (context.player() instanceof ServerPlayer player) {
				PlayerSoulOrbAbsorption.receiveHoldingState(player, message.holding());
			}
		});
	}
}
