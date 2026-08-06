package net.timothaty.timothatystrinkets.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class PlayerCastHandAnchorTracker {
	private static final float HAND_TIP_LOCAL_Y = 10.0F / 16.0F;
	private static final long MAX_ANCHOR_AGE_TICKS = 3L;
	private static final Map<Integer, HandAnchors> ANCHORS = new HashMap<>();

	private PlayerCastHandAnchorTracker() {
	}

	public static void capture(
			AbstractClientPlayer player,
			PlayerModel<?> model,
			PoseStack poseStack
	) {
		if (player == null || model == null || poseStack == null
				|| !PlayerCastHandDustVisuals.isActive(player.getId())) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		boolean palFirstPerson = FirstPersonMode.isFirstPersonPass()
				&& player == minecraft.player
				&& player == minecraft.getCameraEntity();
		Vec3 right = model.rightArm.visible ? captureArm(model.rightArm, poseStack, minecraft) : null;
		Vec3 left = model.leftArm.visible ? captureArm(model.leftArm, poseStack, minecraft) : null;
		if (right == null && left == null)
			return;

		ClientLevel level = (ClientLevel) player.level();
		long now = level.getGameTime();
		HandAnchors existing = ANCHORS.get(player.getId());
		if (existing != null && existing.gameTime == now && existing.palFirstPerson && !palFirstPerson)
			return;
		ANCHORS.put(player.getId(), new HandAnchors(right, left, now, palFirstPerson));
	}

	static HandAnchors getFresh(int entityId, long gameTime) {
		HandAnchors anchors = ANCHORS.get(entityId);
		if (anchors == null || gameTime - anchors.gameTime > MAX_ANCHOR_AGE_TICKS)
			return null;
		return anchors;
	}

	static void prune(long gameTime) {
		ANCHORS.entrySet().removeIf(entry -> gameTime - entry.getValue().gameTime > MAX_ANCHOR_AGE_TICKS);
	}

	static void remove(int entityId) {
		ANCHORS.remove(entityId);
	}

	public static void clear() {
		ANCHORS.clear();
	}

	private static Vec3 captureArm(ModelPart arm, PoseStack poseStack, Minecraft minecraft) {
		poseStack.pushPose();
		try {
			arm.translateAndRotate(poseStack);
			Vector3f cameraRelative = poseStack.last().pose().transformPosition(
					0.0F,
					HAND_TIP_LOCAL_Y,
					0.0F,
					new Vector3f()
			);
			Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
			return new Vec3(
					camera.x + cameraRelative.x(),
					camera.y + cameraRelative.y(),
					camera.z + cameraRelative.z()
			);
		} finally {
			poseStack.popPose();
		}
	}

	static final class HandAnchors {
		private final Vec3 right;
		private final Vec3 left;
		private final long gameTime;
		private final boolean palFirstPerson;

		private HandAnchors(Vec3 right, Vec3 left, long gameTime, boolean palFirstPerson) {
			this.right = right;
			this.left = left;
			this.gameTime = gameTime;
			this.palFirstPerson = palFirstPerson;
		}

		Vec3 right() {
			return this.right;
		}

		Vec3 left() {
			return this.left;
		}
	}
}
