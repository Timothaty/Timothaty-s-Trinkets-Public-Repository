package net.timothaty.timothatystrinkets.mechanics.armlet_gauntlet_synergy;

import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class SoulEmpowerEvents {
	private SoulEmpowerEvents() {
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Pre event) {
		if (!event.getEntity().level().isClientSide()) {
			SoulEmpowerHelper.refreshAttributeModifiers(event.getEntity());
		}
	}

	@SubscribeEvent
	public static void onEffectAdded(MobEffectEvent.Added event) {
		MobEffectInstance instance = event.getEffectInstance();
		if (!event.getEntity().level().isClientSide() && isSoulEmpower(instance)) {
			int incomingLevel = instance.getAmplifier() + 1;
			SoulEmpowerHelper.setAttributeModifiersForLevel(event.getEntity(), Math.max(SoulEmpowerHelper.getLevel(event.getEntity()), incomingLevel));
		}
	}

	@SubscribeEvent
	public static void onEffectRemoved(MobEffectEvent.Remove event) {
		if (!event.getEntity().level().isClientSide() && event.getEffect().value() == TimothatysTrinketsModMobEffects.SOUL_EMPOWER.get()) {
			SoulEmpowerHelper.clearAttributeModifiers(event.getEntity());
		}
	}

	@SubscribeEvent
	public static void onEffectExpired(MobEffectEvent.Expired event) {
		if (!event.getEntity().level().isClientSide() && isSoulEmpower(event.getEffectInstance())) {
			SoulEmpowerHelper.clearAttributeModifiers(event.getEntity());
		}
	}

	private static boolean isSoulEmpower(MobEffectInstance instance) {
		return instance != null && instance.getEffect().value() == TimothatysTrinketsModMobEffects.SOUL_EMPOWER.get();
	}
}
