package net.timothaty.timothatystrinkets.client.duelist;

import net.timothaty.timothatystrinkets.client.DuelistGuardClient;
import net.timothaty.timothatystrinkets.client.animation.HumanoidAnimationRenderPassHelper;
import net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet.DuelistGuardDirection;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class DuelistGuardThirdPersonAnimation {
	private static final float MIN_ALPHA_ON_TARGET_CHANGE = 0.16F;
	private static final float ALPHA_PER_TICK = 0.55F;
	private static final float MAX_ALPHA = 0.34F;
	private static final float SNAP_EPSILON = 0.015F;
	private static final int MAX_IDLE_AGE_TICKS = 80;

	private static final PoseAngles CENTER = PoseAngles.degrees(-97.3495F, 1.693F, 87.2795F);
	private static final PoseAngles LEFT = PoseAngles.degrees(-68.0576F, -61.6556F, -5.0269F);
	private static final PoseAngles RIGHT = PoseAngles.degrees(-62.8453F, -24.1116F, 51.462F);
	private static final Map<Integer, VisualState> STATES = new HashMap<>();

	private DuelistGuardThirdPersonAnimation() {
	}

	public static void apply(LivingEntity entity, float ageInTicks, ModelPart rightArm) {
		if (!(entity instanceof Player) || rightArm == null)
			return;
		if (!entity.isAlive() || entity.isDeadOrDying() || entity.isRemoved()) {
			STATES.remove(entity.getId());
			return;
		}

		DuelistGuardDirection direction = DuelistGuardClient.getThirdPersonGuardDirection(entity);
		VisualState state = STATES.get(entity.getId());
		if (!direction.canBeHeldByPlayer()) {
			if (state != null) {
				applyReturningToVanilla(entity, ageInTicks, rightArm, state);
			}
			cleanStaleStates(entity.tickCount);
			return;
		}

		if (state == null) {
			state = new VisualState(rightArm.xRot, rightArm.yRot, rightArm.zRot, ageInTicks, entity.tickCount);
			STATES.put(entity.getId(), state);
		}

		state.lastSeenTick = entity.tickCount;
		applyPose(rightArm, state, poseFor(direction), direction, ageInTicks);
	}

	public static boolean isVanillaFirstPersonArmPass(LivingEntity entity, float limbSwing, float limbSwingAmount,
			float ageInTicks, float netHeadYaw, float headPitch) {
		return HumanoidAnimationRenderPassHelper.isVanillaFirstPersonArmPass(
				entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch
		);
	}

	private static void applyReturningToVanilla(LivingEntity entity, float ageInTicks, ModelPart rightArm, VisualState state) {
		PoseAngles target = new PoseAngles(rightArm.xRot, rightArm.yRot, rightArm.zRot);
		state.lastSeenTick = entity.tickCount;
		applyPose(rightArm, state, target, DuelistGuardDirection.NONE, ageInTicks);
		if (state.isCloseTo(target)) {
			STATES.remove(entity.getId());
		}
	}

	private static void applyPose(ModelPart rightArm, VisualState state, PoseAngles target, DuelistGuardDirection targetDirection, float ageInTicks) {
		float alpha = state.nextAlpha(targetDirection, ageInTicks);
		state.xRot = Mth.lerp(alpha, state.xRot, target.xRot());
		state.yRot = Mth.lerp(alpha, state.yRot, target.yRot());
		state.zRot = Mth.lerp(alpha, state.zRot, target.zRot());

		rightArm.xRot = state.xRot;
		rightArm.yRot = state.yRot;
		rightArm.zRot = state.zRot;
	}

	private static PoseAngles poseFor(DuelistGuardDirection direction) {
		return switch (direction) {
			case LEFT -> LEFT;
			case RIGHT -> RIGHT;
			default -> CENTER;
		};
	}

	private static void cleanStaleStates(int currentTick) {
		Iterator<Map.Entry<Integer, VisualState>> iterator = STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			if (currentTick - iterator.next().getValue().lastSeenTick > MAX_IDLE_AGE_TICKS) {
				iterator.remove();
			}
		}
	}

	private record PoseAngles(float xRot, float yRot, float zRot) {
		private static PoseAngles degrees(float xRot, float yRot, float zRot) {
			return new PoseAngles(xRot * Mth.DEG_TO_RAD, yRot * Mth.DEG_TO_RAD, zRot * Mth.DEG_TO_RAD);
		}
	}

	private static final class VisualState {
		private float xRot;
		private float yRot;
		private float zRot;
		private float lastAgeInTicks;
		private int lastSeenTick;
		private DuelistGuardDirection targetDirection = DuelistGuardDirection.NONE;

		private VisualState(float xRot, float yRot, float zRot, float ageInTicks, int tickCount) {
			this.xRot = xRot;
			this.yRot = yRot;
			this.zRot = zRot;
			this.lastAgeInTicks = ageInTicks;
			this.lastSeenTick = tickCount;
		}

		private float nextAlpha(DuelistGuardDirection direction, float ageInTicks) {
			boolean changedTarget = direction != this.targetDirection;
			this.targetDirection = direction;

			float deltaTicks = Mth.clamp(ageInTicks - this.lastAgeInTicks, 0.0F, 1.0F);
			this.lastAgeInTicks = ageInTicks;

			float alpha = Mth.clamp(deltaTicks * ALPHA_PER_TICK, 0.0F, MAX_ALPHA);
			return changedTarget ? Math.max(alpha, MIN_ALPHA_ON_TARGET_CHANGE) : alpha;
		}

		private boolean isCloseTo(PoseAngles target) {
			return Math.abs(this.xRot - target.xRot()) < SNAP_EPSILON
					&& Math.abs(this.yRot - target.yRot()) < SNAP_EPSILON
					&& Math.abs(this.zRot - target.zRot()) < SNAP_EPSILON;
		}
	}
}
