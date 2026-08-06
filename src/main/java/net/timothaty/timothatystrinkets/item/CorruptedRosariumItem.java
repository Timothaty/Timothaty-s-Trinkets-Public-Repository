package net.timothaty.timothatystrinkets.item;

import net.timothaty.timothatystrinkets.api.rosarium.RosariumCombination;
import net.timothaty.timothatystrinkets.api.rosarium.RosariumCombinationApi;
import net.timothaty.timothatystrinkets.api.rosarium.RosariumTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumData;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumStateEvents;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumTooltip;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;

import java.util.List;
import java.util.Optional;

public class CorruptedRosariumItem extends Item {
	public CorruptedRosariumItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public boolean overrideStackedOnOther(ItemStack rosarium, Slot slot, ClickAction clickAction, Player player) {
		if (rosarium.getCount() != 1 || clickAction != ClickAction.SECONDARY)
			return false;

		ItemStack slotStack = slot.getItem();
		if (slotStack.isEmpty()) {
			ItemStack removedBead = CorruptedRosariumData.removeLast(rosarium);
			if (removedBead.isEmpty())
				return false;

			playRemoveOneSound(player);
			ItemStack leftover = slot.safeInsert(removedBead);
			if (!leftover.isEmpty())
				CorruptedRosariumData.addBeadId(rosarium, BuiltInRegistries.ITEM.getKey(leftover.getItem()));
			CorruptedRosariumStateEvents.onRosariumDataChanged(player);
			return true;
		}

		if (!CorruptedRosariumData.canInsert(rosarium, slotStack))
			return false;

		ItemStack taken = slot.safeTake(1, 1, player);
		if (taken.isEmpty())
			return false;

		CorruptedRosariumData.addBeadId(rosarium, BuiltInRegistries.ITEM.getKey(taken.getItem()));
		CorruptedRosariumStateEvents.onRosariumDataChanged(player);
		playInsertSound(player);
		return true;
	}

	@Override
	public boolean overrideOtherStackedOnMe(
			ItemStack rosarium,
			ItemStack carriedStack,
			Slot slot,
			ClickAction clickAction,
			Player player,
			SlotAccess carriedAccess
	) {
		if (rosarium.getCount() != 1
				|| clickAction != ClickAction.SECONDARY
				|| !slot.allowModification(player))
			return false;

		if (carriedStack.isEmpty()) {
			ItemStack removedBead = CorruptedRosariumData.removeLast(rosarium);
			if (removedBead.isEmpty())
				return false;

			playRemoveOneSound(player);
			carriedAccess.set(removedBead);
			CorruptedRosariumStateEvents.onRosariumDataChanged(player);
			return true;
		}

		if (CorruptedRosariumData.insertOne(rosarium, carriedStack)) {
			CorruptedRosariumStateEvents.onRosariumDataChanged(player);
			playInsertSound(player);
			return true;
		}

		return false;
	}

	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		if (stack.has(DataComponents.HIDE_TOOLTIP) || stack.has(DataComponents.HIDE_ADDITIONAL_TOOLTIP))
			return Optional.empty();

		List<ResourceLocation> beadIds = CorruptedRosariumData.getBeadIds(stack);
		Optional<Component> combinationName = RosariumCombinationApi.find(RosariumTypes.CORRUPTED, beadIds)
				.map(RosariumCombination::displayName);
		return Optional.of(new CorruptedRosariumTooltip(CorruptedRosariumData.getBeadStacks(stack), combinationName));
	}

	@Override
	public void onDestroyed(ItemEntity itemEntity) {
		ItemStack rosarium = itemEntity.getItem();
		List<ItemStack> beads = CorruptedRosariumData.getBeadStacks(rosarium);
		if (beads.isEmpty())
			return;

		CorruptedRosariumData.clear(rosarium);
		ItemUtils.onContainerDestroyed(itemEntity, beads);
	}

	@Override
	public boolean canFitInsideContainerItems() {
		return false;
	}

	private void playInsertSound(Player player) {
		float pitch = 1.0F + player.getRandom().nextFloat() * 0.2F;
		player.playNotifySound(
				TimothatysTrinketsModSounds.BEAD_INSERT.get(),
				SoundSource.PLAYERS,
				0.8F,
				pitch
		);
	}

	private void playRemoveOneSound(Player player) {
		player.playNotifySound(
				SoundEvents.ARMOR_EQUIP_LEATHER.value(),
				SoundSource.PLAYERS,
				0.8F,
				1.0F
		);
	}
}
