package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.blight.BlightSunShelterHelper;
import net.timothaty.timothatystrinkets.mechanics.blight.BlightConfig;
import net.timothaty.timothatystrinkets.mechanics.blight.BlightZoneHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FleeSunGoal.class)
public abstract class BlightFleeSunGoalMixin {
	@Shadow
	@Final
	protected PathfinderMob mob;

	@Unique
	private BlockPos timothatys_trinkets$cachedBlightStandPos;
	@Unique
	private int timothatys_trinkets$cachedBlightStandTick = -BlightConfig.SUN_SHELTER_CACHE_TICKS;

	@Inject(method = "canUse", at = @At("HEAD"), cancellable = true, require = 0)
	private void timothatys_trinkets$skipSunFleeUnderUnholyAura(CallbackInfoReturnable<Boolean> cir) {
		if (timothatys_trinkets$isUndeadProtectedByAura()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true, require = 0)
	private void timothatys_trinkets$stopSunFleeUnderUnholyAura(CallbackInfoReturnable<Boolean> cir) {
		if (timothatys_trinkets$isUndeadProtectedByAura()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "getHidePos", at = @At("RETURN"), cancellable = true, require = 0)
	private void timothatys_trinkets$preferNearbyBlightShelter(CallbackInfoReturnable<Vec3> cir) {
		if (!(this.mob.level() instanceof ServerLevel level) || !this.mob.isOnFire() || !BlightSunShelterHelper.shouldUseBlightSunShelter(this.mob)) {
			return;
		}

		BlockPos blightStandPos = timothatys_trinkets$getCachedBlightStandPos(level);
		if (blightStandPos == null) {
			return;
		}

		Vec3 blightHidePos = Vec3.atBottomCenterOf(blightStandPos);
		Vec3 vanillaHidePos = cir.getReturnValue();
		if (vanillaHidePos == null || blightHidePos.distanceToSqr(this.mob.position()) < vanillaHidePos.distanceToSqr(this.mob.position())) {
			cir.setReturnValue(blightHidePos);
		}
	}

	@Unique
	private BlockPos timothatys_trinkets$getCachedBlightStandPos(ServerLevel level) {
		int cacheAge = this.mob.tickCount - this.timothatys_trinkets$cachedBlightStandTick;
		if (cacheAge >= 0 && cacheAge < BlightConfig.SUN_SHELTER_CACHE_TICKS) {
			return this.timothatys_trinkets$cachedBlightStandPos;
		}

		this.timothatys_trinkets$cachedBlightStandPos = BlightZoneHelper.findNearestReachableBlightStandPos(level, this.mob, BlightConfig.SUN_SHELTER_HORIZONTAL_SEARCH_RANGE,
				BlightConfig.SUN_SHELTER_VERTICAL_SEARCH_RANGE, BlightConfig.SUN_SHELTER_MAX_PATH_CHECKS);
		this.timothatys_trinkets$cachedBlightStandTick = this.mob.tickCount;
		return this.timothatys_trinkets$cachedBlightStandPos;
	}

	@Unique
	private boolean timothatys_trinkets$isUndeadProtectedByAura() {
		return this.mob.getType().is(EntityTypeTags.UNDEAD) && this.mob.hasEffect(TimothatysTrinketsModMobEffects.UNHOLY_AURA);
	}
}
