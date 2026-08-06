package net.timothaty.timothatystrinkets.mechanics.pagans_charm;

import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.item.ItemStack;

public interface PaganCharmMeditationPlayerState {
	int PHASE_NONE = 0;
	int PHASE_MEDITATE = 1;
	int PHASE_LOOP = 2;

	int IDLE_TICKS_TO_START = PaganCharmTuning.IDLE_TICKS_TO_START;
	int MEDITATE_TICKS = PaganCharmTuning.MEDITATE_TICKS;
	int LOOP_KEEP_ALIVE_TICKS = PaganCharmTuning.LOOP_KEEP_ALIVE_TICKS;
	int CHARGE_START_TICKS = PaganCharmTuning.CHARGE_START_TICKS;
	int BONUS_CACHE_INTERVAL_TICKS = PaganCharmTuning.BONUS_CACHE_INTERVAL_TICKS;

	int timothatys_trinkets$getPaganCharmMeditationPhase(float ageInTicks);

	float timothatys_trinkets$getPaganCharmMeditationActiveTicks(float ageInTicks);

	boolean timothatys_trinkets$isPaganCharmMeditationPrimed();

	double timothatys_trinkets$getPaganCharmChargeRemainder();

	void timothatys_trinkets$setPaganCharmChargeRemainder(double remainder);

	int timothatys_trinkets$getPaganCharmEquipmentCacheTick();

	ItemStack timothatys_trinkets$getPaganCharmCachedEquippedStack();

	void timothatys_trinkets$setPaganCharmEquipmentCache(int tick, ItemStack charm);

	int timothatys_trinkets$getPaganCharmBonusCacheTick();

	PaganCharmBonuses.ChargeBreakdown timothatys_trinkets$getPaganCharmCachedChargeBreakdown();

	void timothatys_trinkets$setPaganCharmBonusCache(int tick, PaganCharmBonuses.ChargeBreakdown breakdown);

	void timothatys_trinkets$interruptPaganCharmMeditation();

	AnimationState timothatys_trinkets$getPaganCharmMeditateAnimationState();

	AnimationState timothatys_trinkets$getPaganCharmMeditateLoopAnimationState();
}
