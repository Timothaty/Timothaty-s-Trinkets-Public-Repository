package net.timothaty.timothatystrinkets.client.hubris;

import com.mojang.blaze3d.vertex.PoseStack;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris.HubrisData;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class PlayerHandAnchorTracker {
	private static final Map<Integer, Anchor> ANCHORS = new HashMap<>();
	private static final Map<Integer, Long> NEXT_PARTICLE_TICKS = new HashMap<>();
	private static ClientLevel trackedLevel;

	private PlayerHandAnchorTracker() {
	}

	public static void captureThirdPerson(AbstractClientPlayer player, PoseStack poseStack) {
		if (player == null || poseStack == null || !player.hasEffect(TimothatysTrinketsModMobEffects.HUBRIS))
			return;
		Vector3f position = poseStack.last().pose().transformPosition(0.0F, 0.25F, 0.03125F, new Vector3f());
		Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		update(player, new Vec3(position.x() + camera.x, position.y() + camera.y, position.z() + camera.z), false);
	}

	public static void captureFirstPerson(LocalPlayer player, PoseStack poseStack, HumanoidArm arm) {
		if (player == null || poseStack == null || !player.hasEffect(TimothatysTrinketsModMobEffects.HUBRIS))
			return;
		float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
		Vector3f cameraRelative = poseStack.last().pose().transformPosition(side * 0.070625F, 0.20F, 0.070625F, new Vector3f());
		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
		camera.rotation().transform(cameraRelative);
		Vec3 cameraPosition = camera.getPosition();
		update(player, new Vec3(
				cameraPosition.x + cameraRelative.x(),
				cameraPosition.y + cameraRelative.y(),
				cameraPosition.z + cameraRelative.z()
		), true);
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (trackedLevel != level) {
			clear();
			trackedLevel = level;
		}
		if (level == null)
			return;

		long now = level.getGameTime();
		Set<Integer> activePlayers = new HashSet<>();
		for (AbstractClientPlayer player : level.players()) {
			if (!player.isAlive() || !player.hasEffect(TimothatysTrinketsModMobEffects.HUBRIS))
				continue;
			ItemStack weapon = player.getMainHandItem();
			if (!weapon.is(net.minecraft.tags.ItemTags.SWORDS) && !weapon.is(HubrisData.HEAVY_ARMS))
				continue;
			activePlayers.add(player.getId());
			Anchor anchor = ANCHORS.get(player.getId());
			if (anchor == null || now - anchor.gameTime > 2L)
				continue;
			long next = NEXT_PARTICLE_TICKS.getOrDefault(player.getId(), now);
			if (now < next)
				continue;

			long mixed = mix(player.getId(), now);
			float size = 0.6F + (Math.floorMod(mixed >>> 8, 301L) / 1000.0F);
			DustParticleOptions dust = new DustParticleOptions(
					new Vector3f(HubrisData.CRIMSON_RED, HubrisData.CRIMSON_GREEN, HubrisData.CRIMSON_BLUE),
					size
			);
			level.addParticle(dust, anchor.position.x, anchor.position.y, anchor.position.z, 0.0D, 0.004D, 0.0D);
			NEXT_PARTICLE_TICKS.put(player.getId(), now + 6L + Math.floorMod(mixed, 5L));
		}

		NEXT_PARTICLE_TICKS.keySet().removeIf(id -> !activePlayers.contains(id));
		ANCHORS.entrySet().removeIf(entry -> now - entry.getValue().gameTime > 3L);
	}

	public static void clear() {
		ANCHORS.clear();
		NEXT_PARTICLE_TICKS.clear();
		trackedLevel = null;
	}

	private static void update(AbstractClientPlayer player, Vec3 position, boolean firstPerson) {
		ClientLevel level = (ClientLevel) player.level();
		long now = level.getGameTime();
		Anchor existing = ANCHORS.get(player.getId());
		if (existing != null && existing.gameTime == now && existing.firstPerson && !firstPerson)
			return;
		ANCHORS.put(player.getId(), new Anchor(position, now, firstPerson));
	}

	private static long mix(int entityId, long gameTime) {
		long value = gameTime ^ entityId * 0x9E3779B97F4A7C15L;
		value ^= value >>> 30;
		value *= 0xBF58476D1CE4E5B9L;
		value ^= value >>> 27;
		value *= 0x94D049BB133111EBL;
		return value ^ value >>> 31;
	}

	private record Anchor(Vec3 position, long gameTime, boolean firstPerson) {
	}
}
