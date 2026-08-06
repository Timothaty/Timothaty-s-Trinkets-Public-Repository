package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.cherubims_wisdom;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.ThrownPotion;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class CherubimsWisdomProjectileEvents {
	private CherubimsWisdomProjectileEvents() {
	}

	@SubscribeEvent
	public static void onProjectileJoinLevel(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide()
				|| event.loadedFromDisk()
				|| !(event.getEntity() instanceof Projectile projectile)
				|| !canDealPlayerRangedDamage(projectile)
				|| !(projectile.getOwner() instanceof ServerPlayer player)
				|| !player.hasEffect(TimothatysTrinketsModMobEffects.CHERUBIMS_WISDOM))
			return;

		projectile.getPersistentData().putBoolean(CherubimsWisdomData.EMPOWERED_PROJECTILE_TAG, true);
	}

	private static boolean canDealPlayerRangedDamage(Projectile projectile) {
		return !(projectile instanceof FishingHook)
				&& !(projectile instanceof ThrownPotion)
				&& !(projectile instanceof ThrownExperienceBottle);
	}
}
