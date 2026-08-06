package net.timothaty.timothatystrinkets.mechanics.damnation_altar;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public final class DamnationAltarLootService {
	private static final ResourceKey<LootTable> ALTAR_REWARDS = ResourceKey.create(
			Registries.LOOT_TABLE,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "gameplay/damnation_altar")
	);

	private DamnationAltarLootService() {
	}

	public static ItemStack rollOffer(ServerLevel level, ServerPlayer player, BlockPos altarPos, ItemStack ritualDagger) {
		LootTable table = level.getServer().reloadableRegistries().getLootTable(ALTAR_REWARDS);
		if (table == LootTable.EMPTY)
			return ItemStack.EMPTY;

		LootParams params = new LootParams.Builder(level)
				.withParameter(LootContextParams.THIS_ENTITY, player)
				.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(altarPos))
				.withParameter(LootContextParams.BLOCK_STATE, level.getBlockState(altarPos))
				.withParameter(LootContextParams.TOOL, ritualDagger.copy())
				.create(LootContextParamSets.BLOCK);

		for (ItemStack stack : table.getRandomItems(params)) {
			if (!stack.isEmpty())
				return stack;
		}
		return ItemStack.EMPTY;
	}
}
