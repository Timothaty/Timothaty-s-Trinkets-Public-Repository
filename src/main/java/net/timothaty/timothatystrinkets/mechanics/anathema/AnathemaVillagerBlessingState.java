package net.timothaty.timothatystrinkets.mechanics.anathema;

import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.player.Player;

public interface AnathemaVillagerBlessingState {
	int BLESSINGS_DURATION_TICKS = 30;
	byte BLESSINGS_ENTITY_EVENT = -65;
	byte BLESSINGS_STOP_ENTITY_EVENT = -69;

	void timothatys_trinkets$startBlessingsAnimation();

	void timothatys_trinkets$startBlessingsAnimation(Player recipient);

	void timothatys_trinkets$stopBlessingsAnimation();

	boolean timothatys_trinkets$isBlessingsAnimationActive();

	AnimationState timothatys_trinkets$getBlessingsAnimationState();

	boolean timothatys_trinkets$claimBlessingsParticleTick(int tickCount);
}
