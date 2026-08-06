package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.handler.ConcussiveStrikeCameraShakeHandler;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DebtlordFearCameraShakeMessage() implements CustomPacketPayload {
	public static final DebtlordFearCameraShakeMessage INSTANCE = new DebtlordFearCameraShakeMessage();
	public static final Type<DebtlordFearCameraShakeMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "debtlord_fear_camera_shake"));
	public static final StreamCodec<FriendlyByteBuf, DebtlordFearCameraShakeMessage> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	private static final int DURATION_TICKS = 28;
	private static final float PITCH_AMPLITUDE = 7.8F;
	private static final float YAW_AMPLITUDE = 6.8F;
	private static final float ROLL_AMPLITUDE = 7.2F;

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(DebtlordFearCameraShakeMessage message, IPayloadContext context) {
		context.enqueueWork(() -> ConcussiveStrikeCameraShakeHandler.startLightShake(DURATION_TICKS, PITCH_AMPLITUDE, YAW_AMPLITUDE, ROLL_AMPLITUDE));
	}
}
