package net.timothaty.timothatystrinkets.entity.ai;

import net.minecraft.world.entity.LivingEntity;

public record StompImpactResult(
	boolean hitAnyTarget,
	boolean primaryTargetHit,
	LivingEntity primaryTarget
) {
	public static final StompImpactResult MISS = new StompImpactResult(false, false, null);
}
