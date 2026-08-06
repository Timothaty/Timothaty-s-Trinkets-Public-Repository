package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaVillagerBlessingState;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaVillagerFearEvents;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaVillagerRuntimeState;

import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class AnathemaVillagerBlessingStateMixin implements AnathemaVillagerBlessingState, AnathemaVillagerRuntimeState {
	@Unique
	private final AnimationState timothatys_trinkets$blessingsAnimationState = new AnimationState();
	@Unique
	private int timothatys_trinkets$blessingsAnimationTicks;
	@Unique
	private int timothatys_trinkets$lastBlessingsParticleTick = Integer.MIN_VALUE;
	@Unique
	private long timothatys_trinkets$anathemaHideUntil;
	@Unique
	private UUID timothatys_trinkets$blessingsRecipient;

	@Override
	public void timothatys_trinkets$startBlessingsAnimation() {
		Villager villager = (Villager) (Object) this;
		this.timothatys_trinkets$blessingsAnimationTicks = BLESSINGS_DURATION_TICKS;
		if (villager.level().isClientSide()) {
			this.timothatys_trinkets$blessingsAnimationState.start(villager.tickCount);
		} else {
			villager.getNavigation().stop();
			villager.level().broadcastEntityEvent(villager, BLESSINGS_ENTITY_EVENT);
		}
	}

	@Override
	public void timothatys_trinkets$startBlessingsAnimation(Player recipient) {
		Villager villager = (Villager) (Object) this;
		if (!villager.level().isClientSide())
			this.timothatys_trinkets$blessingsRecipient = recipient.getUUID();
		timothatys_trinkets$startBlessingsAnimation();
	}

	@Override
	public void timothatys_trinkets$stopBlessingsAnimation() {
		Villager villager = (Villager) (Object) this;
		this.timothatys_trinkets$blessingsAnimationTicks = 0;
		this.timothatys_trinkets$blessingsRecipient = null;
		if (villager.level().isClientSide())
			this.timothatys_trinkets$blessingsAnimationState.stop();
		else
			villager.level().broadcastEntityEvent(villager, BLESSINGS_STOP_ENTITY_EVENT);
	}

	@Override
	public boolean timothatys_trinkets$isBlessingsAnimationActive() {
		return this.timothatys_trinkets$blessingsAnimationTicks > 0;
	}

	@Override
	public AnimationState timothatys_trinkets$getBlessingsAnimationState() {
		return this.timothatys_trinkets$blessingsAnimationState;
	}

	@Override
	public boolean timothatys_trinkets$claimBlessingsParticleTick(int tickCount) {
		if (this.timothatys_trinkets$lastBlessingsParticleTick == tickCount)
			return false;
		this.timothatys_trinkets$lastBlessingsParticleTick = tickCount;
		return true;
	}

	@Override
	public long timothatys_trinkets$getAnathemaHideUntil() {
		return this.timothatys_trinkets$anathemaHideUntil;
	}

	@Override
	public void timothatys_trinkets$setAnathemaHideUntil(long hideUntil) {
		this.timothatys_trinkets$anathemaHideUntil = hideUntil;
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void timothatys_trinkets$tickBlessingsAnimation(CallbackInfo ci) {
		Villager villager = (Villager) (Object) this;
		AnathemaVillagerFearEvents.tickFearState(villager, this);
		if (this.timothatys_trinkets$blessingsAnimationTicks > 0) {
			this.timothatys_trinkets$blessingsAnimationTicks--;
			if (villager.level().isClientSide())
				this.timothatys_trinkets$blessingsAnimationState.startIfStopped(villager.tickCount);
			else {
				villager.getNavigation().stop();
				timothatys_trinkets$lookAtBlessingsRecipient(villager);
			}
		} else if (villager.level().isClientSide()) {
			this.timothatys_trinkets$blessingsAnimationState.stop();
		} else {
			this.timothatys_trinkets$blessingsRecipient = null;
		}
	}

	@Unique
	private void timothatys_trinkets$lookAtBlessingsRecipient(Villager villager) {
		if (this.timothatys_trinkets$blessingsRecipient == null || !(villager.level() instanceof ServerLevel serverLevel))
			return;

		Player recipient = serverLevel.getPlayerByUUID(this.timothatys_trinkets$blessingsRecipient);
		if (recipient == null || !recipient.isAlive() || recipient.distanceToSqr(villager) > 256.0D) {
			this.timothatys_trinkets$blessingsRecipient = null;
			return;
		}

		villager.getLookControl().setLookAt(recipient, 30.0F, 30.0F);
	}

	@Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
	private void timothatys_trinkets$handleBlessingsEntityEvent(byte eventId, CallbackInfo ci) {
		if (eventId == BLESSINGS_ENTITY_EVENT) {
			timothatys_trinkets$startBlessingsAnimation();
			ci.cancel();
		} else if (eventId == BLESSINGS_STOP_ENTITY_EVENT) {
			timothatys_trinkets$stopBlessingsAnimation();
			ci.cancel();
		}
	}
}
