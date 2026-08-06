package net.timothaty.timothatystrinkets.client.sound;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.block.entity.EchoSphereBlockEntity;
import net.timothaty.timothatystrinkets.block.entity.EchoSphereSoundStateDispatcher;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModBlocks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class EchoSphereAmbientSoundManager {
	private static final Map<GlobalPos, EchoSphereAmbientSoundInstance> ACTIVE_SOUNDS = new HashMap<>();

	static {
		EchoSphereSoundStateDispatcher.registerClientListeners(
				EchoSphereAmbientSoundManager::onBlockEntityLoaded,
				EchoSphereAmbientSoundManager::onBlockEntityRemoved
		);
	}

	private EchoSphereAmbientSoundManager() {
	}

	public static void ensurePlaying(ClientLevel level, BlockPos pos) {
		Minecraft minecraft = Minecraft.getInstance();
		BlockPos immutablePos = pos.immutable();
		if (minecraft.level != level
				|| !level.hasChunkAt(immutablePos)
				|| !level.getBlockState(immutablePos).is(TimothatysTrinketsModBlocks.ECHO_SPHERE.get())) {
			return;
		}

		GlobalPos key = GlobalPos.of(level.dimension(), immutablePos);
		EchoSphereAmbientSoundInstance current = ACTIVE_SOUNDS.get(key);
		if (current != null && !current.isStopped() && current.belongsTo(level)) {
			return;
		}
		if (current != null && ACTIVE_SOUNDS.remove(key, current)) {
			stopInstance(current);
		}

		EchoSphereAmbientSoundInstance instance = new EchoSphereAmbientSoundInstance(level, immutablePos, key);
		ACTIVE_SOUNDS.put(key, instance);
		minecraft.getSoundManager().play(instance);
	}

	public static void stop(ClientLevel level, BlockPos pos) {
		GlobalPos key = GlobalPos.of(level.dimension(), pos.immutable());
		EchoSphereAmbientSoundInstance instance = ACTIVE_SOUNDS.get(key);
		if (instance != null && instance.belongsTo(level) && ACTIVE_SOUNDS.remove(key, instance)) {
			stopInstance(instance);
		}
	}

	public static void stopAll() {
		ArrayList<EchoSphereAmbientSoundInstance> sounds = new ArrayList<>(ACTIVE_SOUNDS.values());
		ACTIVE_SOUNDS.clear();
		for (EchoSphereAmbientSoundInstance sound : sounds) {
			stopInstance(sound);
		}
	}

	static void onInstanceStopped(GlobalPos key, EchoSphereAmbientSoundInstance instance) {
		ACTIVE_SOUNDS.remove(key, instance);
	}

	@SubscribeEvent
	public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		stopAll();
	}

	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		if (event.getLevel() instanceof ClientLevel) {
			stopAll();
		}
	}

	private static void onBlockEntityLoaded(EchoSphereBlockEntity sphere) {
		if (sphere.getLevel() instanceof ClientLevel level) {
			ensurePlaying(level, sphere.getBlockPos());
		}
	}

	private static void onBlockEntityRemoved(EchoSphereBlockEntity sphere) {
		if (sphere.getLevel() instanceof ClientLevel level) {
			stop(level, sphere.getBlockPos());
		}
	}

	private static void stopInstance(EchoSphereAmbientSoundInstance instance) {
		instance.forceStop();
		Minecraft.getInstance().getSoundManager().stop(instance);
	}
}
