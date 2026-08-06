package net.timothaty.timothatystrinkets.util;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModEntities;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.sounds.SoundSource;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public class ClericMasterPurgeTradeHandler {
	private static final float PURGE_CHANCE = 0.15F;
	private static final int MASTER_LEVEL = 5;
	private static final int PURGE_DURATION_TICKS = 20 * 60 * 10;

	@SubscribeEvent
	public static void onPlayerTradeWithVillager(TradeWithVillagerEvent event) {
		if (event == null || event.getEntity() == null || event.getEntity().level().isClientSide()) {
			return;
		}

		if (!(event.getAbstractVillager() instanceof Villager villager)) {
			return;
		}

		if (villager.getVillagerData().getProfession() != VillagerProfession.CLERIC) {
			return;
		}
		if (villager.getVillagerData().getLevel() != MASTER_LEVEL) {
			return;
		}

		if (villager.getRandom().nextFloat() < PURGE_CHANCE) {
			if (!event.getEntity().hasEffect(TimothatysTrinketsModMobEffects.PURGE)) {
				var vfx = TimothatysTrinketsModEntities.VFX_INDULGENCY_BLESSING.get().create(event.getEntity().level());
				if (vfx != null) {
					vfx.moveTo(event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity().getYRot(), 0.0F);
					event.getEntity().level().addFreshEntity(vfx);
				}

				event.getEntity().addEffect(new MobEffectInstance(TimothatysTrinketsModMobEffects.PURGE, PURGE_DURATION_TICKS, 0, false, true, true));
				event.getEntity().level().playSound(
						null,
						event.getEntity().getX(),
						event.getEntity().getY(),
						event.getEntity().getZ(),
						TimothatysTrinketsModSounds.PURGE.get(),
						SoundSource.PLAYERS,
						1.0F,
						1.0F
				);
			}
		}
	}
}
