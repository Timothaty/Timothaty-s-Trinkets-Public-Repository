package net.timothaty.timothatystrinkets.block;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;

import net.minecraft.world.level.block.entity.BlockEntityType;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModBlocks;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public class DebtlordHeadBlockEntityTypes {
	@SubscribeEvent
	public static void addBlockEntityValidBlocks(BlockEntityTypeAddBlocksEvent event) {
		event.modify(BlockEntityType.SKULL, TimothatysTrinketsModBlocks.DEBTLORDS_HEAD.get(), TimothatysTrinketsModBlocks.DEBTLORDS_WALL_HEAD.get());
	}
}
