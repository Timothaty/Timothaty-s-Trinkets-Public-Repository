package net.timothaty.timothatystrinkets.mechanics.champions_gauntlet;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.mechanics.armlet_gauntlet_synergy.ArmletGauntletSynergyState;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class ChampionsGauntletClericDropHandler {
	private static final float DROP_CHANCE = 0.90F;

	private ChampionsGauntletClericDropHandler() {
	}

	@SubscribeEvent
	public static void onLivingDrops(LivingDropsEvent event) {
		if (!(event.getEntity() instanceof Villager villager)
				|| !(villager.level() instanceof ServerLevel level)
				|| villager.isBaby()
				|| villager.getVillagerData().getProfession() != VillagerProfession.CLERIC)
			return;

		Entity attacker = event.getSource().getEntity();
		if (!(attacker instanceof Player killer))
			return;

		ArmletGauntletSynergyState.refreshFromCurios(killer);
		if (!ArmletGauntletSynergyState.isGauntletActive(killer) || level.getRandom().nextFloat() >= DROP_CHANCE)
			return;

		int count = 2 + level.getRandom().nextInt(3);
		event.getDrops().add(new ItemEntity(
				level,
				villager.getX(),
				villager.getY() + 0.25D,
				villager.getZ(),
				new ItemStack(TimothatysTrinketsModItems.CURSED_EMERALD.get(), count)
		));
	}
}
