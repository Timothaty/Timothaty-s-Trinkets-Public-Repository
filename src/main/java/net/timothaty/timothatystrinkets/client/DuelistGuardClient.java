package net.timothaty.timothatystrinkets.client;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.duelist.DuelistGuardClientFeedback;
import net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet.DuelistGauntletCurios;
import net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet.DuelistGuardData;
import net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet.DuelistGuardDirection;
import net.timothaty.timothatystrinkets.network.DuelistGuardStateMessage;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class DuelistGuardClient {
	private static boolean wasEligible = false;
	private static boolean lastSentGuarding = false;
	private static boolean attackHeldForCenterReset = false;
	private static float previousYaw = 0.0F;
	private static int guardSwitchCooldown = 0;
	private static float guardCursor = 0.0F;
	private static float serverStamina = DuelistGuardData.MAX_STAMINA;
	private static final Map<Integer, DuelistGuardDirection> serverVisualDirections = new HashMap<>();
	private static DuelistGuardDirection currentDirection = DuelistGuardDirection.NONE;
	private static DuelistGuardDirection lastSentDirection = DuelistGuardDirection.NONE;

	private DuelistGuardClient() {
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null) {
			resetLocalState();
			return;
		}
		if (!isPlayerRenderableForGuard(player)) {
			stopGuardingNow();
			previousYaw = player.getYRot();
			return;
		}

		boolean eligible = isGuardInputEligible(minecraft, player);
		DuelistGuardDirection direction = DuelistGuardDirection.NONE;
		if (eligible) {
			if (isGuardWeaponCoolingDown(player) || attackHeldForCenterReset) {
				forceDirectionToCenter();
				previousYaw = player.getYRot();
				direction = currentDirection;
			} else {
				direction = updateDirection(player);
			}
		} else {
			guardSwitchCooldown = 0;
			guardCursor = 0.0F;
			currentDirection = DuelistGuardDirection.NONE;
			previousYaw = player.getYRot();
			attackHeldForCenterReset = false;
		}

		sendGuardStateIfChanged(eligible, direction);
		if (eligible) {
			releaseAttackKey(minecraft);
		}

		wasEligible = eligible;
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!minecraft.options.keyAttack.matchesMouse(event.getButton()))
			return;

		updateAttackHeldForCenterReset(event.getAction());
		LocalPlayer player = minecraft.player;
		if (shouldBlockAttackInput(minecraft, player)) {
			event.setCanceled(true);
			releaseAttackKey(minecraft);
		}
	}

	@SubscribeEvent
	public static void onKey(InputEvent.Key event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!minecraft.options.keyAttack.matches(event.getKey(), event.getScanCode()))
			return;

		updateAttackHeldForCenterReset(event.getAction());
		if (shouldBlockAttackInput(minecraft, minecraft.player)) {
			releaseAttackKey(minecraft);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
		if (!event.isAttack())
			return;

		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (!shouldBlockAttackInput(minecraft, player))
			return;

		event.setSwingHand(false);
		event.setCanceled(true);
		releaseAttackKey(minecraft);
	}

	private static DuelistGuardDirection updateDirection(LocalPlayer player) {
		float yaw = player.getYRot();
		if (!wasEligible) {
			previousYaw = yaw;
			guardSwitchCooldown = 0;
			guardCursor = 0.0F;
			currentDirection = DuelistGuardDirection.CENTER;
			return currentDirection;
		}

		float deltaYaw = Mth.wrapDegrees(yaw - previousYaw);
		previousYaw = yaw;
		if (isGuardWeaponCoolingDown(player)) {
			forceDirectionToCenter();
			return currentDirection;
		}

		updateGuardCursor(deltaYaw);
		if (guardSwitchCooldown > 0) {
			guardSwitchCooldown--;
			return currentDirection.canBeHeldByPlayer() ? currentDirection : DuelistGuardDirection.CENTER;
		}

		applyDirectionFromCursor(player);
		return currentDirection.canBeHeldByPlayer() ? currentDirection : DuelistGuardDirection.CENTER;
	}

	private static void updateGuardCursor(float deltaYaw) {
		if (Math.abs(deltaYaw) < DuelistGuardData.CLIENT_YAW_DEADZONE_DEGREES) {
			guardCursor = Mth.approach(guardCursor, 0.0F, DuelistGuardData.CLIENT_GUARD_CURSOR_RETURN_STEP);
			return;
		}
		guardCursor = Mth.clamp(guardCursor + deltaYaw * DuelistGuardData.CLIENT_GUARD_CURSOR_YAW_SCALE, -1.0F, 1.0F);
	}

	private static void applyDirectionFromCursor(LocalPlayer player) {
		DuelistGuardDirection target = getDirectionFromCursor();
		if (target != currentDirection) {
			setDirection(player, target);
		}
	}

	private static DuelistGuardDirection getDirectionFromCursor() {
		if (isGuardWeaponCoolingDown(Minecraft.getInstance().player))
			return DuelistGuardDirection.CENTER;
		if (currentDirection == DuelistGuardDirection.LEFT) {
			return guardCursor >= -DuelistGuardData.CLIENT_GUARD_CURSOR_CENTER_ENTER ? DuelistGuardDirection.CENTER : DuelistGuardDirection.LEFT;
		}
		if (currentDirection == DuelistGuardDirection.RIGHT) {
			return guardCursor <= DuelistGuardData.CLIENT_GUARD_CURSOR_CENTER_ENTER ? DuelistGuardDirection.CENTER : DuelistGuardDirection.RIGHT;
		}
		if (guardCursor <= -DuelistGuardData.CLIENT_GUARD_CURSOR_SIDE_ENTER)
			return DuelistGuardDirection.LEFT;
		if (guardCursor >= DuelistGuardData.CLIENT_GUARD_CURSOR_SIDE_ENTER)
			return DuelistGuardDirection.RIGHT;
		return DuelistGuardDirection.CENTER;
	}

	private static void resetDirectionToCenter(LocalPlayer player) {
		guardCursor = 0.0F;
		setDirection(player, DuelistGuardDirection.CENTER);
	}

	private static void forceDirectionToCenter() {
		guardCursor = 0.0F;
		guardSwitchCooldown = 0;
		currentDirection = DuelistGuardDirection.CENTER;
	}

	private static void updateAttackHeldForCenterReset(int action) {
		if (action == GLFW.GLFW_PRESS) {
			attackHeldForCenterReset = true;
		} else if (action == GLFW.GLFW_RELEASE) {
			attackHeldForCenterReset = false;
		}
	}

	private static void setDirection(LocalPlayer player, DuelistGuardDirection direction) {
		if (direction == currentDirection || !direction.canBeHeldByPlayer())
			return;
		if (direction.isSide() && isGuardWeaponCoolingDown(player))
			return;
		DuelistGuardDirection previous = currentDirection;
		currentDirection = direction;
		guardSwitchCooldown = DuelistGuardData.CLIENT_GUARD_SWITCH_COOLDOWN_TICKS;
		DuelistGuardClientFeedback.playDirectionShift(player, getShiftSign(previous, currentDirection), previous, currentDirection);
		sendGuardStateIfChanged(true, currentDirection);
	}

	private static int getShiftSign(DuelistGuardDirection previous, DuelistGuardDirection current) {
		return sideValue(current) >= sideValue(previous) ? 1 : -1;
	}

	private static int sideValue(DuelistGuardDirection direction) {
		return switch (direction) {
			case LEFT -> -1;
			case RIGHT -> 1;
			default -> 0;
		};
	}

	private static boolean isGuardInputEligible(Minecraft minecraft, LocalPlayer player) {
		return minecraft.screen == null
				&& minecraft.options.keyUse.isDown()
				&& isPlayerRenderableForGuard(player)
				&& !player.isSpectator()
				&& DuelistGauntletCurios.hasGauntletEquipped(player)
				&& hasSwordInHand(player)
				&& !hasShieldInHand(player);
	}

	private static boolean shouldBlockAttackInput(Minecraft minecraft, LocalPlayer player) {
		return player != null && minecraft.screen == null && !hasShieldInHand(player) && (isVisuallyGuarding() || isGuardInputEligible(minecraft, player));
	}

	private static boolean hasSwordInHand(LocalPlayer player) {
		return !getGuardWeapon(player).isEmpty();
	}

	private static ItemStack getGuardWeapon(LocalPlayer player) {
		if (player == null)
			return ItemStack.EMPTY;
		if (isSword(player.getMainHandItem()))
			return player.getMainHandItem();
		if (isSword(player.getOffhandItem()))
			return player.getOffhandItem();
		return ItemStack.EMPTY;
	}

	private static boolean isSword(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.getItem() instanceof SwordItem;
	}

	private static boolean hasShieldInHand(LocalPlayer player) {
		return player != null && (isShield(player.getMainHandItem()) || isShield(player.getOffhandItem()));
	}

	private static boolean isShield(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.getItem() instanceof ShieldItem;
	}

	private static boolean isPlayerRenderableForGuard(LocalPlayer player) {
		return player != null && player.isAlive() && !player.isDeadOrDying() && !player.isRemoved();
	}

	private static void releaseAttackKey(Minecraft minecraft) {
		KeyMapping attack = minecraft.options.keyAttack;
		attack.setDown(false);
		while (attack.consumeClick()) {
		}
	}

	private static boolean isGuardWeaponCoolingDown(LocalPlayer player) {
		ItemStack stack = getGuardWeapon(player);
		return player != null && !stack.isEmpty() && player.getCooldowns().isOnCooldown(stack.getItem());
	}

	private static void sendGuardStateIfChanged(boolean guarding, DuelistGuardDirection direction) {
		if (guarding != lastSentGuarding || direction != lastSentDirection) {
			PacketDistributor.sendToServer(new DuelistGuardStateMessage(guarding, direction.networkId()));
			lastSentGuarding = guarding;
			lastSentDirection = direction;
		}
	}

	private static void stopGuardingNow() {
		sendGuardStateIfChanged(false, DuelistGuardDirection.NONE);
		wasEligible = false;
		attackHeldForCenterReset = false;
		guardSwitchCooldown = 0;
		guardCursor = 0.0F;
		currentDirection = DuelistGuardDirection.NONE;
		lastSentGuarding = false;
		lastSentDirection = DuelistGuardDirection.NONE;
	}

	public static boolean isVisuallyGuarding() {
		return lastSentGuarding && currentDirection.canBeHeldByPlayer();
	}

	public static DuelistGuardDirection getVisualGuardDirection() {
		return isVisuallyGuarding() ? currentDirection : DuelistGuardDirection.NONE;
	}

	public static void setServerStamina(float stamina) {
		serverStamina = Mth.clamp(stamina, 0.0F, DuelistGuardData.MAX_STAMINA);
	}

	public static float getSyncedStaminaProgress() {
		return Mth.clamp(serverStamina / DuelistGuardData.MAX_STAMINA, 0.0F, 1.0F);
	}

	public static void setServerGuardVisualState(int entityId, boolean guarding, DuelistGuardDirection direction) {
		if (!guarding || direction == null || !direction.canBeHeldByPlayer()) {
			serverVisualDirections.remove(entityId);
			return;
		}
		serverVisualDirections.put(entityId, direction);
	}

	public static DuelistGuardDirection getThirdPersonGuardDirection(LivingEntity entity) {
		if (entity == null)
			return DuelistGuardDirection.NONE;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == entity) {
			return isPlayerRenderableForGuard(minecraft.player) ? getVisualGuardDirection() : DuelistGuardDirection.NONE;
		}

		return serverVisualDirections.getOrDefault(entity.getId(), DuelistGuardDirection.NONE);
	}

	private static void resetLocalState() {
		wasEligible = false;
		lastSentGuarding = false;
		attackHeldForCenterReset = false;
		guardSwitchCooldown = 0;
		guardCursor = 0.0F;
		currentDirection = DuelistGuardDirection.NONE;
		lastSentDirection = DuelistGuardDirection.NONE;
		serverStamina = DuelistGuardData.MAX_STAMINA;
		serverVisualDirections.clear();
	}
}
