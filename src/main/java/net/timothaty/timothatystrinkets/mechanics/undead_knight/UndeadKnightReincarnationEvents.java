package net.timothaty.timothatystrinkets.mechanics.undead_knight;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.entity.UndeadKnightEntity;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class UndeadKnightReincarnationEvents {
	private static final float REINCARNATION_CHANCE = 0.4F;

	private UndeadKnightReincarnationEvents() {
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
		if (!(event.getEntity() instanceof UndeadKnightEntity knight) || knight.level().isClientSide())
			return;

		if (knight.isReincarnating()) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
		if (!(event.getEntity() instanceof UndeadKnightEntity knight) || knight.level().isClientSide())
			return;

		if (knight.isReincarnating()) {
			event.setNewDamage(0.0F);
			return;
		}
		if (!knight.canReincarnate() || knight.isOnFire() || event.getNewDamage() <= 0.0F || knight.getHealth() > event.getNewDamage())
			return;
		if (knight.getRandom().nextFloat() >= REINCARNATION_CHANCE)
			return;

		event.setNewDamage(0.0F);
		knight.startReincarnation(knight.getRandom().nextInt(3));
	}

	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Pre event) {
		if (!(event.getEntity() instanceof UndeadKnightEntity knight) || knight.level().isClientSide())
			return;

		if (knight.isReincarnating()) {
			knight.tickReincarnation();
		}
	}
}
