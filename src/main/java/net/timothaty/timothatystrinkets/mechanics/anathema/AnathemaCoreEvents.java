package net.timothaty.timothatystrinkets.mechanics.anathema;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class AnathemaCoreEvents {
	private AnathemaCoreEvents() {
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onEffectRemoved(MobEffectEvent.Remove event) {
		if (event.getEffect().value() != TimothatysTrinketsModMobEffects.ANATHEMA.get())
			return;
		if (!AnathemaHelper.isRemovalAllowed(event.getEntity()))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		if (!event.isWasDeath())
			return;

		MobEffectInstance original = event.getOriginal().getEffect(TimothatysTrinketsModMobEffects.ANATHEMA);
		if (original == null || original.getDuration() <= 0)
			return;

		event.getEntity().addEffect(new MobEffectInstance(
			TimothatysTrinketsModMobEffects.ANATHEMA,
			original.getDuration(),
			Math.min(AnathemaData.MAX_LEVEL - 1, original.getAmplifier()),
			original.isAmbient(),
			false,
			original.showIcon()
		));
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player.level().isClientSide() || Math.floorMod(player.tickCount + player.getId(), 20) != 0)
			return;

		MobEffectInstance instance = player.getEffect(TimothatysTrinketsModMobEffects.ANATHEMA);
		if (instance != null && (instance.isVisible() || instance.getAmplifier() >= AnathemaData.MAX_LEVEL))
			AnathemaHelper.replacePreservingDuration(player, instance);
	}

	@SubscribeEvent
	public static void onEffectParticle(EffectParticleModificationEvent event) {
		if (event.getEffect().getEffect().value() == TimothatysTrinketsModMobEffects.ANATHEMA.get())
			event.setVisible(false);
	}

	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
		if (!(event.getEntity() instanceof Player player))
			return;
		if (!(event.getSource().getEntity() instanceof IronGolem))
			return;

		int level = AnathemaHelper.getLevel(player);
		if (level > 0)
			event.setNewDamage(event.getNewDamage() * (1.0F + AnathemaData.IRON_GOLEM_DAMAGE_PER_LEVEL * level));
	}
}
