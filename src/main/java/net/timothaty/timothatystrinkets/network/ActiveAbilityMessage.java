package net.timothaty.timothatystrinkets.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;

import net.timothaty.timothatystrinkets.mechanics.pagans_charm.PaganCharmMeditationInterrupts;
import net.timothaty.timothatystrinkets.mechanics.active_ability.ActiveAbilityUseGuard;
import net.timothaty.timothatystrinkets.mechanics.active_ability.ActiveAbilityKeyHandler;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsDebug;
import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

@EventBusSubscriber
public record ActiveAbilityMessage(int eventType, int pressedms) implements CustomPacketPayload {
	public static final Type<ActiveAbilityMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "key_active_ability"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ActiveAbilityMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, ActiveAbilityMessage message) -> {
		buffer.writeInt(message.eventType);
		buffer.writeInt(message.pressedms);
	}, (RegistryFriendlyByteBuf buffer) -> new ActiveAbilityMessage(buffer.readInt(), buffer.readInt()));

	@Override
	public Type<ActiveAbilityMessage> type() {
		return TYPE;
	}

	public static void handleData(final ActiveAbilityMessage message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
			context.enqueueWork(() -> {
				pressAction(context.player(), message.eventType, message.pressedms);
			}).exceptionally(e -> {
				context.connection().disconnect(Component.literal(e.getMessage()));
				return null;
			});
		}
	}

	public static void pressAction(Player entity, int type, int pressedms) {
		if (entity == null)
			return;
		if (type == 0 && ActiveAbilityUseGuard.isBlocked(entity))
			return;
		Level world = entity.level();
		if (!world.hasChunkAt(entity.blockPosition())) {
			TimothatysTrinketsDebug.insatiable(entity, "NO_CHUNK", ChatFormatting.RED);
			return;
		}
		TimothatysTrinketsDebug.insatiable(entity, "ActiveAbilityMessage type=" + type + ", pressedms=" + pressedms, ChatFormatting.DARK_GRAY);
		if (type == 0) {
			PaganCharmMeditationInterrupts.interrupt(entity);
			ActiveAbilityKeyHandler.handle(entity);
		} else {
			TimothatysTrinketsDebug.insatiable(entity, "SKIP: eventType is not 0", ChatFormatting.RED);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		TimothatysTrinketsMod.addNetworkMessage(ActiveAbilityMessage.TYPE, ActiveAbilityMessage.STREAM_CODEC, ActiveAbilityMessage::handleData);
	}
}
