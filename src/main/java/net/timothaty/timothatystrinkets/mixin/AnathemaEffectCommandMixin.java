package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaHelper;

import net.minecraft.core.Holder;
import net.minecraft.server.commands.EffectCommands;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EffectCommands.class)
public abstract class AnathemaEffectCommandMixin {
	@Redirect(
		method = "clearEffects",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;removeAllEffects()Z")
	)
	private static boolean timothatys_trinkets$allowCommandClearAll(LivingEntity entity) {
		return AnathemaHelper.withAllowedRemoval(entity, entity::removeAllEffects);
	}

	@Redirect(
		method = "clearEffect",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;removeEffect(Lnet/minecraft/core/Holder;)Z")
	)
	private static boolean timothatys_trinkets$allowCommandClearOne(LivingEntity entity, Holder<MobEffect> effect) {
		return AnathemaHelper.withAllowedRemoval(entity, () -> entity.removeEffect(effect));
	}
}
