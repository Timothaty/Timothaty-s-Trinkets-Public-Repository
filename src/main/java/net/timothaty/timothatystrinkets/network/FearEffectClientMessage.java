package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FearEffectClientMessage() implements CustomPacketPayload {
	public static final FearEffectClientMessage INSTANCE = new FearEffectClientMessage();
	public static final Type<FearEffectClientMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "fear_effect_client"));
	public static final StreamCodec<FriendlyByteBuf, FearEffectClientMessage> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(FearEffectClientMessage message, IPayloadContext context) {
		context.enqueueWork(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null)
				return;

			minecraft.player.playSound(TimothatysTrinketsModSounds.FEAR.get(), 1.35F, 0.82F);
		});
	}
}
