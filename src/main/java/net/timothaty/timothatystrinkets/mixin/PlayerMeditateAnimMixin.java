package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.mechanics.pagans_charm.PaganCharmBonuses;
import net.timothaty.timothatystrinkets.mechanics.pagans_charm.PaganCharmMeditationPlayerState;
import net.timothaty.timothatystrinkets.mechanics.pagans_charm.PaganCharmMeditationRules;

import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMeditateAnimMixin implements PaganCharmMeditationPlayerState {
	@Unique
	private static final double timothatys_trinkets$MOVEMENT_EPSILON_SQR = 1.0E-5D;
	@Unique
	private static final int timothatys_trinkets$LOOP_START_TICKS = MEDITATE_TICKS - 1;

	@Unique
	private boolean timothatys_trinkets$meditationInitialized;
	@Unique
	private double timothatys_trinkets$previousX;
	@Unique
	private double timothatys_trinkets$previousY;
	@Unique
	private double timothatys_trinkets$previousZ;
	@Unique
	private int timothatys_trinkets$idleStartTick = -1;
	@Unique
	private int timothatys_trinkets$meditationStartTick = -1;
	@Unique
	private int timothatys_trinkets$loopKeepAliveUntilTick = -1;
	@Unique
	private double timothatys_trinkets$chargeRemainder;
	@Unique
	private int timothatys_trinkets$equipmentCacheTick = Integer.MIN_VALUE;
	@Unique
	private ItemStack timothatys_trinkets$cachedEquippedCharm = ItemStack.EMPTY;
	@Unique
	private int timothatys_trinkets$bonusCacheTick = Integer.MIN_VALUE;
	@Unique
	private PaganCharmBonuses.ChargeBreakdown timothatys_trinkets$cachedChargeBreakdown = PaganCharmBonuses.ChargeBreakdown.EMPTY;
	@Unique
	private final AnimationState timothatys_trinkets$meditateAnimationState = new AnimationState();
	@Unique
	private final AnimationState timothatys_trinkets$meditateLoopAnimationState = new AnimationState();

	@Inject(method = "tick", at = @At("TAIL"))
	private void timothatys_trinkets$tickPaganCharmMeditation(CallbackInfo ci) {
		Player player = (Player) (Object) this;
		boolean moved = this.timothatys_trinkets$hasMovedSinceLastTick(player);

		if (!PaganCharmMeditationRules.canCountAsIdle(player, this.timothatys_trinkets$meditationInitialized, moved)) {
			this.timothatys_trinkets$stopMeditating();
			this.timothatys_trinkets$rememberCurrentPose(player);
			return;
		}

		if (this.timothatys_trinkets$idleStartTick < 0)
			this.timothatys_trinkets$idleStartTick = player.tickCount;

		if (player.tickCount - this.timothatys_trinkets$idleStartTick >= IDLE_TICKS_TO_START) {
			if (this.timothatys_trinkets$meditationStartTick < 0) {
				this.timothatys_trinkets$meditationStartTick = this.timothatys_trinkets$idleStartTick + IDLE_TICKS_TO_START;
				this.timothatys_trinkets$loopKeepAliveUntilTick = -1;
				this.timothatys_trinkets$meditateAnimationState.start(player.tickCount);
				this.timothatys_trinkets$meditateLoopAnimationState.stop();
			}

			if (player.tickCount >= this.timothatys_trinkets$meditationStartTick + timothatys_trinkets$LOOP_START_TICKS) {
				this.timothatys_trinkets$meditateAnimationState.stop();
				this.timothatys_trinkets$meditateLoopAnimationState.startIfStopped(player.tickCount);

				if (player.tickCount + 1 >= this.timothatys_trinkets$loopKeepAliveUntilTick)
					this.timothatys_trinkets$loopKeepAliveUntilTick = player.tickCount + LOOP_KEEP_ALIVE_TICKS;
			}
		} else {
			this.timothatys_trinkets$meditationStartTick = -1;
			this.timothatys_trinkets$loopKeepAliveUntilTick = -1;
			this.timothatys_trinkets$meditateAnimationState.stop();
			this.timothatys_trinkets$meditateLoopAnimationState.stop();
		}

		this.timothatys_trinkets$rememberCurrentPose(player);
	}

	@Override
	public int timothatys_trinkets$getPaganCharmMeditationPhase(float ageInTicks) {
		if (this.timothatys_trinkets$meditationStartTick < 0)
			return PHASE_NONE;

		float activeTicks = this.timothatys_trinkets$getPaganCharmMeditationActiveTicks(ageInTicks);
		if (activeTicks < 0.0F)
			return PHASE_NONE;
		if (activeTicks < timothatys_trinkets$LOOP_START_TICKS)
			return PHASE_MEDITATE;

		return PHASE_LOOP;
	}

	@Override
	public float timothatys_trinkets$getPaganCharmMeditationActiveTicks(float ageInTicks) {
		if (this.timothatys_trinkets$meditationStartTick < 0)
			return -1.0F;

		return ageInTicks - this.timothatys_trinkets$meditationStartTick;
	}

	@Override
	public boolean timothatys_trinkets$isPaganCharmMeditationPrimed() {
		return this.timothatys_trinkets$idleStartTick >= 0 || this.timothatys_trinkets$meditationStartTick >= 0;
	}

	@Override
	public double timothatys_trinkets$getPaganCharmChargeRemainder() {
		return this.timothatys_trinkets$chargeRemainder;
	}

	@Override
	public void timothatys_trinkets$setPaganCharmChargeRemainder(double remainder) {
		this.timothatys_trinkets$chargeRemainder = Math.max(0.0D, remainder);
	}

	@Override
	public int timothatys_trinkets$getPaganCharmEquipmentCacheTick() {
		return this.timothatys_trinkets$equipmentCacheTick;
	}

	@Override
	public ItemStack timothatys_trinkets$getPaganCharmCachedEquippedStack() {
		return this.timothatys_trinkets$cachedEquippedCharm;
	}

	@Override
	public void timothatys_trinkets$setPaganCharmEquipmentCache(int tick, ItemStack charm) {
		this.timothatys_trinkets$equipmentCacheTick = tick;
		this.timothatys_trinkets$cachedEquippedCharm = charm == null ? ItemStack.EMPTY : charm;
	}

	@Override
	public int timothatys_trinkets$getPaganCharmBonusCacheTick() {
		return this.timothatys_trinkets$bonusCacheTick;
	}

	@Override
	public PaganCharmBonuses.ChargeBreakdown timothatys_trinkets$getPaganCharmCachedChargeBreakdown() {
		return this.timothatys_trinkets$cachedChargeBreakdown;
	}

	@Override
	public void timothatys_trinkets$setPaganCharmBonusCache(int tick, PaganCharmBonuses.ChargeBreakdown breakdown) {
		this.timothatys_trinkets$bonusCacheTick = tick;
		this.timothatys_trinkets$cachedChargeBreakdown = breakdown == null ? PaganCharmBonuses.ChargeBreakdown.EMPTY : breakdown;
	}

	@Override
	public void timothatys_trinkets$interruptPaganCharmMeditation() {
		Player player = (Player) (Object) this;
		this.timothatys_trinkets$stopMeditating();
		this.timothatys_trinkets$rememberCurrentPose(player);
	}

	@Override
	public AnimationState timothatys_trinkets$getPaganCharmMeditateAnimationState() {
		return this.timothatys_trinkets$meditateAnimationState;
	}

	@Override
	public AnimationState timothatys_trinkets$getPaganCharmMeditateLoopAnimationState() {
		return this.timothatys_trinkets$meditateLoopAnimationState;
	}

	@Unique
	private boolean timothatys_trinkets$hasMovedSinceLastTick(Player player) {
		if (!this.timothatys_trinkets$meditationInitialized)
			return false;

		double dx = player.getX() - this.timothatys_trinkets$previousX;
		double dy = player.getY() - this.timothatys_trinkets$previousY;
		double dz = player.getZ() - this.timothatys_trinkets$previousZ;
		return dx * dx + dy * dy + dz * dz > timothatys_trinkets$MOVEMENT_EPSILON_SQR;
	}

	@Unique
	private void timothatys_trinkets$rememberCurrentPose(Player player) {
		this.timothatys_trinkets$previousX = player.getX();
		this.timothatys_trinkets$previousY = player.getY();
		this.timothatys_trinkets$previousZ = player.getZ();
		this.timothatys_trinkets$meditationInitialized = true;
	}

	@Unique
	private void timothatys_trinkets$stopMeditating() {
		this.timothatys_trinkets$idleStartTick = -1;
		this.timothatys_trinkets$meditationStartTick = -1;
		this.timothatys_trinkets$loopKeepAliveUntilTick = -1;
		this.timothatys_trinkets$chargeRemainder = 0.0D;
		this.timothatys_trinkets$meditateAnimationState.stop();
		this.timothatys_trinkets$meditateLoopAnimationState.stop();
	}
}
