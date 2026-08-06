package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.handler.ConcussiveStrikeCameraShakeHandler;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ConcussiveStrikeCameraShakeMessage() implements CustomPacketPayload {
	public static final ConcussiveStrikeCameraShakeMessage INSTANCE = new ConcussiveStrikeCameraShakeMessage();
	public static final Type<ConcussiveStrikeCameraShakeMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "concussive_strike_camera_shake"));
	public static final StreamCodec<FriendlyByteBuf, ConcussiveStrikeCameraShakeMessage> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	private static final int DURATION_TICKS = 7;
	private static final float PITCH_AMPLITUDE = 1.0F;
	private static final float YAW_AMPLITUDE = 0.65F;
	private static final float ROLL_AMPLITUDE = 0.85F;

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(ConcussiveStrikeCameraShakeMessage message, IPayloadContext context) {
		context.enqueueWork(() -> ConcussiveStrikeCameraShakeHandler.startLightShake(DURATION_TICKS, PITCH_AMPLITUDE, YAW_AMPLITUDE, ROLL_AMPLITUDE));
	}
}
