package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaCrime;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaCrimes;
import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaVillageRules;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BaseFireBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireChargeItem.class)
public abstract class AnathemaFireChargeItemMixin {
	@Inject(method = "useOn", at = @At("RETURN"))
	private void timothatys_trinkets$reportVillageArson(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		InteractionResult result = cir.getReturnValue();
		if (result == null || !result.consumesAction())
			return;
		if (!(context.getPlayer() instanceof ServerPlayer player) || !(context.getLevel() instanceof ServerLevel level))
			return;

		BlockPos villageBlockPos = context.getClickedPos();
		BlockPos firePos = villageBlockPos.relative(context.getClickedFace());
		if (!(level.getBlockState(firePos).getBlock() instanceof BaseFireBlock))
			return;
		if (!AnathemaVillageRules.isVillageHouseBlock(level, villageBlockPos))
			return;

		AnathemaCrimes.reportCrime(level, player, villageBlockPos, AnathemaCrime.ARSON);
	}
}
