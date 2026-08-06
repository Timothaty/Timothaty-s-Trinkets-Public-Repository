package net.timothaty.timothatystrinkets.mechanics.rusty_armlet;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModEntities;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.mechanics.undead_knights_armlet.UndeadKnightsArmletEvents;

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
public final class RustyArmletEvents {
	private RustyArmletEvents() {
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		LivingEntity victim = event.getEntity();
		if (victim == null || victim.level().isClientSide() || victim instanceof ArmorStand)
			return;
		if (victim.getType() != TimothatysTrinketsModEntities.NECROMANCER.get())
			return;

		Player killer = getKiller(event, victim);
		if (killer == null)
			return;

		upgradeEquippedArmlet(killer);
	}

	private static void upgradeEquippedArmlet(Player player) {
		CuriosApi.getCuriosInventory(player).ifPresent(curios -> {
			ItemStack upgraded = replaceRustyArmlet(curios);
			if (upgraded.isEmpty())
				return;

			UndeadKnightsArmletEvents.onCurioEquip(player, upgraded);
		});
	}

	private static ItemStack replaceRustyArmlet(ICuriosItemHandler curios) {
		return curios.findFirstCurio(RustyArmletEvents::isRustyArmlet)
				.map(RustyArmletEvents::replaceSlot)
				.orElse(ItemStack.EMPTY);
	}

	private static ItemStack replaceSlot(SlotResult slotResult) {
		SlotContext context = slotResult.slotContext();
		if (context.cosmetic())
			return ItemStack.EMPTY;

		ItemStack upgraded = new ItemStack(TimothatysTrinketsModItems.UNDEAD_KNIGHTS_ARMLET.get());
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

	private static boolean isRustyArmlet(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.getItem() == TimothatysTrinketsModItems.RUSTY_ARMLET.get();
	}
}
