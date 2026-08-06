package net.timothaty.timothatystrinkets.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.soul_empower.SoulOrbAbsorptionVisualState;

public record SoulOrbAbsorptionVisualStateMessage(int entityId, boolean pulling, boolean rightArm) implements CustomPacketPayload {
	public static final Type<SoulOrbAbsorptionVisualStateMessage> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "soul_orb_absorption_visual_state")
	);
	public static final StreamCodec<FriendlyByteBuf, SoulOrbAbsorptionVisualStateMessage> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, SoulOrbAbsorptionVisualStateMessage message) -> {
				buffer.writeInt(message.entityId());
				buffer.writeBoolean(message.pulling());
				buffer.writeBoolean(message.rightArm());
			},
			(FriendlyByteBuf buffer) -> new SoulOrbAbsorptionVisualStateMessage(
					buffer.readInt(), buffer.readBoolean(), buffer.readBoolean()
			)
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(SoulOrbAbsorptionVisualStateMessage message, IPayloadContext context) {
		if (context.flow() != PacketFlow.CLIENTBOUND) {
			return;
		}

		context.enqueueWork(() -> SoulOrbAbsorptionVisualState.setPullingState(
				message.entityId(), message.pulling(), message.rightArm() ? HumanoidArm.RIGHT : HumanoidArm.LEFT
		));
	}
}
