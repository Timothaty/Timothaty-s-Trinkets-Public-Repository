package net.timothaty.timothatystrinkets.mechanics.rusty_gauntlet;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModEntities;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.mechanics.champions_gauntlet.ChampionsGauntletEvents;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class RustyGauntletEvents {
	private RustyGauntletEvents() {
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		LivingEntity victim = event.getEntity();
		if (victim == null || victim.level().isClientSide() || victim instanceof ArmorStand)
			return;
		if (victim.getType() != TimothatysTrinketsModEntities.UNDEAD_KNIGHT.get())
			return;

		Player killer = getKiller(event, victim);
		if (killer == null)
			return;

		upgradeEquippedGauntlet(killer);
	}

	private static void upgradeEquippedGauntlet(Player player) {
		CuriosApi.getCuriosInventory(player).ifPresent(curios -> {
			ItemStack upgraded = replaceRustyGauntlet(curios);
			if (upgraded.isEmpty())
				return;

			ChampionsGauntletEvents.onCurioEquip(player, upgraded);
		});
	}

	private static ItemStack replaceRustyGauntlet(ICuriosItemHandler curios) {
		return curios.findFirstCurio(RustyGauntletEvents::isRustyGauntlet)
				.map(RustyGauntletEvents::replaceSlot)
				.orElse(ItemStack.EMPTY);
	}

	private static ItemStack replaceSlot(SlotResult slotResult) {
		SlotContext context = slotResult.slotContext();
		if (context.cosmetic())
			return ItemStack.EMPTY;

		ItemStack upgraded = new ItemStack(TimothatysTrinketsModItems.CHAMPIONS_GAUNTLET.get());
		CuriosApi.getCuriosInventory(context.entity()).ifPresent(curios -> curios.setEquippedCurio(context.identifier(), context.index(), upgraded));
		return upgraded;
	}

	private static Player getKiller(LivingDeathEvent event, LivingEntity victim) {
		Entity attackerEntity = event.getSource().getEntity();
		if (attackerEntity instanceof Player attacker)
			return attacker;

		LivingEntity killCredit = victim.getKillCredit();
		return killCredit instanceof Player player ? player : null;
	}

	private static boolean isRustyGauntlet(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.getItem() == TimothatysTrinketsModItems.RUSTY_GAUNTLET.get();
	}
}
