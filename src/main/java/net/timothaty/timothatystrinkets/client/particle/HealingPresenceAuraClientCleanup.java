package net.timothaty.timothatystrinkets.client.particle;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class HealingPresenceAuraClientCleanup {
	private HealingPresenceAuraClientCleanup() {
	}

	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		if (event.getLevel().isClientSide())
			HealingPresenceAuraParticle.clearTrackedParticles();
	}
}
