package net.timothaty.timothatystrinkets.item;

import net.timothaty.timothatystrinkets.api.rosarium.RosariumCombination;
import net.timothaty.timothatystrinkets.api.rosarium.RosariumCombinationApi;
import net.timothaty.timothatystrinkets.api.rosarium.RosariumTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumData;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumStateEvents;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumTooltip;

import net.minecraft.core.component.DataComponents;
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

public class HolyRosariumItem extends Item {
	public HolyRosariumItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public boolean overrideStackedOnOther(ItemStack rosarium, Slot slot, ClickAction clickAction, Player player) {
		if (rosarium.getCount() != 1 || clickAction != ClickAction.SECONDARY)
			return false;

		ItemStack slotStack = slot.getItem();
		if (slotStack.isEmpty()) {
			ItemStack removedBead = HolyRosariumData.removeLast(rosarium);
			if (removedBead.isEmpty())
				return false;

			playRemoveOneSound(player);
			ItemStack leftover = slot.safeInsert(removedBead);
			if (!leftover.isEmpty())
				HolyRosariumData.addBeadId(rosarium, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(leftover.getItem()));
			HolyRosariumStateEvents.onRosariumDataChanged(player);
			return true;
		}

		if (!HolyRosariumData.canInsert(rosarium, slotStack))
			return false;

		ItemStack taken = slot.safeTake(1, 1, player);
		if (taken.isEmpty())
			return false;

		HolyRosariumData.addBeadId(rosarium, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(taken.getItem()));
		HolyRosariumStateEvents.onRosariumDataChanged(player);
		playInsertSound(player);
		return true;
	}

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack rosarium, ItemStack carriedStack, Slot slot, ClickAction clickAction, Player player, SlotAccess carriedAccess) {
		if (rosarium.getCount() != 1 || clickAction != ClickAction.SECONDARY || !slot.allowModification(player))
			return false;

		if (carriedStack.isEmpty()) {
			ItemStack removedBead = HolyRosariumData.removeLast(rosarium);
			if (removedBead.isEmpty())
				return false;

			playRemoveOneSound(player);
			carriedAccess.set(removedBead);
			HolyRosariumStateEvents.onRosariumDataChanged(player);
			return true;
		}

		if (HolyRosariumData.insertOne(rosarium, carriedStack)) {
			HolyRosariumStateEvents.onRosariumDataChanged(player);
			playInsertSound(player);
			return true;
		}

		return false;
	}

	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		if (stack.has(DataComponents.HIDE_TOOLTIP) || stack.has(DataComponents.HIDE_ADDITIONAL_TOOLTIP))
			return Optional.empty();

		List<ResourceLocation> beadIds = HolyRosariumData.getBeadIds(stack);
		Optional<Component> combinationName = RosariumCombinationApi.find(RosariumTypes.HOLY, beadIds)
				.map(RosariumCombination::displayName);
		return Optional.of(new HolyRosariumTooltip(HolyRosariumData.getBeadStacks(stack), combinationName));
	}

	@Override
	public void onDestroyed(ItemEntity itemEntity) {
		ItemStack rosarium = itemEntity.getItem();
		List<ItemStack> beads = HolyRosariumData.getBeadStacks(rosarium);
		if (beads.isEmpty())
			return;

		HolyRosariumData.clear(rosarium);
		ItemUtils.onContainerDestroyed(itemEntity, beads);
	}

	@Override
	public boolean canFitInsideContainerItems() {
		return false;
	}

	private void playInsertSound(Player player) {
		float pitch = 1.0F + player.getRandom().nextFloat() * 0.2F;
		player.playNotifySound(TimothatysTrinketsModSounds.BEAD_INSERT.get(), SoundSource.PLAYERS, 0.8F, pitch);
	}

	private void playRemoveOneSound(Player player) {
		player.playNotifySound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.8F, 1.0F);
	}
}
