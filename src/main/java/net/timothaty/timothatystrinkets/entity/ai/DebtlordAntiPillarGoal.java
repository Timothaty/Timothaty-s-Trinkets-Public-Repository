package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.DebtlordEntity;
import net.timothaty.timothatystrinkets.entity.DebtlordPhase;
import net.timothaty.timothatystrinkets.mechanics.debtlord.DebtlordTelekineticHold;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class DebtlordAntiPillarGoal extends Goal {
	public static final int PHASE_NONE = 0;
	public static final int PHASE_FADE_OUT = 1;
	public static final int PHASE_REPOSITION = 2;
	public static final int PHASE_FADE_IN = 3;
	public static final int PHASE_FIRST_CLAW = 4;
	public static final int PHASE_LAUNCH = 5;
	public static final int PHASE_HOLD = 6;
	public static final int PHASE_SECOND_CLAW = 7;
	public static final int PHASE_RECOVERY = 8;

	public static final int FADE_OUT_TICKS = 10;
	public static final int FADE_IN_TICKS = 9;
	private static final int CHAIN_MISS_CONFIRMATION_TICKS = DebtlordChainsGoal.FAILED_DURATION_TICKS;
	private static final int LAUNCH_FLIGHT_TICKS = 5;
	private static final int HOLD_PREPARATION_TICKS = 10;
	private static final int RECOVERY_TICKS = 8;
	private static final int FULL_COOLDOWN_TICKS = 500;
	private static final int PHASE_ONE_RECOVERY_COOLDOWN_TICKS = 80;
	private static final int PHASE_ONE_FOLLOWUP_WINDOW_TICKS = 60;
	private static final double MIN_PILLAR_HEIGHT = 3.0D;
	private static final double MIN_FALLING_VELOCITY = -0.10D;
	private static final double STABLE_PATH_MAX_FEET_DELTA = 0.55D;
	private static final double STABLE_PATH_MAX_ATTACK_GAP = DebtlordHornsGoal.TRIGGER_RANGE;
	private static final double STABLE_PATH_MAX_ATTACK_GAP_SQR = STABLE_PATH_MAX_ATTACK_GAP * STABLE_PATH_MAX_ATTACK_GAP;
	private static final double LAUNCH_HORIZONTAL_SPEED = 1.45D;
	private static final double LAUNCH_VERTICAL_SPEED = 0.85D;

	private final DebtlordEntity debtlord;
	private final DebtlordClawGoal clawGoal;
	private LivingEntity pendingTarget;
	private DebtlordChainsGoal.CaptureResult pendingMissReason = DebtlordChainsGoal.CaptureResult.INVALID_TARGET;
	private LivingEntity target;
	private long confirmationNotBeforeGameTime;
	private long nextFullPunishmentGameTime;
	private long nextPhaseOneRecoveryGameTime;
	private boolean pathEvaluated;
	private int phase = PHASE_NONE;
	private int phaseTicks;
	private Vec3 holdAnchor;
	private boolean holdActive;
	private boolean targetHadNoGravity;

	public DebtlordAntiPillarGoal(DebtlordEntity debtlord, DebtlordClawGoal clawGoal) {
		this.debtlord = debtlord;
		this.clawGoal = clawGoal;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
	}

	public void reportFailedChainResolution(LivingEntity missedTarget, DebtlordChainsGoal.CaptureResult missReason) {
		if (!isAntiPillarFailure(missReason)
			|| !(missedTarget instanceof Player)
			|| !isValidTarget(missedTarget)
			|| getFeetDelta(missedTarget) < MIN_PILLAR_HEIGHT)
			return;
		long gameTime = debtlord.level().getGameTime();
		if (debtlord.getPhase() == DebtlordPhase.PHASE_ONE) {
			if (gameTime < nextPhaseOneRecoveryGameTime)
				return;
		} else if (gameTime < nextFullPunishmentGameTime) {
			return;
		}
		pendingTarget = missedTarget;
		pendingMissReason = missReason;
		confirmationNotBeforeGameTime = gameTime + CHAIN_MISS_CONFIRMATION_TICKS;
		pathEvaluated = false;
	}

	@Override
	public boolean canUse() {
		if (pendingTarget == null || debtlord.level().getGameTime() < confirmationNotBeforeGameTime)
			return false;
		if (!isPendingTargetValid(pendingTarget)) {
			pendingTarget = null;
			return false;
		}
		if (debtlord.isUsingAbility())
			return false;
		if (pendingMissReason == DebtlordChainsGoal.CaptureResult.NO_LINE_OF_SIGHT && debtlord.hasLineOfSight(pendingTarget)) {
			pendingTarget = null;
			return false;
		}
		if (!pathEvaluated) {
			pathEvaluated = true;
			PathNavigation navigation = debtlord.getNavigation();
			Path path = navigation.createPath(pendingTarget, 0);
			if (endsAtStableAttackPosition(path, pendingTarget)) {
				pendingTarget = null;
				return false;
			}
		}

		long gameTime = debtlord.level().getGameTime();
		return debtlord.getPhase() == DebtlordPhase.PHASE_ONE
			? gameTime >= nextPhaseOneRecoveryGameTime
			: gameTime >= nextFullPunishmentGameTime;
	}

	@Override
	public boolean canContinueToUse() {
		return phase != PHASE_NONE && isComboTargetValid();
	}

	@Override
	public void start() {
		target = pendingTarget;
		pendingTarget = null;
		pendingMissReason = DebtlordChainsGoal.CaptureResult.INVALID_TARGET;
		pathEvaluated = false;
		if (debtlord.getPhase() == DebtlordPhase.PHASE_ONE) {
			debtlord.offerClawFollowup(target, 0, PHASE_ONE_FOLLOWUP_WINDOW_TICKS, DebtlordClawFollowupQueue.Reason.PILLAR_RECOVERY);
			nextPhaseOneRecoveryGameTime = debtlord.level().getGameTime() + PHASE_ONE_RECOVERY_COOLDOWN_TICKS;
			target = null;
			return;
		}

		nextFullPunishmentGameTime = debtlord.level().getGameTime() + FULL_COOLDOWN_TICKS;
		debtlord.clearClawFollowup();
		debtlord.beginAntiPillarAbility(PHASE_FADE_OUT, FADE_OUT_TICKS);
		debtlord.setNoGravity(true);
		phase = PHASE_FADE_OUT;
		phaseTicks = FADE_OUT_TICKS;
	}

	@Override
	public void tick() {
		if (!isComboTargetValid()) {
			cancelCombo();
			return;
		}

		switch (phase) {
			case PHASE_FADE_OUT -> tickFadeOut();
			case PHASE_FADE_IN -> tickFadeIn();
			case PHASE_FIRST_CLAW -> tickFirstClaw();
			case PHASE_LAUNCH -> tickLaunchFlight();
			case PHASE_HOLD -> tickHoldPreparation();
			case PHASE_SECOND_CLAW -> tickSecondClaw();
			case PHASE_RECOVERY -> tickRecovery();
			default -> cancelCombo();
		}
	}

	@Override
	public void stop() {
		cancelCombo();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	public void forceCancel() {
		pendingTarget = null;
		pendingMissReason = DebtlordChainsGoal.CaptureResult.INVALID_TARGET;
		pathEvaluated = false;
		cancelCombo();
	}

	public void clearPendingRequest() {
		pendingTarget = null;
		pendingMissReason = DebtlordChainsGoal.CaptureResult.INVALID_TARGET;
		pathEvaluated = false;
	}

	private void tickFadeOut() {
		debtlord.faceAbilityTargetInPlace(target);
		if (--phaseTicks > 0) {
			debtlord.setAntiPillarVisualState(PHASE_FADE_OUT, phaseTicks);
			return;
		}

		debtlord.setAntiPillarVisualState(PHASE_REPOSITION, 0);
		debtlord.noPhysics = true;
		Vec3 position = DebtlordPillarRepositioning.findPosition(debtlord, target);
		if (position == null) {
			cancelCombo();
			return;
		}
		debtlord.setPos(position.x, position.y, position.z);
		debtlord.setDeltaMovement(Vec3.ZERO);
		transition(PHASE_FADE_IN, FADE_IN_TICKS);
	}

	private void tickFadeIn() {
		debtlord.faceAbilityTargetInPlace(target);
		if (--phaseTicks > 0) {
			debtlord.setAntiPillarVisualState(PHASE_FADE_IN, phaseTicks);
			return;
		}

		debtlord.noPhysics = false;
		if (!clawGoal.beginControlledSingleCast(target)) {
			cancelCombo();
			return;
		}
		transition(PHASE_FIRST_CLAW, 0);
	}

	private void tickFirstClaw() {
		DebtlordClawGoal.ControlledCastStatus status = clawGoal.tickControlledSingleCast();
		if (status == DebtlordClawGoal.ControlledCastStatus.PRIMARY_FINAL_IMPACT) {
			clawGoal.cancelControlledSingleCast();
			launchPrimaryTarget();
			transition(PHASE_LAUNCH, LAUNCH_FLIGHT_TICKS);
		} else if (status == DebtlordClawGoal.ControlledCastStatus.FINISHED
			|| status == DebtlordClawGoal.ControlledCastStatus.CANCELLED) {
			cancelCombo();
		}
	}

	private void tickLaunchFlight() {
		debtlord.faceAbilityTargetInPlace(target);
		if (--phaseTicks > 0) {
			debtlord.setAntiPillarVisualState(PHASE_LAUNCH, phaseTicks);
			return;
		}

		holdAnchor = target.position();
		startHold();
		DebtlordTelekineticHold.tick(target, holdAnchor);
		debtlord.noPhysics = true;
		Vec3 position = DebtlordPillarRepositioning.findPosition(debtlord, target);
		if (position == null) {
			cancelCombo();
			return;
		}
		debtlord.setPos(position.x, position.y, position.z);
		debtlord.noPhysics = false;
		if (!clawGoal.beginControlledSingleCast(target, 2)) {
			cancelCombo();
			return;
		}
		transition(PHASE_HOLD, HOLD_PREPARATION_TICKS);
	}

	private void tickHoldPreparation() {
		DebtlordTelekineticHold.tick(target, holdAnchor);
		DebtlordClawGoal.ControlledCastStatus status = clawGoal.tickControlledSingleCast();
		if (status == DebtlordClawGoal.ControlledCastStatus.PRIMARY_FINAL_IMPACT
			|| status == DebtlordClawGoal.ControlledCastStatus.FINISHED) {
			finishSecondClaw();
			return;
		}
		if (status == DebtlordClawGoal.ControlledCastStatus.CANCELLED) {
			cancelCombo();
			return;
		}
		if (--phaseTicks > 0) {
			debtlord.setAntiPillarVisualState(PHASE_HOLD, phaseTicks);
			return;
		}
		transition(PHASE_SECOND_CLAW, 0);
	}

	private void tickSecondClaw() {
		DebtlordTelekineticHold.tick(target, holdAnchor);
		DebtlordClawGoal.ControlledCastStatus status = clawGoal.tickControlledSingleCast();
		if (status == DebtlordClawGoal.ControlledCastStatus.PRIMARY_FINAL_IMPACT
			|| status == DebtlordClawGoal.ControlledCastStatus.FINISHED) {
			finishSecondClaw();
		} else if (status == DebtlordClawGoal.ControlledCastStatus.CANCELLED) {
			cancelCombo();
		}
	}

	private void finishSecondClaw() {
		clawGoal.cancelControlledSingleCast();
		endHold();
		holdAnchor = null;
		transition(PHASE_RECOVERY, RECOVERY_TICKS);
	}

	private void tickRecovery() {
		debtlord.faceAbilityTargetInPlace(target);
		if (--phaseTicks > 0) {
			debtlord.setAntiPillarVisualState(PHASE_RECOVERY, phaseTicks);
			return;
		}
		finishCombo();
	}

	private void launchPrimaryTarget() {
		Vec3 direction = target.position().subtract(debtlord.position());
		double horizontalLengthSqr = direction.x * direction.x + direction.z * direction.z;
		double x = 0.0D;
		double z = 0.0D;
		if (horizontalLengthSqr > 1.0E-6D) {
			double inverseLength = 1.0D / Math.sqrt(horizontalLengthSqr);
			x = direction.x * inverseLength * LAUNCH_HORIZONTAL_SPEED;
			z = direction.z * inverseLength * LAUNCH_HORIZONTAL_SPEED;
		}
		target.fallDistance = 0.0F;
		target.setDeltaMovement(x, LAUNCH_VERTICAL_SPEED, z);
		target.hurtMarked = true;
		target.hasImpulse = true;
		debtlord.noPhysics = true;
	}

	private void transition(int nextPhase, int ticks) {
		phase = nextPhase;
		phaseTicks = ticks;
		debtlord.setAntiPillarVisualState(nextPhase, ticks);
	}

	private boolean isPendingTargetValid(LivingEntity candidate) {
		return isValidTarget(candidate)
			&& debtlord.getTarget() == candidate
			&& !debtlord.isAltarIntroOrDismissalActive()
			&& getFeetDelta(candidate) >= MIN_PILLAR_HEIGHT
			&& candidate.getDeltaMovement().y > MIN_FALLING_VELOCITY;
	}

	private boolean isComboTargetValid() {
		return phase != PHASE_NONE
			&& isValidTarget(target)
			&& debtlord.getTarget() == target
			&& !debtlord.isAltarIntroOrDismissalActive();
	}

	private boolean isValidTarget(LivingEntity candidate) {
		return debtlord.isAlive()
			&& candidate != null
			&& candidate != debtlord
			&& candidate.isAlive()
			&& !candidate.isRemoved()
			&& candidate.level() == debtlord.level()
			&& !debtlord.isAlliedTo(candidate)
			&& (!(candidate instanceof Player player) || (!player.isCreative() && !player.isSpectator()))
			&& !TimothatysTrinketsStunHelper.isMechanicallyImmunePlayer(candidate);
	}

	private double getFeetDelta(LivingEntity candidate) {
		return candidate.getBoundingBox().minY - debtlord.getBoundingBox().minY;
	}

	private boolean endsAtStableAttackPosition(Path path, LivingEntity candidate) {
		if (path == null || !path.canReach())
			return false;
		Node endNode = path.getEndNode();
		if (endNode == null)
			return false;

		double endpointX = endNode.x + 0.5D;
		double endpointZ = endNode.z + 0.5D;
		AABB targetBounds = candidate.getBoundingBox();
		double gapX = Math.max(Math.max(targetBounds.minX - endpointX, 0.0D), endpointX - targetBounds.maxX);
		double gapZ = Math.max(Math.max(targetBounds.minZ - endpointZ, 0.0D), endpointZ - targetBounds.maxZ);
		double endpointFeetDelta = Math.abs(targetBounds.minY - endNode.y);
		return gapX * gapX + gapZ * gapZ <= STABLE_PATH_MAX_ATTACK_GAP_SQR
			&& endpointFeetDelta <= STABLE_PATH_MAX_FEET_DELTA;
	}

	private static boolean isAntiPillarFailure(DebtlordChainsGoal.CaptureResult result) {
		return result == DebtlordChainsGoal.CaptureResult.NO_LINE_OF_SIGHT
			|| result == DebtlordChainsGoal.CaptureResult.BLOCKED_CHAIN_PATH
			|| result == DebtlordChainsGoal.CaptureResult.OUTSIDE_CORRIDOR
			|| result == DebtlordChainsGoal.CaptureResult.INEFFECTIVE_PULL;
	}

	private void finishCombo() {
		restoreTransientState();
		phase = PHASE_NONE;
		phaseTicks = 0;
		target = null;
	}

	private void cancelCombo() {
		if (phase == PHASE_NONE) {
			target = null;
			return;
		}
		restoreTransientState();
		phase = PHASE_NONE;
		phaseTicks = 0;
		target = null;
	}

	private void restoreTransientState() {
		clawGoal.cancelControlledSingleCast();
		endHold();
		holdAnchor = null;
		debtlord.noPhysics = false;
		debtlord.setNoGravity(false);
		debtlord.fallDistance = 0.0F;
		debtlord.finishAntiPillarAbility();
		debtlord.getNavigation().stop();
	}

	private void startHold() {
		targetHadNoGravity = target.isNoGravity();
		target.setNoGravity(true);
		holdActive = true;
		DebtlordTelekineticHold.start(target);
	}

	private void endHold() {
		if (!holdActive)
			return;
		if (target != null)
			target.setNoGravity(targetHadNoGravity);
		DebtlordTelekineticHold.end(target);
		holdActive = false;
		targetHadNoGravity = false;
	}
}
