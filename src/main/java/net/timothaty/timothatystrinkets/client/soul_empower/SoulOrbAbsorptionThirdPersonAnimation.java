package net.timothaty.timothatystrinkets.client.soul_empower;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.timothaty.timothatystrinkets.client.DuelistGuardClient;
import net.timothaty.timothatystrinkets.client.stunned.StunnedClientAnimationState;
import net.timothaty.timothatystrinkets.mechanics.pagans_charm.PaganCharmMeditationPlayerState;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class SoulOrbAbsorptionThirdPersonAnimation {
	private static final float TARGET_X_ROTATION = -85.0F * Mth.DEG_TO_RAD;
	private static final float TARGET_INWARD_Y_ROTATION = -11.0F * Mth.DEG_TO_RAD;
	private static final float TARGET_INWARD_Z_ROTATION = -8.0F * Mth.DEG_TO_RAD;
	private static final float BASE_ALPHA_PER_TICK = 0.42F;
	private static final float MAX_BASE_ALPHA = 0.30F;
	private static final float MIN_ALPHA_ON_STATE_CHANGE = 0.13F;
	private static final float PULL_ALPHA_PER_TICK = 0.48F;
	private static final float MAX_PULL_ALPHA = 0.38F;
	private static final float SHAKE_X_ROTATION = 1.25F * Mth.DEG_TO_RAD;
	private static final float SHAKE_Y_ROTATION = 0.55F * Mth.DEG_TO_RAD;
	private static final float SHAKE_Z_ROTATION = 0.90F * Mth.DEG_TO_RAD;
	private static final float SNAP_EPSILON = 0.012F;
	private static final int MAX_IDLE_AGE_TICKS = 80;

	private static final Map<Integer, VisualState> STATES = new HashMap<>();

	private SoulOrbAbsorptionThirdPersonAnimation() {
	}

	public static void apply(LivingEntity entity, float ageInTicks, ModelPart rightArm, ModelPart leftArm) {
		if (!(entity instanceof Player) || rightArm == null || leftArm == null) {
			return;
		}
		if (!entity.isAlive() || entity.isDeadOrDying() || entity.isRemoved()) {
			removeEntity(entity.getId());
			return;
		}
		if (hasHigherPriorityPose(entity, ageInTicks)) {
			removeEntity(entity.getId());
			return;
		}

		boolean pulling = SoulOrbAbsorptionVisualState.isPulling(entity);
		HumanoidArm serverArm = SoulOrbAbsorptionVisualState.getPullingArm(entity);
		VisualState state = STATES.get(entity.getId());
		if (state == null) {
			if (!pulling || serverArm == null) {
				return;
			}
			ModelPart armPart = armPart(serverArm, rightArm, leftArm);
			state = new VisualState(serverArm, armPart.xRot, armPart.yRot, armPart.zRot, ageInTicks, entity.level().getGameTime());
			STATES.put(entity.getId(), state);
		} else if (pulling && serverArm != null && serverArm != state.arm) {
			ModelPart armPart = armPart(serverArm, rightArm, leftArm);
			state = new VisualState(serverArm, armPart.xRot, armPart.yRot, armPart.zRot, ageInTicks, entity.level().getGameTime());
			STATES.put(entity.getId(), state);
		}

		ModelPart animatedArm = armPart(state.arm, rightArm, leftArm);
		PoseAngles target = pulling ? absorptionPose(state.arm) : new PoseAngles(animatedArm.xRot, animatedArm.yRot, animatedArm.zRot);
		boolean stateChanged = pulling != state.wasPulling;
		float deltaTicks = state.deltaTicks(ageInTicks);
		float alpha = Mth.clamp(deltaTicks * BASE_ALPHA_PER_TICK, 0.0F, MAX_BASE_ALPHA);
		if (stateChanged) {
			alpha = Math.max(alpha, MIN_ALPHA_ON_STATE_CHANGE);
		}

		state.xRot = Mth.lerp(alpha, state.xRot, target.xRot());
		state.yRot = Mth.lerp(alpha, state.yRot, target.yRot());
		state.zRot = Mth.lerp(alpha, state.zRot, target.zRot());
		float pullAlpha = Mth.clamp(deltaTicks * PULL_ALPHA_PER_TICK, 0.0F, MAX_PULL_ALPHA);
		if (stateChanged) {
			pullAlpha = Math.max(pullAlpha, MIN_ALPHA_ON_STATE_CHANGE);
		}
		state.pullProgress = Mth.lerp(pullAlpha, state.pullProgress, pulling ? 1.0F : 0.0F);
		state.wasPulling = pulling;
		state.lastSeenTick = entity.level().getGameTime();

		float fast = Mth.sin(ageInTicks * 2.7F);
		float detail = Mth.sin(ageInTicks * 5.1F + 0.8F);
		float vertical = Mth.sin(ageInTicks * 3.6F + 1.4F);
		float shake = (fast * 0.7F + detail * 0.3F) * state.pullProgress;
		float secondaryShake = (vertical * 0.75F + detail * 0.25F) * state.pullProgress;
		animatedArm.xRot = state.xRot + SHAKE_X_ROTATION * shake;
		animatedArm.yRot = state.yRot + SHAKE_Y_ROTATION * secondaryShake;
		animatedArm.zRot = state.zRot + SHAKE_Z_ROTATION * shake;

		if (!pulling && state.pullProgress < SNAP_EPSILON && state.isCloseTo(target)) {
			STATES.remove(entity.getId());
		}
	}

	public static void removeEntity(int entityId) {
		STATES.remove(entityId);
	}

	public static void cleanStaleStates(long currentTick) {
		Iterator<Map.Entry<Integer, VisualState>> iterator = STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			if (currentTick - iterator.next().getValue().lastSeenTick > MAX_IDLE_AGE_TICKS) {
				iterator.remove();
			}
		}
	}

	public static void clear() {
		STATES.clear();
	}

	private static boolean hasHigherPriorityPose(LivingEntity entity, float ageInTicks) {
		if (StunnedClientAnimationState.isStunned(entity)
				|| DuelistGuardClient.getThirdPersonGuardDirection(entity).canBeHeldByPlayer()) {
			return true;
		}
		return entity instanceof PaganCharmMeditationPlayerState meditationState
				&& meditationState.timothatys_trinkets$getPaganCharmMeditationPhase(ageInTicks)
				!= PaganCharmMeditationPlayerState.PHASE_NONE;
	}

	private static ModelPart armPart(HumanoidArm arm, ModelPart rightArm, ModelPart leftArm) {
		return arm == HumanoidArm.RIGHT ? rightArm : leftArm;
	}

	private static PoseAngles absorptionPose(HumanoidArm arm) {
		float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
		return new PoseAngles(TARGET_X_ROTATION, side * TARGET_INWARD_Y_ROTATION, side * TARGET_INWARD_Z_ROTATION);
	}

	private record PoseAngles(float xRot, float yRot, float zRot) {
	}

	private static final class VisualState {
		private final HumanoidArm arm;
		private float xRot;
		private float yRot;
		private float zRot;
		private float pullProgress;
		private float lastAgeInTicks;
		private long lastSeenTick;
		private boolean wasPulling;

		private VisualState(HumanoidArm arm, float xRot, float yRot, float zRot, float ageInTicks, long tickCount) {
			this.arm = arm;
			this.xRot = xRot;
			this.yRot = yRot;
			this.zRot = zRot;
			this.lastAgeInTicks = ageInTicks;
			this.lastSeenTick = tickCount;
		}

		private float deltaTicks(float ageInTicks) {
			float delta = Mth.clamp(ageInTicks - this.lastAgeInTicks, 0.0F, 1.0F);
			this.lastAgeInTicks = ageInTicks;
			return delta;
		}

		private boolean isCloseTo(PoseAngles target) {
			return Math.abs(this.xRot - target.xRot()) < SNAP_EPSILON
					&& Math.abs(this.yRot - target.yRot()) < SNAP_EPSILON
					&& Math.abs(this.zRot - target.zRot()) < SNAP_EPSILON;
		}
	}
}
