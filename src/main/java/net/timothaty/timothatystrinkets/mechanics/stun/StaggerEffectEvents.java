package net.timothaty.timothatystrinkets.mechanics.stun;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.potion.StaggerMobEffect;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class StaggerEffectEvents {
	private StaggerEffectEvents() {
	}

	@SubscribeEvent
	public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide()
				|| !(event.getEntity() instanceof LivingEntity living)) {
			return;
		}

		var effect = living.getEffect(TimothatysTrinketsModMobEffects.STAGGER);
		if (effect != null) {
			effect.getEffect().value().addAttributeModifiers(living.getAttributes(), effect.getAmplifier());
			return;
		}

		var movement = living.getAttribute(Attributes.MOVEMENT_SPEED);
		if (movement != null && movement.getModifier(StaggerMobEffect.SPEED_REDUCTION_ID) != null) {
			movement.removeModifier(StaggerMobEffect.SPEED_REDUCTION_ID);
		}
	}
}
