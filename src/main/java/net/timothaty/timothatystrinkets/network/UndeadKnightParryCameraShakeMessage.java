package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.handler.ConcussiveStrikeCameraShakeHandler;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UndeadKnightParryCameraShakeMessage() implements CustomPacketPayload {
	public static final UndeadKnightParryCameraShakeMessage INSTANCE = new UndeadKnightParryCameraShakeMessage();
	public static final Type<UndeadKnightParryCameraShakeMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "undead_knight_parry_camera_shake"));
	public static final StreamCodec<FriendlyByteBuf, UndeadKnightParryCameraShakeMessage> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	private static final int DURATION_TICKS = 6;
	private static final float PITCH_AMPLITUDE = 0.62F;
	private static final float YAW_AMPLITUDE = 0.58F;
	private static final float ROLL_AMPLITUDE = 0.46F;

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(UndeadKnightParryCameraShakeMessage message, IPayloadContext context) {
		context.enqueueWork(() -> ConcussiveStrikeCameraShakeHandler.startParryShake(DURATION_TICKS, PITCH_AMPLITUDE, YAW_AMPLITUDE, ROLL_AMPLITUDE));
	}
}
