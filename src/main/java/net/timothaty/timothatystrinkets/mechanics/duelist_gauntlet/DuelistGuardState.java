package net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet;

import net.timothaty.timothatystrinkets.network.DuelistGuardStaminaMessage;
import net.timothaty.timothatystrinkets.network.DuelistGuardVisualStateMessage;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;

public final class DuelistGuardState {
	private DuelistGuardState() {
	}

	public static void tick(Player player) {
		if (player == null)
			return;

		ensureInitialized(player);
		tickBreakCooldown(player);

		if (isGuarding(player)) {
			setRegenDelay(player, DuelistGuardData.STAMINA_REGEN_DELAY_TICKS);
			if (!canContinueGuarding(player)) {
				stopGuarding(player);
			}
			syncStaminaOccasionally(player);
			syncVisualStateOccasionally(player);
			return;
		}

		regenerateStamina(player);
		syncStaminaOccasionally(player);
	}

	public static void receiveClientState(Player player, boolean guarding, DuelistGuardDirection direction) {
		if (player == null || player.level().isClientSide())
			return;

		ensureInitialized(player);
		if (!guarding) {
			stopGuarding(player);
			return;
		}

		DuelistGuardDirection requested = direction != null && direction.canBeHeldByPlayer() ? direction : DuelistGuardDirection.CENTER;
		DuelistGuardDirection sanitized = canUseGuardDirection(player, requested) ? requested : DuelistGuardDirection.CENTER;
		if (!canStartOrContinueGuarding(player)) {
			stopGuarding(player);
			return;
		}

		CompoundTag tag = player.getPersistentData();
		tag.putBoolean(DuelistGuardData.NBT_GUARDING, true);
		tag.putInt(DuelistGuardData.NBT_DIRECTION, sanitized.networkId());
		setRegenDelay(player, DuelistGuardData.STAMINA_REGEN_DELAY_TICKS);
		syncVisualState(player);
	}

	public static boolean isGuarding(Player player) {
		if (player == null)
			return false;
		CompoundTag tag = player.getPersistentData();
		return tag.getBoolean(DuelistGuardData.NBT_GUARDING) && getDirection(player).canBeHeldByPlayer();
	}

	public static DuelistGuardDirection getDirection(Player player) {
		if (player == null)
			return DuelistGuardDirection.NONE;
		return DuelistGuardDirection.fromNetworkId(player.getPersistentData().getInt(DuelistGuardData.NBT_DIRECTION));
	}

	public static float getStamina(Player player) {
		if (player == null)
			return 0.0F;
		ensureInitialized(player);
		return Mth.clamp(player.getPersistentData().getFloat(DuelistGuardData.NBT_STAMINA), 0.0F, DuelistGuardData.MAX_STAMINA);
	}

	public static int getBreakCooldown(Player player) {
		if (player == null)
			return 0;
		return Math.max(0, player.getPersistentData().getInt(DuelistGuardData.NBT_BREAK_COOLDOWN));
	}

	public static boolean canUseGuardDirection(Player player, DuelistGuardDirection direction) {
		if (direction == null || !direction.canBeHeldByPlayer())
			return false;
		return !direction.isSide() || !isGuardWeaponOnCooldown(player);
	}

	public static boolean consumeCenterParryStamina(Player player, float incomingDamage) {
		if (player == null)
			return false;
		return consumeStamina(player, Math.max(0.0F, incomingDamage) * DuelistGuardData.CENTER_PARRY_STAMINA_COST_MULTIPLIER);
	}

	public static boolean consumeSideDeflectStamina(Player player) {
		if (player == null)
			return false;
		return consumeStamina(player, DuelistGuardData.SIDE_DEFLECT_STAMINA_COST);
	}

	public static void addRiposteStamina(Player player) {
		addStamina(player, DuelistGuardData.RIPOSTE_STAMINA_GAIN);
	}

	public static void addStamina(Player player, float amount) {
		if (player == null || amount <= 0.0F)
			return;
		setStamina(player, getStamina(player) + amount);
	}

	public static void refillStamina(Player player) {
		if (player == null)
			return;
		setStamina(player, DuelistGuardData.MAX_STAMINA);
	}

	public static void applySideDeflectWeaponCooldown(Player player) {
		if (player == null)
			return;

		ItemStack stack = getGuardWeapon(player);
		if (!stack.isEmpty()) {
			player.getCooldowns().addCooldown(stack.getItem(), DuelistGuardData.SIDE_DEFLECT_WEAPON_COOLDOWN_TICKS);
		}
		if (getDirection(player).isSide()) {
			player.getPersistentData().putInt(DuelistGuardData.NBT_DIRECTION, DuelistGuardDirection.CENTER.networkId());
			syncVisualState(player);
		}
	}

	public static boolean isGuardWeaponOnCooldown(Player player) {
		if (player == null)
			return false;
		ItemStack stack = getGuardWeapon(player);
		return !stack.isEmpty() && player.getCooldowns().isOnCooldown(stack.getItem());
	}

	public static void triggerGuardBreak(Player player) {
		if (player == null)
			return;

		CompoundTag tag = player.getPersistentData();
		tag.putBoolean(DuelistGuardData.NBT_GUARDING, false);
		tag.putInt(DuelistGuardData.NBT_DIRECTION, DuelistGuardDirection.BROKEN.networkId());
		setStamina(player, 0.0F);
		tag.putInt(DuelistGuardData.NBT_BREAK_COOLDOWN, DuelistGuardData.GUARD_BREAK_COOLDOWN_TICKS);
		setRegenDelay(player, DuelistGuardData.STAMINA_REGEN_DELAY_TICKS);
		syncVisualState(player);
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DuelistGuardData.GUARD_BREAK_EFFECT_TICKS, DuelistGuardData.GUARD_BREAK_SLOWNESS_AMPLIFIER, false, true, true));
		player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, DuelistGuardData.GUARD_BREAK_EFFECT_TICKS, DuelistGuardData.GUARD_BREAK_FATIGUE_AMPLIFIER, false, true, true));
	}

	public static void stopGuarding(Player player) {
		if (player == null)
			return;
		CompoundTag tag = player.getPersistentData();
		boolean wasGuarding = tag.getBoolean(DuelistGuardData.NBT_GUARDING);
		tag.putBoolean(DuelistGuardData.NBT_GUARDING, false);
		if (getDirection(player) != DuelistGuardDirection.BROKEN) {
			tag.putInt(DuelistGuardData.NBT_DIRECTION, DuelistGuardDirection.NONE.networkId());
		}
		if (wasGuarding) {
			setRegenDelay(player, DuelistGuardData.STAMINA_REGEN_DELAY_TICKS);
			syncVisualState(player);
		}
	}

	public static boolean isValidGuardWeapon(Player player) {
		return player != null && !getGuardWeapon(player).isEmpty();
	}

	public static boolean hasShieldInHand(Player player) {
		return player != null && (isShield(player.getMainHandItem()) || isShield(player.getOffhandItem()));
	}

	public static ItemStack getGuardWeapon(Player player) {
		if (player == null)
			return ItemStack.EMPTY;
		if (isSword(player.getMainHandItem()))
			return player.getMainHandItem();
		if (isSword(player.getOffhandItem()))
			return player.getOffhandItem();
		return ItemStack.EMPTY;
	}

	private static boolean canStartOrContinueGuarding(Player player) {
		return player != null
				&& player.isAlive()
				&& !player.isDeadOrDying()
				&& !player.isRemoved()
				&& !player.isSpectator()
				&& getBreakCooldown(player) <= 0
				&& getStamina(player) > 0.0F
				&& DuelistGauntletCurios.hasGauntletEquipped(player)
				&& isValidGuardWeapon(player)
				&& !hasShieldInHand(player);
	}

	private static boolean canContinueGuarding(Player player) {
		return canStartOrContinueGuarding(player);
	}

	private static boolean isSword(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.getItem() instanceof SwordItem;
	}

	private static boolean isShield(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.getItem() instanceof ShieldItem;
	}

	private static void ensureInitialized(Player player) {
		CompoundTag tag = player.getPersistentData();
		if (!tag.contains(DuelistGuardData.NBT_STAMINA)) {
			tag.putFloat(DuelistGuardData.NBT_STAMINA, DuelistGuardData.MAX_STAMINA);
		}
	}

	private static void tickBreakCooldown(Player player) {
		CompoundTag tag = player.getPersistentData();
		int cooldown = tag.getInt(DuelistGuardData.NBT_BREAK_COOLDOWN);
		if (cooldown <= 0)
			return;

		cooldown--;
		tag.putInt(DuelistGuardData.NBT_BREAK_COOLDOWN, cooldown);
		if (cooldown <= 0 && getDirection(player) == DuelistGuardDirection.BROKEN) {
			tag.putInt(DuelistGuardData.NBT_DIRECTION, DuelistGuardDirection.NONE.networkId());
		}
	}

	private static void regenerateStamina(Player player) {
		CompoundTag tag = player.getPersistentData();
		int regenDelay = tag.getInt(DuelistGuardData.NBT_REGEN_DELAY);
		if (regenDelay > 0) {
			tag.putInt(DuelistGuardData.NBT_REGEN_DELAY, regenDelay - 1);
			return;
		}

		float stamina = getStamina(player);
		if (stamina >= DuelistGuardData.MAX_STAMINA)
			return;
		setStamina(player, stamina + DuelistGuardData.STAMINA_REGEN_PER_TICK);
	}

	private static boolean consumeStamina(Player player, float cost) {
		if (player == null)
			return false;
		if (cost <= 0.0F)
			return getStamina(player) > 0.0F;

		float stamina = Math.max(0.0F, getStamina(player) - cost);
		setStamina(player, stamina);
		return stamina > 0.0F;
	}

	private static void setStamina(Player player, float stamina) {
		CompoundTag tag = player.getPersistentData();
		float clamped = Mth.clamp(stamina, 0.0F, DuelistGuardData.MAX_STAMINA);
		if (Math.abs(tag.getFloat(DuelistGuardData.NBT_STAMINA) - clamped) < 0.001F)
			return;
		tag.putFloat(DuelistGuardData.NBT_STAMINA, clamped);
		syncStamina(player);
	}

	private static void setRegenDelay(Player player, int ticks) {
		player.getPersistentData().putInt(DuelistGuardData.NBT_REGEN_DELAY, Math.max(0, ticks));
	}

	private static void syncStaminaOccasionally(Player player) {
		if (player.tickCount % DuelistGuardData.STAMINA_SYNC_INTERVAL_TICKS != 0)
			return;
		if (!DuelistGauntletCurios.hasGauntletEquipped(player))
			return;
		syncStamina(player);
	}

	private static void syncStamina(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer, new DuelistGuardStaminaMessage(getStamina(player)));
		}
	}

	private static void syncVisualStateOccasionally(Player player) {
		if (player.tickCount % DuelistGuardData.STAMINA_SYNC_INTERVAL_TICKS == 0) {
			syncVisualState(player);
		}
	}

	private static void syncVisualState(Player player) {
		if (!(player instanceof ServerPlayer serverPlayer))
			return;

		boolean guarding = isGuarding(player);
		DuelistGuardDirection direction = guarding ? getDirection(player) : DuelistGuardDirection.NONE;
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer, new DuelistGuardVisualStateMessage(serverPlayer.getId(), guarding, direction.networkId()));
	}
}
