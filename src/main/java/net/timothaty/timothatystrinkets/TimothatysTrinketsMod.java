package net.timothaty.timothatystrinkets;

import net.timothaty.timothatystrinkets.init.*;
import net.timothaty.timothatystrinkets.advancement.TimothatysTrinketsCriteriaTriggers;
import net.timothaty.timothatystrinkets.mechanics.rosarium.RosariumCombinationBootstrap;
import net.timothaty.timothatystrinkets.network.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Tuple;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;


import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Collection;
import java.util.ArrayList;

@Mod("timothatys_trinkets")
public class TimothatysTrinketsMod {
	public static final Logger LOGGER = LogManager.getLogger(TimothatysTrinketsMod.class);
	public static final String MODID = "timothatys_trinkets";

	public TimothatysTrinketsMod(IEventBus modEventBus) {
		RosariumCombinationBootstrap.bootstrap();
		TimothatysTrinketsCriteriaTriggers.REGISTRY.register(modEventBus);
		TimothatysTrinketsModBlockEntities.REGISTRY.register(modEventBus);
		DamnationAltarRecipeRegistry.TYPES.register(modEventBus);
		DamnationAltarRecipeRegistry.SERIALIZERS.register(modEventBus);
		addNetworkMessage(ConcussiveStrikeCameraShakeMessage.TYPE, ConcussiveStrikeCameraShakeMessage.STREAM_CODEC, ConcussiveStrikeCameraShakeMessage::handle);
		addNetworkMessage(StunnedCameraShakeMessage.TYPE, StunnedCameraShakeMessage.STREAM_CODEC, StunnedCameraShakeMessage::handle);
		addNetworkMessage(FearEffectClientMessage.TYPE, FearEffectClientMessage.STREAM_CODEC, FearEffectClientMessage::handle);
		addNetworkMessage(DebtlordFearCameraShakeMessage.TYPE, DebtlordFearCameraShakeMessage.STREAM_CODEC, DebtlordFearCameraShakeMessage::handle);
		addNetworkMessage(DebtlordHoldStateMessage.TYPE, DebtlordHoldStateMessage.STREAM_CODEC, DebtlordHoldStateMessage::handle);
		addNetworkMessage(SoulRipTrailMessage.TYPE, SoulRipTrailMessage.STREAM_CODEC, SoulRipTrailMessage::handle);
		addNetworkMessage(PaganCharmMeditationInterruptMessage.TYPE, PaganCharmMeditationInterruptMessage.STREAM_CODEC, PaganCharmMeditationInterruptMessage::handle);
		addNetworkMessage(DialogueHudMessage.TYPE, DialogueHudMessage.STREAM_CODEC, DialogueHudMessage::handle);
		addNetworkMessage(RefreshingChaliceVfxMessage.TYPE, RefreshingChaliceVfxMessage.STREAM_CODEC, RefreshingChaliceVfxMessage::handle);
		addNetworkMessage(UndeadKnightParryCameraShakeMessage.TYPE, UndeadKnightParryCameraShakeMessage.STREAM_CODEC, UndeadKnightParryCameraShakeMessage::handle);
		addNetworkMessage(DuelistGuardStateMessage.TYPE, DuelistGuardStateMessage.STREAM_CODEC, DuelistGuardStateMessage::handle);
		addNetworkMessage(DuelistGuardStaminaMessage.TYPE, DuelistGuardStaminaMessage.STREAM_CODEC, DuelistGuardStaminaMessage::handle);
		addNetworkMessage(DuelistGuardVisualStateMessage.TYPE, DuelistGuardVisualStateMessage.STREAM_CODEC, DuelistGuardVisualStateMessage::handle);
		addNetworkMessage(SoulOrbAbsorptionStateMessage.TYPE, SoulOrbAbsorptionStateMessage.STREAM_CODEC, SoulOrbAbsorptionStateMessage::handle);
		addNetworkMessage(SoulOrbAbsorptionPulseMessage.TYPE, SoulOrbAbsorptionPulseMessage.STREAM_CODEC, SoulOrbAbsorptionPulseMessage::handle);
		addNetworkMessage(SoulOrbAbsorptionVisualStateMessage.TYPE, SoulOrbAbsorptionVisualStateMessage.STREAM_CODEC, SoulOrbAbsorptionVisualStateMessage::handle);
		addNetworkMessage(WrathOfTheWickedVisualStateMessage.TYPE, WrathOfTheWickedVisualStateMessage.STREAM_CODEC, WrathOfTheWickedVisualStateMessage::handle);
		addNetworkMessage(WrathOfTheWickedLaserMessage.TYPE, WrathOfTheWickedLaserMessage.STREAM_CODEC, WrathOfTheWickedLaserMessage::handle);
		addNetworkMessage(WrathOfTheWickedCameraShakeMessage.TYPE, WrathOfTheWickedCameraShakeMessage.STREAM_CODEC, WrathOfTheWickedCameraShakeMessage::handle);
		addNetworkMessage(FlamingEmberFormationVisualMessage.TYPE, FlamingEmberFormationVisualMessage.STREAM_CODEC, FlamingEmberFormationVisualMessage::handle);
		addNetworkMessage(GorgeConsumptionVisualMessage.TYPE, GorgeConsumptionVisualMessage.STREAM_CODEC, GorgeConsumptionVisualMessage::handle);
		addNetworkMessage(GorgeCameraShakeMessage.TYPE, GorgeCameraShakeMessage.STREAM_CODEC, GorgeCameraShakeMessage::handle);
		addNetworkMessage(GorgeDigestiveSurgeVisualMessage.TYPE, GorgeDigestiveSurgeVisualMessage.STREAM_CODEC, GorgeDigestiveSurgeVisualMessage::handle);
		addNetworkMessage(HubrisActivationMessage.TYPE, HubrisActivationMessage.STREAM_CODEC, HubrisActivationMessage::handle);
		addNetworkMessage(HubrisVisualStateMessage.TYPE, HubrisVisualStateMessage.STREAM_CODEC, HubrisVisualStateMessage::handle);
		addNetworkMessage(CherubimsWisdomActivationMessage.TYPE, CherubimsWisdomActivationMessage.STREAM_CODEC, CherubimsWisdomActivationMessage::handle);
		addNetworkMessage(MorgenshternStrikeMessage.TYPE, MorgenshternStrikeMessage.STREAM_CODEC, MorgenshternStrikeMessage::handle);
		addNetworkMessage(MorgenshternDecapitationMessage.TYPE, MorgenshternDecapitationMessage.STREAM_CODEC, MorgenshternDecapitationMessage::handle);
		addNetworkMessage(NecromancerMagicHitMessage.TYPE, NecromancerMagicHitMessage.STREAM_CODEC, NecromancerMagicHitMessage::handle);
		addNetworkMessage(BeatificPalliumCastMessage.TYPE, BeatificPalliumCastMessage.STREAM_CODEC, BeatificPalliumCastMessage::handle);
		addNetworkMessage(BeatificPalliumImpactMessage.TYPE, BeatificPalliumImpactMessage.STREAM_CODEC, BeatificPalliumImpactMessage::handle);
		addNetworkMessage(BeatificPalliumShatterMessage.TYPE, BeatificPalliumShatterMessage.STREAM_CODEC, BeatificPalliumShatterMessage::handle);
		addNetworkMessage(AngelsShroudEndBurstMessage.TYPE, AngelsShroudEndBurstMessage.STREAM_CODEC, AngelsShroudEndBurstMessage::handle);
		NeoForge.EVENT_BUS.register(this);
		modEventBus.addListener(this::registerNetworking);
		if (ModList.get().isLoaded("curios")) {
			modEventBus.addListener(TimothatysTrinketsModCuriosCompat::registerCapabilities);
		}
		TimothatysTrinketsModSounds.REGISTRY.register(modEventBus);
		TimothatysTrinketsModBlocks.REGISTRY.register(modEventBus);
		TimothatysTrinketsModItems.REGISTRY.register(modEventBus);
		TimothatysTrinketsModEntities.REGISTRY.register(modEventBus);
		TimothatysTrinketsModTabs.REGISTRY.register(modEventBus);
		TimothatysTrinketsModMobEffects.REGISTRY.register(modEventBus);
		TimothatysTrinketsModParticleTypes.REGISTRY.register(modEventBus);
	}

	private static boolean networkingRegistered = false;
	private static final Map<CustomPacketPayload.Type<?>, NetworkMessage<?>> MESSAGES = new HashMap<>();

	private record NetworkMessage<T extends CustomPacketPayload>(StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
	}

	public static <T extends CustomPacketPayload> void addNetworkMessage(CustomPacketPayload.Type<T> id, StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
		if (networkingRegistered)
			throw new IllegalStateException("Cannot register new network messages after networking has been registered");
		MESSAGES.put(id, new NetworkMessage<>(reader, handler));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void registerNetworking(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar(MODID);
		MESSAGES.forEach((id, networkMessage) -> registrar.playBidirectional(id, ((NetworkMessage) networkMessage).reader(), ((NetworkMessage) networkMessage).handler()));
		networkingRegistered = true;
	}

	private static final Collection<Tuple<Runnable, Integer>> workQueue = new ConcurrentLinkedQueue<>();

	public static void queueServerWork(int tick, Runnable action) {
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER)
			workQueue.add(new Tuple<>(action, Math.max(1, tick)));
	}

	@SubscribeEvent
	public void tick(ServerTickEvent.Post event) {
		List<Tuple<Runnable, Integer>> actions = new ArrayList<>();
		workQueue.forEach(work -> {
			work.setB(work.getB() - 1);
			if (work.getB() <= 0)
				actions.add(work);
		});
		workQueue.removeAll(actions);
		actions.forEach(e -> e.getA().run());
	}

	@SubscribeEvent
	public void onServerStopped(ServerStoppedEvent event) {
		workQueue.clear();
	}

	public static class CuriosApiHelper {
		private static final EntityCapability<IItemHandler, Void> CURIOS_INVENTORY = EntityCapability.createVoid(ResourceLocation.fromNamespaceAndPath("curios", "item_handler"), IItemHandler.class);

		public static IItemHandler getCuriosInventory(Player player) {
			if (ModList.get().isLoaded("curios")) {
				return player.getCapability(CURIOS_INVENTORY);
			}
			return null;
		}

		public static boolean isCurioItem(ItemStack itemstack) {
			return BuiltInRegistries.ITEM.getTagNames().filter(tagKey -> tagKey.location().getNamespace().equals("curios")).anyMatch(itemstack::is);
		}
	}
}
