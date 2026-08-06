package net.timothaty.timothatystrinkets.client.duelist;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.handler.ConcussiveStrikeCameraShakeHandler;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet.DuelistGuardDirection;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraft.client.player.LocalPlayer;

public final class DuelistGuardClientFeedback {
	private static final TagKey<Item> LEATHER_CHESTPLATES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "leather_chestplates"));
	private static final TagKey<Item> METALLIC_CHESTPLATES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "metallic_chestplates"));

	private DuelistGuardClientFeedback() {
	}

	public static void playDirectionShift(LocalPlayer player, int shiftSign, DuelistGuardDirection previousDirection, DuelistGuardDirection newDirection) {
		if (player == null || previousDirection == newDirection)
			return;

		player.playSound(shouldUseMetallicParrySound(player) ? TimothatysTrinketsModSounds.METALLIC_PARRY_DIRECTION.get() : TimothatysTrinketsModSounds.LEATHER_PARRY_DIRECTION.get(), 0.34F, 0.94F + player.getRandom().nextFloat() * 0.12F);
		ConcussiveStrikeCameraShakeHandler.startDuelistDirectionShake(shiftSign);
	}

	private static boolean shouldUseMetallicParrySound(LocalPlayer player) {
		ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
		if (chestplate.isEmpty() || chestplate.is(LEATHER_CHESTPLATES))
			return false;
		return !chestplate.isEmpty() && chestplate.is(METALLIC_CHESTPLATES);
	}
}
