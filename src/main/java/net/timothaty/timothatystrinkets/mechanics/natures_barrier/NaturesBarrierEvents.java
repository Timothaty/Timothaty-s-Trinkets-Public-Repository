package net.timothaty.timothatystrinkets.mechanics.natures_barrier;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.item.PagansCharmItem;
import net.timothaty.timothatystrinkets.mechanics.pagans_charm.PaganCharmCharge;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class NaturesBarrierEvents {
	private NaturesBarrierEvents() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
		if (event == null || event.getNewDamage() <= 0.0F)
			return;

		LivingEntity target = event.getEntity();
		if (target == null || !target.isAlive() || target.level().isClientSide() || target instanceof ArmorStand)
			return;

		if (!NaturesBarrierState.hasBarrier(target) && target instanceof Player player) {
			tryActivateFromPaganCharm(player, event.getNewDamage());
		}

		if (NaturesBarrierState.hasBarrier(target)) {
			absorbDamage(event, target);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onBarrierAdded(MobEffectEvent.Added event) {
		MobEffectInstance instance = event.getEffectInstance();
		if (!isNaturesBarrier(instance))
			return;

		LivingEntity entity = event.getEntity();
		if (entity == null || entity.level().isClientSide())
			return;

		NaturesBarrierState.resetAbsorption(entity, instance);
		NaturesBarrierSounds.playActivation(entity);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onBarrierRemoved(MobEffectEvent.Remove event) {
		if (event.getEffect().value() != TimothatysTrinketsModMobEffects.NATURES_BARRIER.get())
			return;

		handleBarrierEnd(event.getEntity());
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onBarrierExpired(MobEffectEvent.Expired event) {
		if (!isNaturesBarrier(event.getEffectInstance()))
			return;

		handleBarrierEnd(event.getEntity());
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		handleBarrierDeath(event.getEntity());
	}

	private static void tryActivateFromPaganCharm(Player player, float incomingDamage) {
		if (player == null || player.isSpectator() || player.isCreative())
			return;
		if (player.getCooldowns().isOnCooldown(TimothatysTrinketsModItems.PAGANS_CHARM.get()))
			return;
		if (!shouldActivate(player, incomingDamage))
			return;

		ItemStack charm = PaganCharmCharge.findEquippedCharm(player);
		if (charm.isEmpty())
			return;
		if (PagansCharmItem.getCharge(charm) < NaturesBarrierTuning.PAGANS_CHARM_CHARGE_COST)
			return;

		MobEffectInstance barrier = new MobEffectInstance(
				TimothatysTrinketsModMobEffects.NATURES_BARRIER,
				NaturesBarrierTuning.DURATION_TICKS,
				0,
				false,
				false,
				true);
		if (!player.addEffect(barrier, player))
			return;

		PagansCharmItem.setCharge(charm, PagansCharmItem.getCharge(charm) - NaturesBarrierTuning.PAGANS_CHARM_CHARGE_COST);
		player.getCooldowns().addCooldown(TimothatysTrinketsModItems.PAGANS_CHARM.get(), NaturesBarrierTuning.PAGANS_CHARM_COOLDOWN_TICKS);
	}

	private static boolean shouldActivate(Player player, float incomingDamage) {
		float threshold = player.getMaxHealth() * NaturesBarrierTuning.ACTIVATION_HEALTH_RATIO;
		return player.getHealth() <= threshold || player.getHealth() - incomingDamage < threshold;
	}

	private static void absorbDamage(LivingDamageEvent.Pre event, LivingEntity target) {
		MobEffectInstance barrier = target.getEffect(TimothatysTrinketsModMobEffects.NATURES_BARRIER);
		if (barrier == null)
			return;

		float damage = event.getNewDamage();
		float remaining = NaturesBarrierState.getOrCreateRemainingAbsorption(target, barrier);
		float absorbed = Math.min(damage, remaining);
		if (absorbed <= 0.0F)
			return;

		NaturesBarrierState.markBarrierHurtSound(target);
		float newRemaining = remaining - absorbed;
		float newDamage = damage - absorbed;
		event.setNewDamage(Math.max(0.0F, newDamage));

		if (newRemaining <= 0.001F) {
			NaturesBarrierState.clear(target);
			target.removeEffect(TimothatysTrinketsModMobEffects.NATURES_BARRIER);
			NaturesBarrierSounds.playEnd(target);
		} else {
			NaturesBarrierState.setRemainingAbsorption(target, newRemaining);
		}
	}

	private static void handleBarrierEnd(LivingEntity entity) {
		if (entity == null || entity.level().isClientSide())
			return;
		if (!NaturesBarrierState.hasStoredAbsorption(entity))
			return;

		NaturesBarrierState.clear(entity);
		if (entity.isAlive()) {
			NaturesBarrierSounds.playEnd(entity);
		}
	}

	private static void handleBarrierDeath(LivingEntity entity) {
		if (entity == null || entity.level().isClientSide())
			return;

		NaturesBarrierState.clear(entity);
	}

	private static boolean isNaturesBarrier(MobEffectInstance instance) {
		return instance != null && instance.getEffect().value() == TimothatysTrinketsModMobEffects.NATURES_BARRIER.get();
	}
}
