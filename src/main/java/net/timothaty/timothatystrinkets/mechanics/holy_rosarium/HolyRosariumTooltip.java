package net.timothaty.timothatystrinkets.mechanics.holy_rosarium;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record HolyRosariumTooltip(List<ItemStack> beads, Optional<Component> combinationName) implements TooltipComponent {
	public HolyRosariumTooltip {
		beads = List.copyOf(beads);
		combinationName = combinationName == null ? Optional.empty() : combinationName;
	}
}
