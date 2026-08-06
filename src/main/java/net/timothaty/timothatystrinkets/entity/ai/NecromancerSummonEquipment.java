package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

public final class NecromancerSummonEquipment {
	private static final ResourceLocation ARMOURED_SUMMONS_LOOT_TABLE =
		ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "gameplay/necromancer_armoured_summons");
	private static final ResourceKey<LootTable> ARMOURED_SUMMONS_LOOT_TABLE_KEY =
		ResourceKey.create(Registries.LOOT_TABLE, ARMOURED_SUMMONS_LOOT_TABLE);
	private static final float ARMOR_DROP_CHANCE = 0.0F;

	private NecromancerSummonEquipment() {
	}

	public static void apply(ServerLevel serverLevel, Mob summoned) {
		LootTable lootTable = serverLevel.getServer().reloadableRegistries().getLootTable(ARMOURED_SUMMONS_LOOT_TABLE_KEY);
		if (lootTable == LootTable.EMPTY) {
			return;
		}

		LootParams lootParams = new LootParams.Builder(serverLevel)
			.withParameter(LootContextParams.THIS_ENTITY, summoned)
			.withParameter(LootContextParams.ORIGIN, summoned.position())
			.create(LootContextParamSets.GIFT);

		List<ItemStack> equipment = lootTable.getRandomItems(lootParams);
		for (ItemStack stack : equipment) {
			equipArmor(summoned, stack);
		}
	}

	private static void equipArmor(Mob summoned, ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return;
		}

		EquipmentSlot slot = summoned.getEquipmentSlotForItem(stack);
		if (!isArmorSlot(slot)) {
			return;
		}

		summoned.setItemSlot(slot, stack.copy());
		summoned.setDropChance(slot, ARMOR_DROP_CHANCE);
	}

	private static boolean isArmorSlot(EquipmentSlot slot) {
		return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
	}
}
