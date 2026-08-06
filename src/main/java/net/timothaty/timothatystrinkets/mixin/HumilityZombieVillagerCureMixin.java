package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility.HumilityDeedType;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility.HumilityQuestService;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.ZombieVillager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ZombieVillager.class)
public abstract class HumilityZombieVillagerCureMixin {
	@Unique
	private static final String timothatys_trinkets$CURE_STARTER = "ttr_humility_cure_starter";

	@Inject(method = "startConverting", at = @At("HEAD"))
	private void timothatys_trinkets$rememberCureStarter(UUID playerId, int conversionTime, CallbackInfo ci) {
		if (playerId != null)
			((ZombieVillager) (Object) this).getPersistentData().putUUID(timothatys_trinkets$CURE_STARTER, playerId);
	}

	@Inject(method = "finishConversion", at = @At("TAIL"))
	private void timothatys_trinkets$countCompletedCure(ServerLevel level, CallbackInfo ci) {
		ZombieVillager zombie = (ZombieVillager) (Object) this;
		if (zombie.getPersistentData().hasUUID(timothatys_trinkets$CURE_STARTER))
			HumilityQuestService.recordDeed(level.getServer(), zombie.getPersistentData().getUUID(timothatys_trinkets$CURE_STARTER), HumilityDeedType.CURE_VILLAGER);
	}
}
