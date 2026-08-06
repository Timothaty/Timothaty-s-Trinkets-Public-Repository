package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.entity.UndeadKnightEntity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.VillagerHostilesSensor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerHostilesSensor.class)
public abstract class UndeadKnightVillagerHostilesSensorMixin {
	@Unique
	private static final float TIMOTHATYS_TRINKETS$UNDEAD_KNIGHT_THREAT_DISTANCE = 9.0F;

	@Inject(method = "isMatchingEntity", at = @At("RETURN"), cancellable = true, require = 0)
	private void timothatys_trinkets$recognizeUndeadKnight(
		LivingEntity villager,
		LivingEntity target,
		CallbackInfoReturnable<Boolean> cir
	) {
		if (Boolean.TRUE.equals(cir.getReturnValue()) || !(target instanceof UndeadKnightEntity) || !target.isAlive()) {
			return;
		}

		double maxDistanceSqr = TIMOTHATYS_TRINKETS$UNDEAD_KNIGHT_THREAT_DISTANCE * TIMOTHATYS_TRINKETS$UNDEAD_KNIGHT_THREAT_DISTANCE;
		if (target.distanceToSqr(villager) <= maxDistanceSqr) {
			cir.setReturnValue(true);
		}
	}
}
