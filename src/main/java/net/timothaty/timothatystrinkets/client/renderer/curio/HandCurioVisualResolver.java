package net.timothaty.timothatystrinkets.client.renderer.curio;

import net.timothaty.timothatystrinkets.util.CuriosBraceletSlotHelper;
import net.timothaty.timothatystrinkets.util.CuriosHandsSlotHelper;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

import net.neoforged.fml.ModList;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class HandCurioVisualResolver {
	private static final List<String> RELEVANT_SLOT_IDENTIFIERS = List.of(
			CuriosHandsSlotHelper.HANDS_SLOT_IDENTIFIER,
			CuriosBraceletSlotHelper.BRACELET_SLOT_IDENTIFIER
	);

	private HandCurioVisualResolver() {
	}

	public static ResolvedHandCurioVisuals resolve(
			AbstractClientPlayer player,
			HumanoidArm arm,
			HandCurioRenderContext context
	) {
		if (player == null || arm == null || context == null || player.isSpectator()
				|| !ModList.get().isLoaded("curios")) {
			return ResolvedHandCurioVisuals.EMPTY;
		}

		ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(player).orElse(null);
		if (inventory == null)
			return ResolvedHandCurioVisuals.EMPTY;

		List<ResolvedHandCurioVisuals.ResolvedVisual> primaries = new ArrayList<>();
		List<ResolvedHandCurioVisuals.ResolvedVisual> accessories = new ArrayList<>();
		for (String identifier : RELEVANT_SLOT_IDENTIFIERS) {
			inventory.getStacksHandler(identifier).ifPresent(handler ->
					collectVisuals(player, arm, identifier, handler, primaries, accessories));
		}

		Comparator<ResolvedHandCurioVisuals.ResolvedVisual> comparator = comparator();
		primaries.sort(comparator);
		accessories.sort(comparator);

		Map<HandCurioVisualDefinition, ResolvedHandCurioVisuals.ResolvedVisual> distinctAccessories =
				new LinkedHashMap<>();
		for (ResolvedHandCurioVisuals.ResolvedVisual accessory : accessories)
			distinctAccessories.putIfAbsent(accessory.definition(), accessory);

		Optional<ResolvedHandCurioVisuals.ResolvedVisual> primary = primaries.stream().findFirst();
		if (primary.isEmpty() && distinctAccessories.isEmpty())
			return ResolvedHandCurioVisuals.EMPTY;
		return new ResolvedHandCurioVisuals(primary, List.copyOf(distinctAccessories.values()));
	}

	private static void collectVisuals(
			AbstractClientPlayer player,
			HumanoidArm arm,
			String identifier,
			ICurioStacksHandler handler,
			List<ResolvedHandCurioVisuals.ResolvedVisual> primaries,
			List<ResolvedHandCurioVisuals.ResolvedVisual> accessories
	) {
		IDynamicStackHandler equippedStacks = handler.getStacks();
		IDynamicStackHandler cosmeticStacks = handler.getCosmeticStacks();
		int slots = Math.min(handler.getSlots(), equippedStacks.getSlots());
		for (int slot = 0; slot < slots; slot++) {
			if (CuriosHandsSlotHelper.physicalArmForSlot(player, slot) != arm)
				continue;

			RenderedStack rendered = selectRenderedStack(handler, equippedStacks, cosmeticStacks, slot);
			if (rendered.stack().isEmpty())
				continue;

			HandCurioVisualDefinition definition = HandCurioVisualRegistry
					.find(rendered.stack().getItem())
					.filter(candidate -> candidate.slotIdentifier().equals(identifier))
					.orElse(null);
			if (definition == null)
				continue;

			ResolvedHandCurioVisuals.ResolvedVisual visual = new ResolvedHandCurioVisuals.ResolvedVisual(
					definition,
					rendered.stack(),
					identifier,
					slot,
					rendered.cosmetic()
			);
			if (definition.category() == HandCurioVisualCategory.PRIMARY_GAUNTLET)
				primaries.add(visual);
			else if (definition.category() == HandCurioVisualCategory.ARM_ACCESSORY)
				accessories.add(visual);
		}
	}

	private static RenderedStack selectRenderedStack(
			ICurioStacksHandler handler,
			IDynamicStackHandler equippedStacks,
			IDynamicStackHandler cosmeticStacks,
			int slot
	) {
		ItemStack cosmeticStack = slot < cosmeticStacks.getSlots()
				? cosmeticStacks.getStackInSlot(slot)
				: ItemStack.EMPTY;
		if (!cosmeticStack.isEmpty())
			return new RenderedStack(cosmeticStack, true);

		var renders = handler.getRenders();
		boolean renderEquipped = slot < renders.size() && Boolean.TRUE.equals(renders.get(slot));
		return renderEquipped
				? new RenderedStack(equippedStacks.getStackInSlot(slot), false)
				: RenderedStack.EMPTY;
	}

	private static Comparator<ResolvedHandCurioVisuals.ResolvedVisual> comparator() {
		return Comparator
				.<ResolvedHandCurioVisuals.ResolvedVisual>comparingInt(
						visual -> visual.definition().priority())
				.reversed()
				.thenComparing(visual -> BuiltInRegistries.ITEM
						.getKey(visual.definition().item()).toString())
				.thenComparing(ResolvedHandCurioVisuals.ResolvedVisual::slotIdentifier)
				.thenComparingInt(ResolvedHandCurioVisuals.ResolvedVisual::slotIndex);
	}

	private record RenderedStack(ItemStack stack, boolean cosmetic) {
		private static final RenderedStack EMPTY = new RenderedStack(ItemStack.EMPTY, false);
	}
}
