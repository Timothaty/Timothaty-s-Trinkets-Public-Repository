package net.timothaty.timothatystrinkets.mechanics.vampiric_fangs;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.potion.HaemorrhageMobEffect;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class HaemorrhageEvents {
	private HaemorrhageEvents() {}

	@SubscribeEvent
	public static void onLivingHeal(LivingHealEvent event) {
		LivingEntity entity = event.getEntity();
		if (entity == null || !entity.hasEffect(TimothatysTrinketsModMobEffects.HAEMORRHAGE))
			return;
		float factor = entity instanceof Player ? 0.50F : 0.30F;
		event.setAmount(event.getAmount() * factor);
	}

	@SubscribeEvent
	public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide()
				|| !(event.getEntity() instanceof LivingEntity living)) {
			return;
		}

		var effect = living.getEffect(TimothatysTrinketsModMobEffects.HAEMORRHAGE);
		if (effect != null) {
			effect.getEffect().value().addAttributeModifiers(living.getAttributes(), effect.getAmplifier());
			return;
		}

		var movement = living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
		if (movement != null && movement.getModifier(HaemorrhageMobEffect.SLOW_MODIFIER_ID) != null) {
			movement.removeModifier(HaemorrhageMobEffect.SLOW_MODIFIER_ID);
		}
	}
}
