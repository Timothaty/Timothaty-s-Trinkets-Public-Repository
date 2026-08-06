package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.confession;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumBead;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumHelper;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.AABB;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class ConfessionTradingEvents {
	private static final Map<MerchantOffer, AppliedDiscount> APPLIED_DISCOUNTS =
			Collections.synchronizedMap(new WeakHashMap<>());

	private ConfessionTradingEvents() {
	}

	@SubscribeEvent
	public static void onContainerOpen(PlayerContainerEvent.Open event) {
		Player player = event.getEntity();
		if (player.level().isClientSide() || !(event.getContainer() instanceof MerchantMenu menu))
			return;

		restoreDiscounts(menu);
		if (!HolyRosariumHelper.hasActiveCombination(
				player,
				HolyRosariumBead.HUMILITY,
				HolyRosariumBead.SACRAMENT
			) || findTradingCleric(player) == null)
			return;

		for (MerchantOffer offer : menu.getOffers())
			applyDiscount(offer);
	}

	@SubscribeEvent
	public static void onContainerClose(PlayerContainerEvent.Close event) {
		if (event.getContainer() instanceof MerchantMenu menu)
			restoreDiscounts(menu);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		synchronized (APPLIED_DISCOUNTS) {
			APPLIED_DISCOUNTS.forEach((offer, applied) -> {
				if (offer.getSpecialPriceDiff() == applied.discountedSpecialPriceDiff())
					offer.setSpecialPriceDiff(applied.originalSpecialPriceDiff());
			});
			APPLIED_DISCOUNTS.clear();
		}
	}

	private static Villager findTradingCleric(Player player) {
		AABB area = player.getBoundingBox().inflate(ConfessionData.TRADING_CLERIC_SEARCH_RADIUS);
		return player.level().getEntitiesOfClass(Villager.class, area, villager ->
				villager.getTradingPlayer() == player
						&& villager.getVillagerData().getProfession() == VillagerProfession.CLERIC
		).stream().findFirst().orElse(null);
	}

	private static void applyDiscount(MerchantOffer offer) {
		int currentCost = offer.getCostA().getCount();
		if (currentCost <= 1)
			return;

		int discount = Math.max(1, (int) Math.floor(currentCost * ConfessionData.CLERIC_TRADE_DISCOUNT));
		discount = Math.min(discount, currentCost - 1);
		int originalSpecialPriceDiff = offer.getSpecialPriceDiff();
		offer.addToSpecialPriceDiff(-discount);
		APPLIED_DISCOUNTS.put(offer, new AppliedDiscount(originalSpecialPriceDiff, offer.getSpecialPriceDiff()));
	}

	private static void restoreDiscounts(MerchantMenu menu) {
		for (MerchantOffer offer : menu.getOffers()) {
			AppliedDiscount applied = APPLIED_DISCOUNTS.remove(offer);
			if (applied != null && offer.getSpecialPriceDiff() == applied.discountedSpecialPriceDiff())
				offer.setSpecialPriceDiff(applied.originalSpecialPriceDiff());
		}
	}

	private record AppliedDiscount(int originalSpecialPriceDiff, int discountedSpecialPriceDiff) {
	}
}
