package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Bee.class)
public abstract class GoldenHoneyCombBeeMixin {

	private static final float GOLDEN_HONEY_COMB_STING_BLOCK_CHANCE = 0.6F;

	@Inject(
			method = "doHurtTarget(Lnet/minecraft/world/entity/Entity;)Z",
			at = @At("HEAD"),
			cancellable = true,
			require = 0
	)
	private void timothatys_trinkets$goldenHoneyCombCanBlockBeeSting(
			Entity entity,
			CallbackInfoReturnable<Boolean> cir
	) {
		if (!(entity instanceof Player player)) {
			return;
		}

		if (!player.getOffhandItem().is(TimothatysTrinketsModItems.GOLDEN_HONEY_COMB.get())) {
			return;
		}

		Bee bee = (Bee) (Object) this;

		if (bee.getRandom().nextFloat() >= GOLDEN_HONEY_COMB_STING_BLOCK_CHANCE) {
			return;
		}

		bee.stopBeingAngry();
		bee.setTarget(null);
		bee.playSound(SoundEvents.HONEY_BLOCK_PLACE, 0.8F, 1.25F);

		cir.setReturnValue(false);
	}
}
