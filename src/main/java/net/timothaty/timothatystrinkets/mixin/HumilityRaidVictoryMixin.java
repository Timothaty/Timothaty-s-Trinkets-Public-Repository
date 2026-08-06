package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility.HumilityDeedType;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility.HumilityQuestService;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.raid.Raid;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.UUID;

@Mixin(Raid.class)
public abstract class HumilityRaidVictoryMixin {
	@Shadow @Final private ServerLevel level;
	@Shadow @Final private Set<UUID> heroesOfTheVillage;
	@Unique private boolean timothatys_trinkets$victoryCounted;

	@Inject(method = "tick", at = @At("TAIL"))
	private void timothatys_trinkets$countRaidVictory(CallbackInfo ci) {
		Raid raid = (Raid) (Object) this;
		if (!raid.isVictory()) {
			timothatys_trinkets$victoryCounted = false;
			return;
		}
		if (timothatys_trinkets$victoryCounted)
			return;
		timothatys_trinkets$victoryCounted = true;
		for (UUID playerId : heroesOfTheVillage)
			HumilityQuestService.recordDeed(level.getServer(), playerId, HumilityDeedType.DEFEND_VILLAGE);
	}
}
