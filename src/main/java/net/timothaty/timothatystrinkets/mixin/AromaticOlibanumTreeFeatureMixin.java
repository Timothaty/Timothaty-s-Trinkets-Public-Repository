package net.timothaty.timothatystrinkets.mixin;

import com.llamalad7.mixinextras.sugar.Local;

import net.timothaty.timothatystrinkets.mechanics.olibanum.AromaticOlibanumGeneration;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(TreeFeature.class)
public abstract class AromaticOlibanumTreeFeatureMixin {
	@Inject(
			method = "place",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;encapsulatingPositions(Ljava/lang/Iterable;)Ljava/util/Optional;"
			)
	)
	private void timothatys_trinkets$addNaturalOlibanum(FeaturePlaceContext<TreeConfiguration> context,
			CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 1) Set<BlockPos> generatedLogs) {
		AromaticOlibanumGeneration.afterTreePlaced(context, generatedLogs);
	}
}
