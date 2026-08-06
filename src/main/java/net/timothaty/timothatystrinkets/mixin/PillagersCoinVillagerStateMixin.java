package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.mechanics.pillagers_coin.PillagersCoinVillagerFearEvents;
import net.timothaty.timothatystrinkets.mechanics.pillagers_coin.PillagersCoinVillagerRuntimeState;
import net.timothaty.timothatystrinkets.mechanics.pillagers_coin.PillagersCoinVillagerSleepEvents;

import net.minecraft.world.entity.npc.Villager;

import java.util.UUID;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class PillagersCoinVillagerStateMixin implements PillagersCoinVillagerRuntimeState {
	@Unique
	private boolean timothatys_trinkets$extortionVisualActive;
	@Unique
	private int timothatys_trinkets$fearVisualTicks;
	@Unique
	private long timothatys_trinkets$pillagersCoinHideUntil;
	@Unique
	@Nullable
	private UUID timothatys_trinkets$pillagersCoinFearedPlayer;
	@Unique
	private long timothatys_trinkets$returnToSleepSettleUntil;

	@Override
	public void timothatys_trinkets$startExtortionVisual() {
		Villager villager = (Villager) (Object) this;
		this.timothatys_trinkets$extortionVisualActive = true;
		if (!villager.level().isClientSide())
			villager.level().broadcastEntityEvent(villager, EXTORTION_VISUAL_START_EVENT);
	}

	@Override
	public void timothatys_trinkets$stopExtortionVisual() {
		Villager villager = (Villager) (Object) this;
		this.timothatys_trinkets$extortionVisualActive = false;
		if (!villager.level().isClientSide())
			villager.level().broadcastEntityEvent(villager, EXTORTION_VISUAL_STOP_EVENT);
	}

	@Override
	public boolean timothatys_trinkets$isExtortionVisualActive() {
		return this.timothatys_trinkets$extortionVisualActive;
	}

	@Override
	public void timothatys_trinkets$startFearVisualPulse() {
		Villager villager = (Villager) (Object) this;
		if (villager.level().isClientSide())
			this.timothatys_trinkets$fearVisualTicks = FEAR_VISUAL_PULSE_DURATION_TICKS;
		else
			villager.level().broadcastEntityEvent(villager, FEAR_VISUAL_PULSE_EVENT);
	}

	@Override
	public boolean timothatys_trinkets$isFearVisualActive() {
		return this.timothatys_trinkets$fearVisualTicks > 0;
	}

	@Override
	public long timothatys_trinkets$getPillagersCoinHideUntil() {
		return this.timothatys_trinkets$pillagersCoinHideUntil;
	}

	@Override
	public void timothatys_trinkets$setPillagersCoinHideUntil(long hideUntil) {
		this.timothatys_trinkets$pillagersCoinHideUntil = hideUntil;
	}

	@Override
	@Nullable
	public UUID timothatys_trinkets$getPillagersCoinFearedPlayer() {
		return this.timothatys_trinkets$pillagersCoinFearedPlayer;
	}

	@Override
	public void timothatys_trinkets$setPillagersCoinFearedPlayer(@Nullable UUID playerId) {
		this.timothatys_trinkets$pillagersCoinFearedPlayer = playerId;
	}

	@Override
	public long timothatys_trinkets$getReturnToSleepSettleUntil() {
		return this.timothatys_trinkets$returnToSleepSettleUntil;
	}

	@Override
	public void timothatys_trinkets$setReturnToSleepSettleUntil(long settleUntil) {
		this.timothatys_trinkets$returnToSleepSettleUntil = settleUntil;
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void timothatys_trinkets$tickPillagersCoinFear(CallbackInfo ci) {
		Villager villager = (Villager) (Object) this;
		PillagersCoinVillagerSleepEvents.tickReturnToSleepSettle(villager, this);
		PillagersCoinVillagerFearEvents.tickFearState(villager, this);
		if (villager.level().isClientSide() && this.timothatys_trinkets$fearVisualTicks > 0)
			this.timothatys_trinkets$fearVisualTicks--;
	}

	@Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$handleExtortionVisualEvent(byte eventId, CallbackInfo ci) {
		if (eventId == EXTORTION_VISUAL_START_EVENT) {
			timothatys_trinkets$startExtortionVisual();
			ci.cancel();
		} else if (eventId == EXTORTION_VISUAL_STOP_EVENT) {
			timothatys_trinkets$stopExtortionVisual();
			ci.cancel();
		} else if (eventId == FEAR_VISUAL_PULSE_EVENT) {
			timothatys_trinkets$startFearVisualPulse();
			ci.cancel();
		}
	}
}
