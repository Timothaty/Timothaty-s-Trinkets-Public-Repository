package net.timothaty.timothatystrinkets.client.renderer.curio;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record ResolvedHandCurioVisuals(
		Optional<ResolvedVisual> primaryGauntlet,
		List<ResolvedVisual> accessories
) {
	public static final ResolvedHandCurioVisuals EMPTY =
			new ResolvedHandCurioVisuals(Optional.empty(), List.of());

	public ResolvedHandCurioVisuals {
		primaryGauntlet = primaryGauntlet == null ? Optional.empty() : primaryGauntlet;
		accessories = accessories == null ? List.of() : List.copyOf(accessories);
	}

	public boolean isEmpty() {
		return this.primaryGauntlet.isEmpty() && this.accessories.isEmpty();
	}

	public record ResolvedVisual(
			HandCurioVisualDefinition definition,
			ItemStack renderedStack,
			String slotIdentifier,
			int slotIndex,
			boolean cosmetic
	) {
	}
}
