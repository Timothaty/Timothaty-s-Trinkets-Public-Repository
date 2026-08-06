package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.angels_shroud;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.network.AngelsShroudEndBurstMessage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class AngelsShroudEndEffects {
	private static final long TRANSITION_SUPPRESSION_TICKS = 5L;
	private static final Set<MobEffectInstance> HANDLED_ENDINGS =
			Collections.newSetFromMap(new WeakHashMap<>());
	private static final Map<UUID, Long> SUPPRESSED_UNTIL = new HashMap<>();

	private AngelsShroudEndEffects() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRemoved(MobEffectEvent.Remove event) {
		if (event.isCanceled()
				|| event.getEffect().value() != TimothatysTrinketsModMobEffects.ANGELS_SHROUD.get()) {
			return;
		}
		handleEnding(event.getEntity(), event.getEffectInstance());
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onExpired(MobEffectEvent.Expired event) {
		MobEffectInstance effect = event.getEffectInstance();
		if (event.isCanceled() || effect == null
				|| effect.getEffect().value() != TimothatysTrinketsModMobEffects.ANGELS_SHROUD.get()) {
			return;
		}
		handleEnding(event.getEntity(), effect);
	}

	@SubscribeEvent
	public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
		if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer player)
			suppressTransition(player);
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			suppressTransition(player);
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			suppressTransition(player);
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (SUPPRESSED_UNTIL.isEmpty())
			return;
		long now = event.getServer().overworld().getGameTime();
		SUPPRESSED_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		HANDLED_ENDINGS.clear();
		SUPPRESSED_UNTIL.clear();
	}

	private static void handleEnding(LivingEntity entity, MobEffectInstance effect) {
		if (!(entity instanceof ServerPlayer player) || effect == null || !HANDLED_ENDINGS.add(effect))
			return;
		if (!(player.level() instanceof ServerLevel level) || !player.isAlive()
				|| player.isDeadOrDying() || player.isRemoved() || player.getHealth() <= 0.0F) {
			return;
		}

		long now = level.getGameTime();
		Long suppressedUntil = SUPPRESSED_UNTIL.get(player.getUUID());
		if (suppressedUntil != null) {
			if (now <= suppressedUntil)
				return;
			SUPPRESSED_UNTIL.remove(player.getUUID());
		}

		Vec3 movement = player.getDeltaMovement();
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(
				player,
				new AngelsShroudEndBurstMessage(
						player.getX(),
						player.getY(),
						player.getZ(),
						player.getBbHeight(),
						(float) movement.x,
						(float) movement.y,
						(float) movement.z,
						player.getRandom().nextInt()
				)
		);
	}

	private static void suppressTransition(ServerPlayer player) {
		if (player != null && player.level() instanceof ServerLevel level) {
			SUPPRESSED_UNTIL.put(
					player.getUUID(),
					level.getGameTime() + TRANSITION_SUPPRESSION_TICKS
			);
		}
	}
}
