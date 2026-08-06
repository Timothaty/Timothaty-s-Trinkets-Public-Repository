package net.timothaty.timothatystrinkets.mechanics.pillagers_coin;

import java.util.UUID;

import javax.annotation.Nullable;

public interface PillagersCoinVillagerRuntimeState {
	byte EXTORTION_VISUAL_START_EVENT = -66;
	byte EXTORTION_VISUAL_STOP_EVENT = -67;
	byte FEAR_VISUAL_PULSE_EVENT = -68;
	int FEAR_VISUAL_PULSE_DURATION_TICKS = 20;

	void timothatys_trinkets$startExtortionVisual();

	void timothatys_trinkets$stopExtortionVisual();

	boolean timothatys_trinkets$isExtortionVisualActive();

	void timothatys_trinkets$startFearVisualPulse();

	boolean timothatys_trinkets$isFearVisualActive();

	long timothatys_trinkets$getPillagersCoinHideUntil();

	void timothatys_trinkets$setPillagersCoinHideUntil(long hideUntil);

	@Nullable
	UUID timothatys_trinkets$getPillagersCoinFearedPlayer();

	void timothatys_trinkets$setPillagersCoinFearedPlayer(@Nullable UUID playerId);

	long timothatys_trinkets$getReturnToSleepSettleUntil();

	void timothatys_trinkets$setReturnToSleepSettleUntil(long settleUntil);
}
