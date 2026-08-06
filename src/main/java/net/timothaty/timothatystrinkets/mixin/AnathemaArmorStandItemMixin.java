package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaVillageClaims;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStandItem.class)
public abstract class AnathemaArmorStandItemMixin {
	@Inject(method = "useOn", at = @At("RETURN"))
	private void timothatys_trinkets$markPlayerPlacedArmorStand(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		InteractionResult result = cir.getReturnValue();
		if (result == null || !result.consumesAction() || context.getPlayer() == null)
			return;
		if (!(context.getLevel() instanceof ServerLevel level))
			return;

		var placedPos = new BlockPlaceContext(context).getClickedPos();
		level.getEntitiesOfClass(
			ArmorStand.class,
			new AABB(placedPos).inflate(0.75D),
			stand -> stand.tickCount <= 1 && !stand.getPersistentData().getBoolean(AnathemaVillageClaims.PLAYER_PLACED_ARMOR_STAND_KEY)
		).stream().min(java.util.Comparator.comparingDouble(stand -> stand.distanceToSqr(context.getPlayer())))
			.ifPresent(stand -> stand.getPersistentData().putBoolean(AnathemaVillageClaims.PLAYER_PLACED_ARMOR_STAND_KEY, true));
	}
}
