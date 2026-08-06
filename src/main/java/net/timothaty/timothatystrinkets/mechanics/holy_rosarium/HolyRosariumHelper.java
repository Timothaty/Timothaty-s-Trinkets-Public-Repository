package net.timothaty.timothatystrinkets.mechanics.holy_rosarium;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public final class HolyRosariumHelper {
	public static final TagKey<Item> UNHOLY_RELICS_TAG = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "unholy_relics"));

	private HolyRosariumHelper() {
	}

	public static ItemStack findActiveRosarium(Player player) {
		Optional<SlotResult> result = findActiveRosariumResult(player);
		return result.isPresent() ? result.get().stack() : ItemStack.EMPTY;
	}

	public static Optional<SlotResult> findActiveRosariumResult(Player player) {
		HolyRosariumState state = HolyRosariumState.get(player);
		if (state == null || !state.bonusesActive())
			return Optional.empty();

		SlotResult result = resolveCachedRosarium(player, state);
		if (result != null)
			return Optional.of(result);

		HolyRosariumState.markDirty(player);
		HolyRosariumState.refreshNow(player);
		state = HolyRosariumState.get(player);
		if (state == null || !state.bonusesActive())
			return Optional.empty();
		return Optional.ofNullable(resolveCachedRosarium(player, state));
	}

	public static boolean isActiveRosarium(Player player, SlotContext slotContext, ItemStack stack) {
		if (slotContext == null || stack == null || stack.isEmpty())
			return false;

		HolyRosariumState state = HolyRosariumState.get(player);
		return state != null
				&& state.bonusesActive()
				&& state.matches(slotContext)
				&& stack.is(TimothatysTrinketsModItems.HOLY_ROSARIUM.get());
	}

	public static Set<HolyRosariumBead> getActiveBeads(Player player) {
		HolyRosariumState state = HolyRosariumState.get(player);
		if (state == null || !state.bonusesActive() || state.beadMask() == 0)
			return Collections.emptySet();

		EnumSet<HolyRosariumBead> beads = EnumSet.noneOf(HolyRosariumBead.class);
		for (HolyRosariumBead bead : HolyRosariumBead.values()) {
			if ((state.beadMask() & bead.bit()) != 0)
				beads.add(bead);
		}
		return beads;
	}

	public static boolean hasActiveCombination(Player player, HolyRosariumBead first, HolyRosariumBead second) {
		HolyRosariumState state = HolyRosariumState.get(player);
		return state != null && state.hasCombination(first, second);
	}

	public static boolean suppressesUnholyRelics(LivingEntity entity) {
		if (!(entity instanceof Player player))
			return false;
		HolyRosariumState state = HolyRosariumState.get(player);
		return state != null && state.suppressesUnholyRelics();
	}

	public static boolean isUnholyRelicSuppressed(LivingEntity entity, ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.is(UNHOLY_RELICS_TAG) && suppressesUnholyRelics(entity);
	}

	private static SlotResult resolveCachedRosarium(Player player, HolyRosariumState state) {
		ICuriosItemHandler handler = CuriosApi.getCuriosInventory(player).orElse(null);
		if (handler == null)
			return null;

		ICurioStacksHandler stacksHandler = handler.getCurios().get(state.slotIdentifier());
		if (stacksHandler == null)
			return null;
		IDynamicStackHandler stacks = state.cosmetic() ? stacksHandler.getCosmeticStacks() : stacksHandler.getStacks();
		if (state.slotIndex() < 0 || state.slotIndex() >= stacks.getSlots())
			return null;
		NonNullList<Boolean> activeStates = stacksHandler.getActiveStates();
		if (!state.cosmetic() && state.slotIndex() < activeStates.size() && !activeStates.get(state.slotIndex()))
			return null;
		ItemStack stack = stacks.getStackInSlot(state.slotIndex());
		if (!stack.is(TimothatysTrinketsModItems.HOLY_ROSARIUM.get()))
			return null;

		NonNullList<Boolean> renders = stacksHandler.getRenders();
		boolean visible = state.slotIndex() < renders.size() && renders.get(state.slotIndex());
		SlotContext context = new SlotContext(
				state.slotIdentifier(),
				player,
				state.slotIndex(),
				state.cosmetic(),
				visible
		);
		return new SlotResult(context, stack);
	}
}
