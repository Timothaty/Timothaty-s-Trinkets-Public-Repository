package net.timothaty.timothatystrinkets.client.sound;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.entity.BeatificPalliumEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class BeatificPalliumLoopSoundManager {
	private static final Map<UUID, BeatificPalliumLoopSoundInstance> ACTIVE_SOUNDS = new HashMap<>();

	private BeatificPalliumLoopSoundManager() {
	}

	public static void ensurePlaying(BeatificPalliumEntity pallium) {
		if (!(pallium.level() instanceof ClientLevel level)
				|| Minecraft.getInstance().level != level
				|| pallium.isRemoved()
				|| pallium.getTarget() == null
				|| pallium.getVisualPhase() == BeatificPalliumEntity.VisualPhase.FADING
				|| pallium.getVisualPhase() == BeatificPalliumEntity.VisualPhase.BURST) {
			return;
		}

		UUID key = pallium.getUUID();
		BeatificPalliumLoopSoundInstance current = ACTIVE_SOUNDS.get(key);
		if (current != null && !current.isStopped() && current.belongsTo(level))
			return;
		if (current != null && ACTIVE_SOUNDS.remove(key, current))
			current.forceStop();

		BeatificPalliumLoopSoundInstance instance = new BeatificPalliumLoopSoundInstance(level, pallium);
		ACTIVE_SOUNDS.put(key, instance);
		Minecraft.getInstance().getSoundManager().play(instance);
	}

	public static void stop(int palliumEntityId) {
		for (BeatificPalliumLoopSoundInstance instance : new ArrayList<>(ACTIVE_SOUNDS.values())) {
			if (instance.matchesEntityId(palliumEntityId))
				instance.forceStop();
		}
	}

	public static void stopAll() {
		ArrayList<BeatificPalliumLoopSoundInstance> sounds = new ArrayList<>(ACTIVE_SOUNDS.values());
		ACTIVE_SOUNDS.clear();
		for (BeatificPalliumLoopSoundInstance sound : sounds)
			sound.forceStop();
	}

	static void onInstanceStopped(UUID key, BeatificPalliumLoopSoundInstance instance) {
		ACTIVE_SOUNDS.remove(key, instance);
	}

	@SubscribeEvent
	public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
		if (event.getLevel() instanceof ClientLevel && event.getEntity() instanceof BeatificPalliumEntity pallium)
			ensurePlaying(pallium);
	}

	@SubscribeEvent
	public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
		if (!(event.getLevel() instanceof ClientLevel)
				|| !(event.getEntity() instanceof BeatificPalliumEntity pallium)) {
			return;
		}
		BeatificPalliumLoopSoundInstance instance = ACTIVE_SOUNDS.get(pallium.getUUID());
		if (instance != null)
			instance.forceStop();
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		ACTIVE_SOUNDS.entrySet().removeIf(entry -> entry.getValue().isStopped());
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null)
			return;
		for (var entity : level.entitiesForRendering()) {
			if (entity instanceof BeatificPalliumEntity pallium && pallium.getTarget() != null)
				ensurePlaying(pallium);
		}
	}

	@SubscribeEvent
	public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		stopAll();
	}

	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		if (event.getLevel() instanceof ClientLevel)
			stopAll();
	}
}
