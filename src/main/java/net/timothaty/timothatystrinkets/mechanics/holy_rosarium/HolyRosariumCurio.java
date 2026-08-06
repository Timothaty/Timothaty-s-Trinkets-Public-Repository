package net.timothaty.timothatystrinkets.mechanics.holy_rosarium;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.registries.DeferredHolder;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

public class HolyRosariumCurio implements ICurio {
	private final ItemStack stack;

	public HolyRosariumCurio(ItemStack stack) {
		this.stack = stack;
	}

	@Override
	public ItemStack getStack() {
		return stack;
	}

	@Override
	public SoundInfo getEquipSound(SlotContext slotContext) {
		SoundEvent sound = DeferredHolder.create(Registries.SOUND_EVENT, ResourceLocation.parse("item.armor.equip_leather")).value();
		return new SoundInfo(sound, 1, 1);
	}

	@Override
	public void onEquip(SlotContext slotContext, ItemStack previousStack) {
		invalidate(slotContext);
	}

	@Override
	public void onUnequip(SlotContext slotContext, ItemStack newStack) {
		invalidate(slotContext);
	}

	private static void invalidate(SlotContext slotContext) {
		if (slotContext != null && slotContext.entity() instanceof Player player) {
			HolyRosariumState.markDirty(player);
			HolyRosariumState.refreshNow(player);
		}
	}
}
