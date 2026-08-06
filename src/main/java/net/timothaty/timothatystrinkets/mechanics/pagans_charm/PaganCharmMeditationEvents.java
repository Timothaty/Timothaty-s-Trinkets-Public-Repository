package net.timothaty.timothatystrinkets.mechanics.pagans_charm;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class PaganCharmMeditationEvents {
	private PaganCharmMeditationEvents() {
	}

	@SubscribeEvent
	public static void onPlayerTickPost(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player.level().isClientSide())
			return;
		if (!(player instanceof PaganCharmMeditationPlayerState state))
			return;
		if (state.timothatys_trinkets$getPaganCharmMeditationPhase(player.tickCount) == PaganCharmMeditationPlayerState.PHASE_NONE)
			return;

		PaganCharmCharge.tickCharge(player, state);
	}

	@SubscribeEvent
	public static void onAttack(AttackEntityEvent event) {
		PaganCharmMeditationInterrupts.interrupt(event.getEntity());
	}

	@SubscribeEvent
	public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		PaganCharmMeditationInterrupts.interrupt(event.getEntity());
	}

	@SubscribeEvent
	public static void onBreakBlock(BlockEvent.BreakEvent event) {
		PaganCharmMeditationInterrupts.interrupt(event.getPlayer());
	}

	@SubscribeEvent
	public static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof Player player) {
			PaganCharmMeditationInterrupts.interrupt(player);
		}
	}

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		PaganCharmMeditationInterrupts.allowBenignSwing(event.getEntity());
	}

	@SubscribeEvent
	public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
		PaganCharmMeditationInterrupts.allowBenignSwing(event.getEntity());
	}

	@SubscribeEvent
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		PaganCharmMeditationInterrupts.allowBenignSwing(event.getEntity());
	}

	@SubscribeEvent
	public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
		PaganCharmMeditationInterrupts.allowBenignSwing(event.getEntity());
	}

	@SubscribeEvent
	public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
		if (event.getNewDamage() > 0.0F && event.getEntity() instanceof Player player) {
			PaganCharmMeditationInterrupts.interrupt(player);
		}
	}

	@SubscribeEvent
	public static void onTeleport(EntityTeleportEvent event) {
		if (event.getEntity() instanceof Player player) {
			PaganCharmMeditationInterrupts.interrupt(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		PaganCharmMeditationInterrupts.interrupt(event.getEntity());
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		PaganCharmMeditationInterrupts.clearAll();
	}

}
