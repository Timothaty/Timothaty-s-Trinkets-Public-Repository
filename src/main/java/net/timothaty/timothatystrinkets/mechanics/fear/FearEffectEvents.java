package net.timothaty.timothatystrinkets.mechanics.fear;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.network.FearEffectClientMessage;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.server.level.ServerPlayer;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class FearEffectEvents {
	private FearEffectEvents() {
	}

	@SubscribeEvent
	public static void onFearAdded(MobEffectEvent.Added event) {
		if (event.getEffectInstance().getEffect().value() != TimothatysTrinketsModMobEffects.FEAR.get())
			return;

		if (event.getEntity() instanceof ServerPlayer player) {
			PacketDistributor.sendToPlayer(player, FearEffectClientMessage.INSTANCE);
		}
	}
}
