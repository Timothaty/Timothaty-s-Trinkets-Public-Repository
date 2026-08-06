package net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class DuelistGauntletDurability {
	private static final EntityCapability<IItemHandler, Void> CURIOS_INVENTORY =
			EntityCapability.createVoid(ResourceLocation.fromNamespaceAndPath("curios", "item_handler"), IItemHandler.class);

	private DuelistGauntletDurability() {
	}

	public static boolean isBoss(LivingEntity entity) {
		return entity != null && entity.getType().is(DuelistGuardTags.BOSSES);
	}

	public static void damageForCenterParry(Player player, LivingEntity attacker) {
		damage(player, DuelistGuardData.CENTER_PARRY_GAUNTLET_DURABILITY_COST, attacker);
	}

	public static void damageForSideDeflect(Player player, LivingEntity attacker) {
		damage(player, DuelistGuardData.SIDE_DEFLECT_GAUNTLET_DURABILITY_COST, attacker);
	}

	private static void damage(Player player, int baseCost, LivingEntity attacker) {
		if (player == null || baseCost <= 0)
			return;

		IItemHandler curios = player.getCapability(CURIOS_INVENTORY);
		if (curios == null)
			return;

		for (int slot = 0; slot < curios.getSlots(); slot++) {
			ItemStack gauntlet = curios.getStackInSlot(slot);
			if (!isDuelistsGauntlet(gauntlet))
				continue;
			if (!gauntlet.isDamageableItem())
				return;

			int multiplier = isBoss(attacker) ? DuelistGuardData.BOSS_GAUNTLET_DURABILITY_MULTIPLIER : 1;
			int damage = Math.min(gauntlet.getMaxDamage(), gauntlet.getDamageValue() + baseCost * multiplier);
			gauntlet.setDamageValue(damage);
			if (damage >= gauntlet.getMaxDamage()) {
				gauntlet.shrink(1);
				DuelistGuardState.stopGuarding(player);
			}
			if (curios instanceof IItemHandlerModifiable modifiable) {
				modifiable.setStackInSlot(slot, gauntlet);
			}
			return;
		}
	}

	private static boolean isDuelistsGauntlet(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.is(TimothatysTrinketsModItems.DUELISTS_GAUNTLET.get());
	}
}
