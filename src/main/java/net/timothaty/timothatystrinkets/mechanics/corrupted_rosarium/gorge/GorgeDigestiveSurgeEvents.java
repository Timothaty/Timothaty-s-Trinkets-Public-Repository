package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.gorge;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class GorgeDigestiveSurgeEvents {
	private GorgeDigestiveSurgeEvents() {
	}

	@SubscribeEvent
	public static void onLivingDamagePost(LivingDamageEvent.Post event) {
		if (event == null || event.getNewDamage() <= 0.0F)
			return;
		if (!(event.getEntity() instanceof ServerPlayer player)
				|| player.level().isClientSide()
				|| !player.isAlive()
				|| player.isDeadOrDying()
				|| player.isRemoved()
				|| !GorgeState.hasActiveSession(player)
				|| !player.hasEffect(TimothatysTrinketsModMobEffects.GORGE)
				|| event.getSource() == null
				|| event.getSource().is(
						DamageTypeTags.BYPASSES_INVULNERABILITY
				)) {
			return;
		}

		GorgeState.tryScheduleDigestiveSurge(player);
	}
}
