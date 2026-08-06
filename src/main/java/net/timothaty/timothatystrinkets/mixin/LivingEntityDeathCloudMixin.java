package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.mechanics.ritual_dagger.RitualDaggerCurioHandler;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDeathCloudMixin {

	@Redirect(
			method = "tickDeath",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/Level;broadcastEntityEvent(Lnet/minecraft/world/entity/Entity;B)V"
			),
			require = 0
	)
	private void timothatys_trinkets$skipDeathPoofOnRitualDaggerProc(Level level, Entity entity, byte eventId) {
		if (eventId == 60 && entity instanceof Player player
				&& RitualDaggerCurioHandler.isRitualDaggerProcActive(player)) {
			RitualDaggerCurioHandler.clearRitualDaggerProcActive(player);
			return;
		}

		level.broadcastEntityEvent(entity, eventId);
	}
}
