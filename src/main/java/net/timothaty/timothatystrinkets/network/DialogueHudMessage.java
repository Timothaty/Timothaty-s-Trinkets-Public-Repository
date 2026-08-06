package net.timothaty.timothatystrinkets.network;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.dialogue.DialogueHudClient;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record DialogueHudMessage(String speakerKey, List<String> lineKeys, String soundId, int letterDelayTicks, int lineHoldTicks, float minPitch, float maxPitch, boolean characterSounds) implements CustomPacketPayload {
	public static final Type<DialogueHudMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "dialogue_hud"));
	public static final StreamCodec<FriendlyByteBuf, DialogueHudMessage> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, DialogueHudMessage message) -> {
		buffer.writeUtf(message.speakerKey());
		buffer.writeVarInt(message.lineKeys().size());
		for (String lineKey : message.lineKeys())
			buffer.writeUtf(lineKey);
		buffer.writeUtf(message.soundId());
		buffer.writeVarInt(message.letterDelayTicks());
		buffer.writeVarInt(message.lineHoldTicks());
		buffer.writeFloat(message.minPitch());
		buffer.writeFloat(message.maxPitch());
		buffer.writeBoolean(message.characterSounds());
	}, (FriendlyByteBuf buffer) -> {
		String speakerKey = buffer.readUtf();
		int lineCount = buffer.readVarInt();
		List<String> lineKeys = new ArrayList<>(lineCount);
		for (int i = 0; i < lineCount; i++)
			lineKeys.add(buffer.readUtf());
		return new DialogueHudMessage(speakerKey, List.copyOf(lineKeys), buffer.readUtf(), buffer.readVarInt(), buffer.readVarInt(), buffer.readFloat(), buffer.readFloat(), buffer.readBoolean());
	});

	public DialogueHudMessage {
		lineKeys = List.copyOf(lineKeys);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(DialogueHudMessage message, IPayloadContext context) {
		context.enqueueWork(() -> DialogueHudClient.play(message));
	}
}
