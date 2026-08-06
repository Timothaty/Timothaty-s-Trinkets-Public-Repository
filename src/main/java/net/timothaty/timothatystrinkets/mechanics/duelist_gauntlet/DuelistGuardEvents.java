package net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris.HubrisStrikeResolver;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class DuelistGuardEvents {
	private DuelistGuardEvents() {
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Pre event) {
		Player player = event.getEntity();
		if (player == null || player.level().isClientSide())
			return;

		DuelistGuardState.tick(player);
		if (player.tickCount % DuelistGuardData.DEBUG_ACTIONBAR_INTERVAL_TICKS == 0 && shouldShowDebug(player)) {
			DuelistGuardDebug.show(player,
					"Guard " + DuelistGuardState.getDirection(player)
							+ " | Stamina " + Math.round(DuelistGuardState.getStamina(player))
							+ " | Break CD " + DuelistGuardState.getBreakCooldown(player),
					ChatFormatting.GRAY);
		}
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
			DuelistGuardState.stopGuarding(player);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
		if (event == null || event.getAmount() <= 0.0F)
			return;
		if (!(event.getEntity() instanceof Player player))
			return;
		if (player.level().isClientSide())
			return;
		if (!DuelistGuardState.isGuarding(player))
			return;
		if (!DuelistGauntletCurios.hasGauntletEquipped(player) || !DuelistGuardState.isValidGuardWeapon(player) || DuelistGuardState.hasShieldInHand(player)) {
			DuelistGuardState.stopGuarding(player);
			return;
		}

		DamageSource source = event.getSource();
		LivingEntity attacker = DuelistMeleeDamage.getLivingAttacker(source);
		if (attacker == null || attacker == player)
			return;

		double relativeAttackAngle = DuelistGuardAngles.getRelativeAttackAngle(player, attacker);
		DuelistGuardDirection guardDirection = DuelistGuardState.getDirection(player);
		if (!DuelistGuardAngles.isInsideFrontalGuardArc(relativeAttackAngle)) {
			DuelistGuardDebug.show(player, "Guard " + guardDirection + " | Outside arc " + Math.round(relativeAttackAngle) + " | Stamina " + Math.round(DuelistGuardState.getStamina(player)), ChatFormatting.YELLOW);
			return;
		}
		if (isCenterParry(guardDirection)) {
			handleCenterParry(event, player, attacker);
			return;
		}
		if (DuelistGuardSideDeflect.tryHandle(event, player, attacker, guardDirection))
			return;

		DuelistGuardDebug.show(player, "Guard " + guardDirection + " | Front arc " + Math.round(relativeAttackAngle) + " | Stamina " + Math.round(DuelistGuardState.getStamina(player)), ChatFormatting.YELLOW);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
		if (shouldCancelGuardInteraction(event.getEntity())) {
			event.setCancellationResult(InteractionResult.FAIL);
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (shouldCancelGuardInteraction(event.getEntity())) {
			event.setCancellationResult(InteractionResult.FAIL);
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (shouldCancelGuardInteraction(event.getEntity())) {
			event.setCancellationResult(InteractionResult.FAIL);
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
		if (shouldCancelGuardInteraction(event.getEntity())) {
			event.setCancellationResult(InteractionResult.FAIL);
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onAttack(AttackEntityEvent event) {
		if (DuelistGuardState.isGuarding(event.getEntity())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		if (DuelistGuardState.isGuarding(event.getEntity())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onBreakBlock(BlockEvent.BreakEvent event) {
		if (DuelistGuardState.isGuarding(event.getPlayer())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
		if (event.getEntity() instanceof Player player && shouldCancelGuardInteraction(player)) {
			event.setCanceled(true);
		}
	}

	private static boolean isCenterParry(DuelistGuardDirection guardDirection) {
		return guardDirection == DuelistGuardDirection.CENTER;
	}

	private static void handleCenterParry(LivingIncomingDamageEvent event, Player player, LivingEntity attacker) {
		float blockedDamage = event.getAmount();
		HubrisStrikeResolver.markDefended(
				player,
				event.getSource(),
				HubrisStrikeResolver.DefenseKind.DUELIST_CENTER_PARRY
		);
		event.setAmount(0.0F);
		event.setCanceled(true);
		boolean hasStamina = DuelistGuardState.consumeCenterParryStamina(player, blockedDamage);
		DuelistGauntletDurability.damageForCenterParry(player, attacker);
		DuelistGuardParticles.spawnCenterParrySparks(player, attacker);
		playParrySound(player);
		DuelistGuardDebug.show(player, "Parried CENTER | Stamina " + Math.round(DuelistGuardState.getStamina(player)), ChatFormatting.GREEN);
		if (!hasStamina) {
			DuelistGuardState.triggerGuardBreak(player);
			DuelistGuardDebug.show(player, "Guard BROKEN | Parried CENTER", ChatFormatting.RED);
		}
	}

	private static boolean shouldShowDebug(Player player) {
		return DuelistGuardState.isGuarding(player)
				|| DuelistGuardState.getBreakCooldown(player) > 0
				|| DuelistGuardState.getDirection(player) == DuelistGuardDirection.BROKEN;
	}

	private static boolean shouldCancelGuardInteraction(Player player) {
		return DuelistGuardState.isGuarding(player) && !DuelistGuardState.hasShieldInHand(player);
	}

	private static void playParrySound(Player player) {
		player.level().playSound(null, player.blockPosition(), TimothatysTrinketsModSounds.SWORD_PARRY.get(), SoundSource.PLAYERS, 0.85F, 0.82F + player.getRandom().nextFloat() * 0.18F);
	}
}
