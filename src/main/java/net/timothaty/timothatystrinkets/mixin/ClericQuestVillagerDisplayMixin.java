package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.mechanics.cleric_quests.display.ClericQuestRewardDisplayState;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.npc.Villager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class ClericQuestVillagerDisplayMixin implements ClericQuestRewardDisplayState {
	@Unique
	private static final EntityDataAccessor<Byte> timothatys_trinkets$CLERIC_QUEST_REWARD_DISPLAY = SynchedEntityData.defineId(Villager.class, EntityDataSerializers.BYTE);

	@Inject(method = "defineSynchedData", at = @At("TAIL"))
	private void timothatys_trinkets$defineClericQuestRewardDisplay(SynchedEntityData.Builder builder, CallbackInfo ci) {
		builder.define(timothatys_trinkets$CLERIC_QUEST_REWARD_DISPLAY, NONE);
	}

	@Override
	public byte timothatys_trinkets$getClericQuestRewardDisplay() {
		return ((Villager) (Object) this).getEntityData().get(timothatys_trinkets$CLERIC_QUEST_REWARD_DISPLAY);
	}

	@Override
	public void timothatys_trinkets$setClericQuestRewardDisplay(byte displayType) {
		byte clamped = displayType < NONE || displayType > SACRAMENT ? NONE : displayType;
		Villager villager = (Villager) (Object) this;
		if (villager.getEntityData().get(timothatys_trinkets$CLERIC_QUEST_REWARD_DISPLAY) != clamped)
			villager.getEntityData().set(timothatys_trinkets$CLERIC_QUEST_REWARD_DISPLAY, clamped);
	}
}
