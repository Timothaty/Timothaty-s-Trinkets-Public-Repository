package net.timothaty.timothatystrinkets.client;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.block.entity.DormantSphereBlockEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class DormantSphereItemColors {
	private static final int CORE_TINT_INDEX = 0;
	private static final int OUTLINE_TINT_INDEX = 1;

	private DormantSphereItemColors() {
	}

	@SubscribeEvent
	public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		event.register(DormantSphereItemColors::getColor, TimothatysTrinketsModItems.DORMANT_SPHERE.get());
	}

	private static int getColor(ItemStack stack, int tintIndex) {
		return switch (tintIndex) {
			case CORE_TINT_INDEX -> DormantSphereBlockEntity.getCoreColor(stack);
			case OUTLINE_TINT_INDEX -> DormantSphereBlockEntity.getOutlineColor(stack);
			default -> -1;
		};
	}
}
