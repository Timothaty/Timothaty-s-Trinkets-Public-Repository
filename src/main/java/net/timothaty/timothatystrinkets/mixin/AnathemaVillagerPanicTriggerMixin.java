package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaVillagerFearEvents;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.VillagerPanicTrigger;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerPanicTrigger.class)
public abstract class AnathemaVillagerPanicTriggerMixin {
	@Inject(method = "start", at = @At("TAIL"), require = 0)
	private void timothatys_trinkets$hideFromAnathema(ServerLevel level, Villager villager, long gameTime, CallbackInfo ci) {
		if (AnathemaVillagerFearEvents.shouldHideFromAnathema(villager, gameTime))
			villager.getBrain().setActiveActivityIfPossible(Activity.HIDE);
	}
}
