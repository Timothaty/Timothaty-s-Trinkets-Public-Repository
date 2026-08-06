package net.timothaty.timothatystrinkets.mechanics.cleric_quests;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.pillagers_coin.PillagersCoinHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ClericQuestRewardService {
	private ClericQuestRewardService() {
	}

	public static boolean grantHumility(ServerLevel level, Villager cleric, ServerPlayer player) {
		List<ItemStack> bonus = PillagersCoinHelper.rollClericGiftLoot(level, cleric, 1, 1);
		if (bonus.size() != 1 || bonus.get(0).isEmpty())
			return false;
		if (VillagerGiftThrower.hasSequence(cleric.getUUID()))
			return false;
		List<ItemStack> guaranteed = List.of(
			new ItemStack(TimothatysTrinketsModItems.BEAD_OF_HUMILITY.get()),
			new ItemStack(TimothatysTrinketsModItems.HOLY_ROSARIUM.get())
		);
		return VillagerGiftThrower.throwStacks(level, cleric, player, guaranteed, TimothatysTrinketsModSounds.RARE_ITEM_DROP_VILLAGER.get())
			&& VillagerGiftThrower.beginBonusSequence(level, cleric, player, bonus);
	}

	public static boolean grantSacrament(ServerLevel level, Villager cleric, ServerPlayer player) {
		List<ItemStack> bonus = PillagersCoinHelper.rollClericGiftLoot(level, cleric, 2, 3);
		if (bonus.size() < 2 || bonus.size() > 3)
			return false;
		if (!VillagerGiftThrower.throwStack(level, cleric, player, new ItemStack(TimothatysTrinketsModItems.BEAD_OF_THE_SACRAMENT.get()), TimothatysTrinketsModSounds.RARE_ITEM_DROP_VILLAGER.get()))
			return false;
		VillagerGiftThrower.beginBonusSequence(level, cleric, player, bonus);
		return true;
	}
}
