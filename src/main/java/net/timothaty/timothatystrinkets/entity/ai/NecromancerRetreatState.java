package net.timothaty.timothatystrinkets.entity.ai;

import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

public class NecromancerRetreatState {
	private final int durationTicks;

	private int ticksLeft;
	@Nullable
	private LivingEntity retreatTarget;

	public NecromancerRetreatState(int durationTicks) {
		this.durationTicks = durationTicks;
	}

	public void startRetreat(LivingEntity attacker) {
		this.retreatTarget = attacker;
		this.ticksLeft = durationTicks;
	}

	public void tick() {
		if (ticksLeft > 0) {
			ticksLeft--;
		}

		if (retreatTarget != null && (!retreatTarget.isAlive() || ticksLeft <= 0)) {
			retreatTarget = null;
		}
	}

	public boolean shouldRetreat() {
		return ticksLeft > 0 && retreatTarget != null && retreatTarget.isAlive();
	}

	@Nullable
	public LivingEntity getRetreatTarget() {
		return retreatTarget;
	}
}
