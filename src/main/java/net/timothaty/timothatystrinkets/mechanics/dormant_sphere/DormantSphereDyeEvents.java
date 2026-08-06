package net.timothaty.timothatystrinkets.mechanics.dormant_sphere;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.block.entity.DormantSphereBlockEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class DormantSphereDyeEvents {
	private DormantSphereDyeEvents() {
	}

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		Player player = event.getEntity();
		Level level = player.level();
		BlockPos pos = event.getPos();
		if (!level.getBlockState(pos).is(TimothatysTrinketsModBlocks.DORMANT_SPHERE.get())) {
			return;
		}

		ItemStack dyeStack = player.getItemInHand(event.getHand());
		DyeColor dyeColor = DyeColor.getColor(dyeStack);
		if (dyeColor == null) {
			return;
		}

		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
		if (level.isClientSide || !(level.getBlockEntity(pos) instanceof DormantSphereBlockEntity sphere)) {
			return;
		}

		boolean changed = player.isSecondaryUseActive()
				? sphere.setOutlineColor(dyeColor.getTextureDiffuseColor())
				: sphere.setCoreColor(dyeColor.getTextureDiffuseColor());
		if (!changed) {
			return;
		}

		dyeStack.consume(1, player);
		level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
	}
}
