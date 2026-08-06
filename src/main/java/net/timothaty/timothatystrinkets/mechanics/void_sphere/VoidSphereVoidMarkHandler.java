package net.timothaty.timothatystrinkets.mechanics.void_sphere;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.pact.PactOfAllianceHelper;
import net.timothaty.timothatystrinkets.util.TimothatysCuriosHelper;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public class VoidSphereVoidMarkHandler {

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
		if (event == null)
			return;

		LivingEntity target = event.getEntity();
		if (target == null || !target.isAlive())
			return;

		DamageSource source = event.getSource();
		if (source == null)
			return;

		Entity attackerEntity = source.getEntity();
		if (!(attackerEntity instanceof Player player))
			return;

		if (player.level().isClientSide())
			return;

		if (target == player)
			return;

		if (PactOfAllianceHelper.areAllied(player, target))
			return;

		if (!isVoidSphereEquippedCurios(player))
			return;

		if (player.getCooldowns().isOnCooldown(TimothatysTrinketsModItems.VOID_SPHERE.get()))
			return;

		if (target.hasEffect(TimothatysTrinketsModMobEffects.MARKED_BY_VOID))
			return;

		if (player.getRandom().nextFloat() >= VoidSphereData.MARK_CHANCE)
			return;

		boolean applied = target.addEffect(new MobEffectInstance(TimothatysTrinketsModMobEffects.MARKED_BY_VOID, VoidSphereData.MARK_DURATION_TICKS, 0, false, true, true), player);
		if (!applied)
			return;

		playMarkedByVoidAppliedSound(target);
		MarkedByVoidHandler.beginMark(target, player);
		player.getCooldowns().addCooldown(TimothatysTrinketsModItems.VOID_SPHERE.get(), VoidSphereData.VOID_SPHERE_COOLDOWN_TICKS);
	}

	private static void playMarkedByVoidAppliedSound(LivingEntity target) {
		target.level().playSound(null, target.getX(), target.getY(), target.getZ(), TimothatysTrinketsModSounds.MARKED_BY_VOID_APPLIED.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
	}

	private static boolean isVoidSphereEquippedCurios(Player player) {
		return TimothatysCuriosHelper.hasCurio(player, TimothatysTrinketsModItems.VOID_SPHERE.get());
	}
}
