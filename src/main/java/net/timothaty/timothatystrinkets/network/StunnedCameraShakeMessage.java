package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.handler.ConcussiveStrikeCameraShakeHandler;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StunnedCameraShakeMessage() implements CustomPacketPayload {
	public static final StunnedCameraShakeMessage INSTANCE = new StunnedCameraShakeMessage();
	public static final Type<StunnedCameraShakeMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "stunned_camera_shake"));
	public static final StreamCodec<FriendlyByteBuf, StunnedCameraShakeMessage> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	private static final int DURATION_TICKS = 8;
	private static final float PITCH_AMPLITUDE = 1.15F;
	private static final float YAW_AMPLITUDE = 0.9F;
	private static final float ROLL_AMPLITUDE = 1.0F;

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(StunnedCameraShakeMessage message, IPayloadContext context) {
		context.enqueueWork(() -> ConcussiveStrikeCameraShakeHandler.startStunnedShake(DURATION_TICKS, PITCH_AMPLITUDE, YAW_AMPLITUDE, ROLL_AMPLITUDE));
	}
}
