package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.mechanics.blight.BlightSunShelterHelper;
import net.timothaty.timothatystrinkets.mechanics.blight.BlightZoneHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GroundPathNavigation.class)
public abstract class BlightPathNavigationMixin extends PathNavigation {
	protected BlightPathNavigationMixin(Mob mob, Level level) {
		super(mob, level);
	}

	@Redirect(method = "trimPath", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;canSeeSky(Lnet/minecraft/core/BlockPos;)Z"), require = 0)
	private boolean timothatys_trinkets$blightShelterCountsAsShade(Level level, BlockPos pos) {
		return level.canSeeSky(pos)
				&& !(BlightSunShelterHelper.shouldUseBlightSunShelter(this.mob) && BlightZoneHelper.isBlightShelterAt(level, pos));
	}
}
