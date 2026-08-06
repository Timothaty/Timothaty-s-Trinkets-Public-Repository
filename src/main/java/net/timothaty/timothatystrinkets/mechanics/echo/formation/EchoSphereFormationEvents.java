package net.timothaty.timothatystrinkets.mechanics.echo.formation;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.monster.warden.Warden;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class EchoSphereFormationEvents {
	private EchoSphereFormationEvents() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
		if (!(event.getEntity() instanceof ServerPlayer player)
				|| !player.isAlive()
				|| player.isCreative()
				|| player.isSpectator()
				|| event.getNewDamage() <= 0.0F
				|| !event.getSource().is(DamageTypes.SONIC_BOOM)
				|| !(event.getSource().getEntity() instanceof Warden warden)) {
			return;
		}

		if (EchoSphereFormationHelper.hasCreatedEchoSphere(warden)) {
			return;
		}

		BlockPos spherePos = EchoSphereFormationHelper.findNearestDormantSphere(player).orElse(null);
		if (spherePos == null
				|| !EchoSphereFormationHelper.awakenDormantSphere(player.serverLevel(), spherePos)) {
			return;
		}

		EchoSphereFormationHelper.markEchoSphereCreated(warden);
		event.setNewDamage(event.getNewDamage() * 0.45F);
		EchoSphereFormationVisuals.spawnSonicTransfer(player.serverLevel(), player, spherePos);
		player.serverLevel().playSound(null, spherePos, SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.BLOCKS, 1.15F, 0.75F);
	}
}
