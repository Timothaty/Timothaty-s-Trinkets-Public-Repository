package net.timothaty.timothatystrinkets.event;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.advancement.TimothatysTrinketsCriteriaTriggers;
import net.timothaty.timothatystrinkets.mechanics.blight.BlightAuraCache;
import net.timothaty.timothatystrinkets.mechanics.blight.BlightConfig;
import net.timothaty.timothatystrinkets.mechanics.blight.BlightGroundEffectHelper;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public class BlightGroundAuraEvent {
	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Post event) {
		if (!(event.getEntity() instanceof LivingEntity living)) {
			return;
		}

		Level level = living.level();
		if (level.isClientSide() || !living.isAlive()) {
			return;
		}
		if (!shouldProcessThisTick(living)) {
			return;
		}

		BlightAuraCache.Sample sample = BlightAuraCache.sample(living);

		BlightGroundEffectHelper.tick(living, sample.inBlightAura(), sample.standingOnBlight());
		if (sample.standingOnBlight() && living instanceof ServerPlayer player)
			TimothatysTrinketsCriteriaTriggers.triggerStepOnBlight(player);
	}

	private static boolean shouldProcessThisTick(LivingEntity living) {
		return Math.floorMod(living.tickCount + living.getId(), BlightConfig.GROUND_AURA_TICK_INTERVAL_TICKS) == 0;
	}
}
