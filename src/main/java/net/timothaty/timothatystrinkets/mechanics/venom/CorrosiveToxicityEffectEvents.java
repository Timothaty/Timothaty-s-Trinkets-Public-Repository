package net.timothaty.timothatystrinkets.mechanics.venom;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class CorrosiveToxicityEffectEvents {
	private CorrosiveToxicityEffectEvents() {
	}

	@SubscribeEvent
	public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living))
			return;

		MobEffectInstance effect = living.getEffect(TimothatysTrinketsModMobEffects.CORROSIVE_TOXICITY);
		if (effect != null) {
			effect.getEffect().value().addAttributeModifiers(living.getAttributes(), effect.getAmplifier());
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onMobEffectRemove(MobEffectEvent.Remove event) {
		if (event.isCanceled() || event.getEffect().value() != TimothatysTrinketsModMobEffects.CORROSIVE_TOXICITY.get())
			return;

		cleanupCorrosiveToxicity(event.getEntity());
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onMobEffectExpired(MobEffectEvent.Expired event) {
		if (event.isCanceled() || !isCorrosiveToxicity(event.getEffectInstance()))
			return;

		cleanupCorrosiveToxicity(event.getEntity());
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		LivingEntity entity = event.getEntity();
		if (entity == null)
			return;

		cleanupCorrosiveToxicity(entity);
	}

	private static void cleanupCorrosiveToxicity(LivingEntity entity) {
		VenomSphereTargetTracker.forgetOwnerFromTargetData(entity);
	}

	private static boolean isCorrosiveToxicity(MobEffectInstance instance) {
		return instance != null && instance.getEffect().value() == TimothatysTrinketsModMobEffects.CORROSIVE_TOXICITY.get();
	}

}
