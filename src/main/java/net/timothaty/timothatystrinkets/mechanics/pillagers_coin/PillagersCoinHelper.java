package net.timothaty.timothatystrinkets.mechanics.pillagers_coin;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.mechanics.bloodstained.BloodstainedHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.List;

public final class PillagersCoinHelper {
	private PillagersCoinHelper() {
	}

	public static boolean isDirectPlayerMelee(DamageSource source) {
		if (source == null || !source.is(DamageTypes.PLAYER_ATTACK))
			return false;
		Entity attacker = source.getEntity();
		return attacker instanceof Player && source.getDirectEntity() == attacker;
	}

	public static boolean hasUsableCoin(Player player) {
		return player != null
			&& player.getOffhandItem().is(TimothatysTrinketsModItems.PILLAGERS_COIN.get())
			&& !player.getCooldowns().isOnCooldown(TimothatysTrinketsModItems.PILLAGERS_COIN.get());
	}

	public static double calculateSuccessChance(Player player, boolean witnessed) {
		boolean bloodstained = BloodstainedHelper.hasBloodstained(player);
		boolean threateningWeapon = player.getMainHandItem().is(ItemTags.AXES) || player.getMainHandItem().is(ItemTags.SWORDS);
		double chance = witnessed ? PillagersCoinData.WITNESSED_BASE_CHANCE : PillagersCoinData.UNWITNESSED_BASE_CHANCE;
		if (bloodstained)
			chance += PillagersCoinData.BLOODSTAINED_BONUS;
		if (threateningWeapon)
			chance += bloodstained ? PillagersCoinData.BLOODSTAINED_WEAPON_BONUS : PillagersCoinData.WEAPON_BONUS;
		return Mth.clamp(chance, PillagersCoinData.MIN_SUCCESS_CHANCE, PillagersCoinData.MAX_SUCCESS_CHANCE);
	}

	public static List<ItemStack> rollExtortionLoot(ServerLevel level, Villager villager, int maximumDrops) {
		if (maximumDrops <= 0)
			return List.of();
		ResourceKey<LootTable> table = villager.getVillagerData().getProfession() == VillagerProfession.CLERIC
				? PillagersCoinData.CLERIC_EXTORTION_LOOT_TABLE
				: PillagersCoinData.EXTORTION_LOOT_TABLE;
		return rollDistinctLoot(level, villager, table, 1, Math.min(3, maximumDrops));
	}

	public static List<ItemStack> rollClericGiftLoot(ServerLevel level, Villager villager, int minimumDrops, int maximumDrops) {
		return rollDistinctLoot(level, villager, PillagersCoinData.CLERIC_EXTORTION_LOOT_TABLE, minimumDrops, maximumDrops);
	}

	private static List<ItemStack> rollDistinctLoot(ServerLevel level, Villager villager, ResourceKey<LootTable> tableKey, int minimumDrops, int maximumDrops) {
		int minimum = Math.max(1, minimumDrops);
		int maximum = Math.max(minimum, maximumDrops);
		LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(tableKey);
		LootParams params = new LootParams.Builder(level)
			.withParameter(LootContextParams.THIS_ENTITY, villager)
			.withParameter(LootContextParams.ORIGIN, villager.position())
			.create(LootContextParamSets.GIFT);
		List<ItemStack> merged = List.of();
		for (int attempt = 0; attempt < 8; attempt++) {
			List<ItemStack> candidate = mergeStacks(lootTable.getRandomItems(params));
			if (candidate.size() > merged.size())
				merged = candidate;
			if (merged.size() >= minimum)
				break;
		}
		if (merged.size() < minimum)
			return List.of();

		shuffle(merged, level.getRandom());
		int limit = Math.min(maximum, merged.size());
		int distinctCount = minimum + level.getRandom().nextInt(limit - minimum + 1);
		List<ItemStack> selected = new ArrayList<>(distinctCount);
		for (int i = 0; i < distinctCount; i++)
			selected.add(merged.get(i).copy());
		return selected;
	}

	private static List<ItemStack> mergeStacks(List<ItemStack> generated) {
		List<ItemStack> merged = new ArrayList<>();
		for (ItemStack stack : generated) {
			if (stack.isEmpty())
				continue;
			ItemStack existing = merged.stream()
				.filter(candidate -> ItemStack.isSameItemSameComponents(candidate, stack))
				.findFirst()
				.orElse(null);
			if (existing == null)
				merged.add(stack.copy());
			else
				existing.grow(stack.getCount());
		}
		return merged;
	}

	private static void shuffle(List<ItemStack> stacks, RandomSource random) {
		for (int i = stacks.size() - 1; i > 0; i--) {
			int other = random.nextInt(i + 1);
			ItemStack stack = stacks.get(i);
			stacks.set(i, stacks.get(other));
			stacks.set(other, stack);
		}
	}
}
