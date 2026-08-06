package net.timothaty.timothatystrinkets.client.hubris;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris.HubrisAnimationVariant;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris.HubrisData;
import net.timothaty.timothatystrinkets.network.HubrisActivationMessage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class HubrisActivationClientState {
	private static final Map<Integer, State> STATES = new HashMap<>();
	private static ClientLevel trackedLevel;

	private HubrisActivationClientState() {
	}

	public static void handle(HubrisActivationMessage message) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		synchronizeLevel(level);
		if (!message.active()) {
			State state = STATES.get(message.entityId());
			if (state != null && state.sessionToken == message.sessionToken()) {
				STATES.remove(message.entityId());
				HubrisBodyRotationState.finish(message.entityId());
			}
			return;
		}
		if (level == null || message.entityId() <= 0)
			return;

		State existing = STATES.get(message.entityId());
		if (existing != null && existing.startGameTime > message.startGameTime())
			return;
		HumanoidArm[] arms = HumanoidArm.values();
		HubrisAnimationVariant[] variants = HubrisAnimationVariant.values();
		HumanoidArm arm = arms[Mth.clamp(message.mainArmOrdinal(), 0, arms.length - 1)];
		HubrisAnimationVariant variant = variants[Mth.clamp(message.variantOrdinal(), 0, variants.length - 1)];
		STATES.put(message.entityId(), new State(
				message.sessionToken(),
				message.startGameTime(),
				arm,
				Mth.clamp(message.selectedHotbarSlot(), 0, 8),
				variant,
				message.weaponSnapshot().copy()
		));
		HubrisBodyRotationState.start(message.entityId(), message.startGameTime(), arm);
	}

	public static boolean isCasting(Entity entity) {
		return findActive(entity) != null;
	}

	public static boolean isInputLocked(LocalPlayer player) {
		return isCasting(player);
	}

	public static float elapsedTicks(LivingEntity entity, float ageInTicks) {
		State state = findActive(entity);
		if (state == null)
			return -1.0F;
		float partialTick = Mth.clamp(ageInTicks - entity.tickCount, 0.0F, 1.0F);
		return entity.level().getGameTime() - state.startGameTime + partialTick;
	}

	public static View getView(Entity entity) {
		State state = findActive(entity);
		return state == null ? null : state.view();
	}

	public static View getView(Entity entity, float partialTick) {
		State state = findActive(entity);
		if (state == null)
			return null;
		return state.viewWithElapsed(
				entity.level().getGameTime() - state.startGameTime + Mth.clamp(partialTick, 0.0F, 1.0F)
		);
	}

	public static View getLocalView(float partialTick) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		State state = findActive(player);
		if (state == null || player == null)
			return null;
		return state.viewWithElapsed(
				player.level().getGameTime() - state.startGameTime + Mth.clamp(partialTick, 0.0F, 1.0F)
		);
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		synchronizeLevel(level);
		if (level == null)
			return;

		long now = level.getGameTime();
		Iterator<Map.Entry<Integer, State>> iterator = STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, State> entry = iterator.next();
			Entity entity = level.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity living)
					|| !living.isAlive()
					|| living.isRemoved()
					|| now - entry.getValue().startGameTime >= HubrisData.ACTIVATION_TICKS) {
				iterator.remove();
			}
		}

		LocalPlayer player = minecraft.player;
		State local = player == null ? null : STATES.get(player.getId());
		if (player != null && local != null) {
			player.getInventory().selected = local.selectedHotbarSlot;
			player.setSprinting(false);
		}
	}

	public static void clear() {
		STATES.clear();
		trackedLevel = null;
		HubrisBodyRotationState.clear();
	}

	private static State findActive(Entity entity) {
		if (entity == null || entity.level() != trackedLevel || !entity.isAlive() || entity.isRemoved())
			return null;
		State state = STATES.get(entity.getId());
		if (state == null)
			return null;
		if (entity.level().getGameTime() - state.startGameTime >= HubrisData.ACTIVATION_TICKS) {
			STATES.remove(entity.getId());
			return null;
		}
		return state;
	}

	private static void synchronizeLevel(ClientLevel level) {
		if (trackedLevel == level)
			return;
		STATES.clear();
		trackedLevel = level;
	}

	public record View(
			long sessionToken,
			long startGameTime,
			HumanoidArm mainArm,
			int selectedHotbarSlot,
			HubrisAnimationVariant variant,
			ItemStack weaponSnapshot,
			float elapsedTicks
	) {
	}

	private static final class State {
		private final long sessionToken;
		private final long startGameTime;
		private final HumanoidArm mainArm;
		private final int selectedHotbarSlot;
		private final HubrisAnimationVariant variant;
		private final ItemStack weaponSnapshot;

		private State(long sessionToken, long startGameTime, HumanoidArm mainArm, int selectedHotbarSlot, HubrisAnimationVariant variant, ItemStack weaponSnapshot) {
			this.sessionToken = sessionToken;
			this.startGameTime = startGameTime;
			this.mainArm = mainArm;
			this.selectedHotbarSlot = selectedHotbarSlot;
			this.variant = variant;
			this.weaponSnapshot = weaponSnapshot;
		}

		private View view() {
			return viewWithElapsed(0.0F);
		}

		private View viewWithElapsed(float elapsedTicks) {
			return new View(sessionToken, startGameTime, mainArm, selectedHotbarSlot, variant, weaponSnapshot, elapsedTicks);
		}
	}
}
