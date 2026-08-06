package net.timothaty.timothatystrinkets.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

final class UndeadKnightSoulAbsorption {
	static final int PHASE_NONE = 0;
	static final int PHASE_START = 1;
	static final int PHASE_LOOP = 2;
	static final int PHASE_END = 3;
	static final int LOOP_ANIMATION_TICKS = 30;
	static final int MIN_ORBS_TO_CAST = 3;
	static final int MAX_ABSORBED_ORBS = 15;
	static final double RANGE = 6.0D;
	static final double RANGE_SQR = RANGE * RANGE;

	private static final int START_DURATION_TICKS = 10;
	private static final int START_STEP_SOUND_TICK = 5;
	private static final int LOOP_DURATION_TICKS = 120;
	private static final int LOOP_SOUND_INTERVAL_TICKS = 24;
	private static final int END_DURATION_TICKS = 15;
	private static final int IDLE_SCAN_INTERVAL_TICKS = 80;
	private static final int ACTIVE_ORB_SCAN_INTERVAL_TICKS = 5;
	private static final double PARTIAL_ORB_RELEASE_SPEED = 0.08D;
	private static final double MAX_HEALTH_PER_ORB = 2.0D;
	private static final double MODEL_SCALE_PER_ORB = 0.015D;
	private static final double MOVEMENT_SPEED_PENALTY_PER_ORB = -0.01D;
	private static final float REGEN_PER_ORB = 0.25F;
	private static final DustParticleOptions SOUL_DUST = new DustParticleOptions(new Vector3f(0.0F, 1.0F, 0.6549F), 1.0F);
	private static final double MODEL_UNIT = 1.0D / 16.0D;
	private static final double MODEL_ROOT_Y = 24.0D;
	private static final Vec3 LEFT_HAND_TIP_MODEL_POS = new Vec3(2.3D, 12.0D, 0.0D);
	private static final Vec3 LEFT_ARM_MODEL_OFFSET = new Vec3(3.7D, -12.0D, 0.0D);
	private static final Vec3 ALL_BODY_MODEL_OFFSET = new Vec3(0.0D, 12.0D, 0.0D);
	private static final ResourceLocation MAX_HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "undead_knight_soul_absorption_max_health");
	private static final ResourceLocation MOVEMENT_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "undead_knight_soul_absorption_movement_speed");
	private static final ResourceLocation SCALE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "undead_knight_soul_absorption_scale");

	private static final double[] START_WAIST_X_TIMES = {0.0D, 0.125D, 0.5D};
	private static final double[] START_WAIST_X_VALUES = {0.0D, 2.5D, -2.5D};
	private static final double[] LOOP_WAIST_X_TIMES = {0.0D, 0.5D, 1.0D, 1.5D};
	private static final double[] LOOP_WAIST_X_VALUES = {-2.5D, 10.0D, -12.5D, -2.5D};
	private static final double[] END_WAIST_X_TIMES = {0.0D, 0.75D};
	private static final double[] END_WAIST_X_VALUES = {-2.5D, 0.0D};
	private static final double[] START_LEFT_ARM_X_TIMES = {0.0D, 0.125D, 0.25D, 0.5D};
	private static final double[] START_LEFT_ARM_X_VALUES = {0.0D, -40.0D, -120.0D, -165.0D};
	private static final double[] LOOP_LEFT_ARM_X_TIMES = {0.0D, 0.5D, 1.0D, 1.5D};
	private static final double[] LOOP_LEFT_ARM_X_VALUES = {-165.0D, -110.0D, -150.0D, -165.0D};
	private static final double[] END_LEFT_ARM_X_TIMES = {0.0D, 0.75D};
	private static final double[] END_LEFT_ARM_X_VALUES = {-165.0D, 0.0D};
	private static final double[] START_WAIST_Z_TIMES = {0.0D, 0.125D, 0.25D, 0.5D};
	private static final double[] START_WAIST_Z_VALUES = {0.0D, -1.0D, -3.0D, -3.0D};
	private static final double[] LOOP_WAIST_Z_TIMES = {0.0D, 0.5D, 1.5D};
	private static final double[] LOOP_WAIST_Z_VALUES = {-3.0D, -4.0D, -3.0D};
	private static final double[] END_WAIST_Z_TIMES = {0.0D, 0.75D};
	private static final double[] END_WAIST_Z_VALUES = {-3.0D, 0.0D};

	private final UndeadKnightEntity knight;
	private final Set<Integer> trackedOrbIds = new HashSet<>();
	private int clientPhase = PHASE_NONE;
	private int clientTicks;
	private float lockedYRot;
	private float lockedYHeadRot;
	private float lockedXRot;

	UndeadKnightSoulAbsorption(UndeadKnightEntity knight) {
		this.knight = knight;
	}

	boolean isActive() {
		return this.getPhase() != PHASE_NONE;
	}

	boolean isStarting() {
		return this.getPhase() == PHASE_START;
	}

	boolean isLooping() {
		return this.getPhase() == PHASE_LOOP;
	}

	boolean isEnding() {
		return this.getPhase() == PHASE_END;
	}

	boolean canStart() {
		return !this.knight.level().isClientSide()
				&& this.knight.canAbsorbSouls()
				&& !this.isActive()
				&& !this.knight.isBlocking()
				&& !this.knight.isEmpowering()
				&& !this.knight.isReincarnating()
				&& !this.knight.isDeathAnimationActive()
				&& !this.knight.isNoAi()
				&& this.knight.getAbsorbedSoulOrbs() < MAX_ABSORBED_ORBS
				&& !TimothatysTrinketsStunHelper.isStunned(this.knight)
				&& !TimothatysTrinketsStunHelper.isStaggered(this.knight);
	}

	boolean start() {
		if (!this.canStart() || !this.hasEnoughAvailableSoulOrbsInRange()) {
			return false;
		}

		return this.startAfterOrbCheck();
	}

	private boolean startAfterOrbCheck() {
		this.knight.clearBlockState();
		this.knight.clearEmpowerState();
		this.lockedYRot = this.knight.getYRot();
		this.lockedYHeadRot = this.knight.yHeadRot;
		this.lockedXRot = this.knight.getXRot();
		this.trackedOrbIds.clear();
		this.knight.setSoulAbsorptionPhaseData(PHASE_START);
		this.knight.setSoulAbsorptionTicksData(0);
		this.lockControl();
		return true;
	}

	void clear(boolean releaseOrbs) {
		if (releaseOrbs) {
			this.releaseTrackedSoulOrbs();
		} else {
			this.trackedOrbIds.clear();
		}
		this.knight.setSoulAbsorptionPhaseData(PHASE_NONE);
		this.knight.setSoulAbsorptionTicksData(0);
	}

	void tickIdleScan() {
		if (!this.canStart()) {
			return;
		}
		if ((this.knight.tickCount + this.knight.getId()) % IDLE_SCAN_INTERVAL_TICKS != 0) {
			return;
		}
		if (this.hasEnoughAvailableSoulOrbsInRange()) {
			this.startAfterOrbCheck();
		}
	}

	void tickActive() {
		if (!this.isActive() || this.knight.level().isClientSide()) {
			return;
		}
		if (!this.knight.canAbsorbSouls() || TimothatysTrinketsStunHelper.isStunned(this.knight) || TimothatysTrinketsStunHelper.isStaggered(this.knight) || this.knight.isDeathAnimationActive()) {
			this.clear(true);
			return;
		}

		this.lockControl();
		this.spawnHandParticles();
		int ticks = this.advanceTicks();
		if (this.isStarting()) {
			this.tickStart(ticks);
		} else if (this.isLooping()) {
			this.tickLoop(ticks);
		} else if (this.isEnding()) {
			this.tickEnd(ticks);
		}
	}

	void tickPassiveRegeneration() {
		int absorbedOrbs = this.knight.getAbsorbedSoulOrbs();
		if (!this.knight.canAbsorbSouls() || absorbedOrbs <= 0 || this.knight.tickCount % 20 != 0 || this.knight.getHealth() >= this.knight.getMaxHealth()) {
			return;
		}

		this.knight.heal(absorbedOrbs * REGEN_PER_ORB);
	}

	void setAbsorbedOrbs(int absorbedOrbs) {
		this.knight.setAbsorbedSoulOrbsData(this.knight.canAbsorbSouls() ? Mth.clamp(absorbedOrbs, 0, MAX_ABSORBED_ORBS) : 0);
		this.updateAttributeModifiers();
		this.knight.refreshDimensions();
	}

	void updateClientAnimation() {
		int phase = this.getPhase();
		int ticks = this.getTicks();
		boolean phaseChanged = phase != this.clientPhase;
		boolean ticksRewound = phase != PHASE_NONE && ticks < this.clientTicks;
		boolean restartPhaseAnimation = phaseChanged || ticksRewound;
		if (restartPhaseAnimation) {
			this.stopClientAnimations();
			this.clientPhase = phase;
		}
		this.clientTicks = ticks;

		if (phase == PHASE_START) {
			this.startAnimation(this.knight.soulAbsorptionStartAnimationState, restartPhaseAnimation, this.knight.tickCount - ticks);
			this.knight.soulAbsorptionLoopAnimationState.stop();
			this.knight.soulAbsorptionEndAnimationState.stop();
		} else if (phase == PHASE_LOOP) {
			this.knight.soulAbsorptionStartAnimationState.stop();
			this.startAnimation(this.knight.soulAbsorptionLoopAnimationState, restartPhaseAnimation, this.knight.tickCount - (ticks % LOOP_ANIMATION_TICKS));
			this.knight.soulAbsorptionEndAnimationState.stop();
		} else if (phase == PHASE_END) {
			this.knight.soulAbsorptionStartAnimationState.stop();
			this.knight.soulAbsorptionLoopAnimationState.stop();
			this.startAnimation(this.knight.soulAbsorptionEndAnimationState, restartPhaseAnimation, this.knight.tickCount - ticks);
		} else {
			this.stopClientAnimations();
		}
	}

	void stopClientAnimations() {
		this.knight.soulAbsorptionStartAnimationState.stop();
		this.knight.soulAbsorptionLoopAnimationState.stop();
		this.knight.soulAbsorptionEndAnimationState.stop();
		this.clientPhase = PHASE_NONE;
		this.clientTicks = 0;
	}

	void clearTravelInput() {
		this.knight.xxa = 0.0F;
		this.knight.yya = 0.0F;
		this.knight.zza = 0.0F;
	}

	void freezeHorizontalMotion() {
		Vec3 motion = this.knight.getDeltaMovement();
		this.knight.setDeltaMovement(0.0D, Math.min(0.0D, motion.y), 0.0D);
		this.knight.hurtMarked = true;
	}

	private int getPhase() {
		return this.knight.getSoulAbsorptionPhase();
	}

	private int getTicks() {
		return this.knight.getSoulAbsorptionTicks();
	}

	private void startAnimation(AnimationState animationState, boolean forceStart, int startTick) {
		if (forceStart) {
			animationState.start(startTick);
		} else {
			animationState.startIfStopped(startTick);
		}
	}

	private int advanceTicks() {
		int ticks = this.getTicks() + 1;
		this.knight.setSoulAbsorptionTicksData(ticks);
		return ticks;
	}

	private void tickStart(int ticks) {
		if (ticks == START_STEP_SOUND_TICK) {
			this.knight.level().playSound(null, this.knight.blockPosition(), TimothatysTrinketsModSounds.UNDEAD_KNIGHT_STEP.get(), SoundSource.HOSTILE, 0.8F, 1.0F + this.knight.getRandom().nextFloat() * 0.2F);
		}
		if (ticks >= START_DURATION_TICKS) {
			this.setPhase(PHASE_LOOP);
		}
	}

	private void tickLoop(int ticks) {
		if (this.shouldScanForNewOrbs()) {
			this.trackSoulOrbsInRange();
		}
		this.pullTrackedSoulOrbs();
		if (!this.isActive()) {
			return;
		}
		if (ticks == 1 || (ticks - 1) % LOOP_SOUND_INTERVAL_TICKS == 0) {
			Vec3 hand = this.getLeftHandTipPosition();
			this.knight.level().playSound(null, hand.x, hand.y, hand.z, TimothatysTrinketsModSounds.SOUL_ABSORPTION_CAST_LOOP.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
		}
		if (ticks >= LOOP_DURATION_TICKS || this.knight.getAbsorbedSoulOrbs() >= MAX_ABSORBED_ORBS) {
			this.setPhase(PHASE_END);
		}
	}

	private void tickEnd(int ticks) {
		if (ticks >= END_DURATION_TICKS) {
			this.clear(true);
		}
	}

	private void setPhase(int phase) {
		this.knight.setSoulAbsorptionPhaseData(phase);
		this.knight.setSoulAbsorptionTicksData(0);
		if (phase != PHASE_LOOP) {
			this.releaseTrackedSoulOrbs();
		}
	}

	private void releaseTrackedSoulOrbs() {
		Iterator<Integer> iterator = this.trackedOrbIds.iterator();
		while (iterator.hasNext()) {
			Entity entity = this.knight.level().getEntity(iterator.next());
			if (entity instanceof SoulOrbEntity orb && orb.isSoulAbsorbedBy(this.knight)) {
				orb.clearSoulAbsorptionTarget();
			}
			iterator.remove();
		}
	}

	private boolean hasEnoughAvailableSoulOrbsInRange() {
		int foundLogicalUnits = 0;
		for (SoulOrbEntity orb : this.knight.level().getEntitiesOfClass(SoulOrbEntity.class, this.getSearchBox(), this::canClaimSoulOrb)) {
			foundLogicalUnits += Math.min(orb.getSoulValue(), MIN_ORBS_TO_CAST - foundLogicalUnits);
			if (foundLogicalUnits >= MIN_ORBS_TO_CAST) {
				return true;
			}
		}
		return false;
	}

	private boolean shouldScanForNewOrbs() {
		if (!this.isLooping() || this.getTicks() % ACTIVE_ORB_SCAN_INTERVAL_TICKS != 1) {
			return false;
		}

		int remainingCapacity = MAX_ABSORBED_ORBS - this.knight.getAbsorbedSoulOrbs();
		return remainingCapacity > 0 && this.getTrackedLogicalSoulUnits(remainingCapacity) < remainingCapacity;
	}

	private void trackSoulOrbsInRange() {
		int remainingCapacity = MAX_ABSORBED_ORBS - this.knight.getAbsorbedSoulOrbs();
		if (remainingCapacity <= 0) {
			return;
		}

		int logicalUnitsNeeded = remainingCapacity - this.getTrackedLogicalSoulUnits(remainingCapacity);
		if (logicalUnitsNeeded <= 0) {
			return;
		}

		List<SoulOrbEntity> orbs = this.knight.level().getEntitiesOfClass(SoulOrbEntity.class, this.getSearchBox(),
				orb -> orb != null
						&& !orb.isRemoved()
						&& orb.hasSoulValue()
						&& orb.distanceToSqr(this.knight) <= RANGE_SQR
						&& (orb.isAvailableForSoulAbsorption() || orb.isSoulAbsorbedBy(this.knight)));
		for (SoulOrbEntity orb : orbs) {
			if (logicalUnitsNeeded <= 0) {
				return;
			}
			if (this.trackSoulOrb(orb)) {
				logicalUnitsNeeded -= Math.min(orb.getSoulValue(), logicalUnitsNeeded);
			}
		}
	}

	private boolean trackSoulOrb(SoulOrbEntity orb) {
		if (orb == null || orb.isRemoved() || !this.isLooping()) {
			return false;
		}
		if (!orb.isSoulAbsorbedBy(this.knight)) {
			if (!this.canClaimSoulOrb(orb)) {
				return false;
			}
			orb.setSoulAbsorptionTarget(this.knight, 0.75F + this.knight.getRandom().nextFloat() * 0.55F);
		}
		return this.trackedOrbIds.add(orb.getId());
	}

	private int getTrackedLogicalSoulUnits(int limit) {
		int logicalUnits = 0;
		Iterator<Integer> iterator = this.trackedOrbIds.iterator();
		while (iterator.hasNext()) {
			Entity entity = this.knight.level().getEntity(iterator.next());
			if (!(entity instanceof SoulOrbEntity orb) || orb.isRemoved() || !orb.hasSoulValue() || !orb.isSoulAbsorbedBy(this.knight)) {
				iterator.remove();
				continue;
			}

			logicalUnits += Math.min(orb.getSoulValue(), limit - logicalUnits);
			if (logicalUnits >= limit) {
				return limit;
			}
		}
		return logicalUnits;
	}

	private void pullTrackedSoulOrbs() {
		Vec3 hand = this.getLeftHandTipPosition();
		Iterator<Integer> iterator = this.trackedOrbIds.iterator();
		while (iterator.hasNext()) {
			if (this.knight.getAbsorbedSoulOrbs() >= MAX_ABSORBED_ORBS) {
				return;
			}

			Entity entity = this.knight.level().getEntity(iterator.next());
			if (!(entity instanceof SoulOrbEntity orb) || orb.isRemoved() || !orb.hasSoulValue() || !orb.isSoulAbsorbedBy(this.knight)) {
				iterator.remove();
				continue;
			}

			if (SoulOrbPullPhysics.pullOrReached(orb, hand)) {
				iterator.remove();
				this.collectSoulOrb(orb, hand);
			}
		}
	}

	private void collectSoulOrb(SoulOrbEntity orb, Vec3 hand) {
		if (!orb.consumeOneSoulUnit()) {
			if (orb.isSoulAbsorbedBy(this.knight)) {
				orb.clearSoulAbsorptionTarget();
			}
			if (!orb.hasSoulValue()) {
				orb.discard();
			}
			return;
		}

		this.knight.level().playSound(null, hand.x, hand.y, hand.z, TimothatysTrinketsModSounds.SOUL_COLLECT.get(), SoundSource.HOSTILE, 0.9F, 1.0F + this.knight.getRandom().nextFloat() * 0.2F);
		this.addAbsorbedSoulOrb();
		orb.clearSoulAbsorptionTarget();
		if (orb.hasSoulValue()) {
			this.releasePartiallyConsumedOrb(orb, hand);
		} else {
			orb.discard();
		}
	}

	private void releasePartiallyConsumedOrb(SoulOrbEntity orb, Vec3 hand) {
		double dx = orb.getX() - hand.x;
		double dy = orb.getY() + orb.getBbHeight() * 0.5D - hand.y;
		double dz = orb.getZ() - hand.z;
		double distanceSqr = dx * dx + dy * dy + dz * dz;
		if (distanceSqr < 1.0E-6D) {
			double angle = this.knight.getRandom().nextDouble() * Math.PI * 2.0D;
			dx = Math.cos(angle);
			dy = 0.25D;
			dz = Math.sin(angle);
			distanceSqr = dx * dx + dy * dy + dz * dz;
		}

		double scale = PARTIAL_ORB_RELEASE_SPEED / Math.sqrt(distanceSqr);
		orb.setDeltaMovement(dx * scale, Math.max(0.025D, dy * scale), dz * scale);
		orb.hurtMarked = true;
		orb.hasImpulse = true;
	}

	private boolean canClaimSoulOrb(SoulOrbEntity orb) {
		return orb != null
				&& !orb.isRemoved()
				&& orb.hasSoulValue()
				&& orb.isAvailableForSoulAbsorption()
				&& orb.distanceToSqr(this.knight) <= RANGE_SQR;
	}

	private AABB getSearchBox() {
		return this.knight.getBoundingBox().inflate(RANGE);
	}

	private void lockControl() {
		this.knight.getNavigation().stop();
		this.knight.setAggressive(false);
		this.clearTravelInput();
		this.freezeHorizontalMotion();
		this.knight.setYRot(this.lockedYRot);
		this.knight.yRotO = this.lockedYRot;
		this.knight.yBodyRot = this.lockedYRot;
		this.knight.yBodyRotO = this.lockedYRot;
		this.knight.yHeadRot = this.lockedYHeadRot;
		this.knight.yHeadRotO = this.lockedYHeadRot;
		this.knight.setXRot(this.lockedXRot);
		this.knight.xRotO = this.lockedXRot;
	}

	private Vec3 getLeftHandTipPosition() {
		Vec3 forward = Vec3.directionFromRotation(0.0F, this.lockedYRot);
		Vec3 flatForward = new Vec3(forward.x, 0.0D, forward.z);
		if (flatForward.lengthSqr() < 1.0E-4D) {
			flatForward = this.knight.getLookAngle();
			flatForward = new Vec3(flatForward.x, 0.0D, flatForward.z);
		}
		if (flatForward.lengthSqr() < 1.0E-4D) {
			flatForward = new Vec3(0.0D, 0.0D, 1.0D);
		} else {
			flatForward = flatForward.normalize();
		}

		Vec3 left = new Vec3(flatForward.z, 0.0D, -flatForward.x).normalize();
		double scale = this.getBodyScale();
		Vec3 modelHand = this.getLeftHandTipModelPosition();
		double sideOffset = modelHand.x * MODEL_UNIT * scale;
		double heightOffset = (MODEL_ROOT_Y - modelHand.y) * MODEL_UNIT * scale;
		double forwardOffset = -modelHand.z * MODEL_UNIT * scale;

		return this.knight.position()
				.add(left.scale(sideOffset))
				.add(flatForward.scale(forwardOffset))
				.add(0.0D, heightOffset, 0.0D);
	}

	private Vec3 getLeftHandTipModelPosition() {
		double waistXRot = this.getWaistXRotRadians();
		double leftArmXRot = this.getLeftArmXRotRadians();
		double waistZ = this.getWaistZOffsetModelUnits();
		Vec3 handFromArmPivot = rotateModelX(LEFT_HAND_TIP_MODEL_POS, leftArmXRot);
		Vec3 handFromWaist = LEFT_ARM_MODEL_OFFSET.add(handFromArmPivot);
		Vec3 waistAnimated = rotateModelX(handFromWaist, waistXRot).add(0.0D, 0.0D, waistZ);
		return ALL_BODY_MODEL_OFFSET.add(waistAnimated);
	}

	private double getWaistXRotRadians() {
		double seconds = this.getAnimationSeconds();
		double degrees;
		if (this.isStarting()) {
			degrees = interpolateKeyframes(seconds, START_WAIST_X_TIMES, START_WAIST_X_VALUES);
		} else if (this.isLooping()) {
			degrees = interpolateKeyframes(seconds, LOOP_WAIST_X_TIMES, LOOP_WAIST_X_VALUES);
		} else if (this.isEnding()) {
			degrees = interpolateKeyframes(seconds, END_WAIST_X_TIMES, END_WAIST_X_VALUES);
		} else {
			degrees = 0.0D;
		}
		return Math.toRadians(degrees);
	}

	private double getLeftArmXRotRadians() {
		double seconds = this.getAnimationSeconds();
		double degrees;
		if (this.isStarting()) {
			degrees = interpolateKeyframes(seconds, START_LEFT_ARM_X_TIMES, START_LEFT_ARM_X_VALUES);
		} else if (this.isLooping()) {
			degrees = interpolateKeyframes(seconds, LOOP_LEFT_ARM_X_TIMES, LOOP_LEFT_ARM_X_VALUES);
		} else if (this.isEnding()) {
			degrees = interpolateKeyframes(seconds, END_LEFT_ARM_X_TIMES, END_LEFT_ARM_X_VALUES);
		} else {
			degrees = 0.0D;
		}
		return Math.toRadians(degrees);
	}

	private double getWaistZOffsetModelUnits() {
		double seconds = this.getAnimationSeconds();
		if (this.isStarting()) {
			return interpolateKeyframes(seconds, START_WAIST_Z_TIMES, START_WAIST_Z_VALUES);
		}
		if (this.isLooping()) {
			return interpolateKeyframes(seconds, LOOP_WAIST_Z_TIMES, LOOP_WAIST_Z_VALUES);
		}
		if (this.isEnding()) {
			return interpolateKeyframes(seconds, END_WAIST_Z_TIMES, END_WAIST_Z_VALUES);
		}
		return 0.0D;
	}

	private double getAnimationSeconds() {
		if (this.isLooping()) {
			return (this.getTicks() % LOOP_ANIMATION_TICKS) / 20.0D;
		}
		return this.getTicks() / 20.0D;
	}

	private static Vec3 rotateModelX(Vec3 vector, double radians) {
		double cos = Math.cos(radians);
		double sin = Math.sin(radians);
		double y = vector.y * cos - vector.z * sin;
		double z = vector.y * sin + vector.z * cos;
		return new Vec3(vector.x, y, z);
	}

	private static double interpolateKeyframes(double time, double[] keyTimes, double[] values) {
		if (time <= keyTimes[0]) {
			return values[0];
		}
		for (int i = 1; i < keyTimes.length; i++) {
			if (time <= keyTimes[i]) {
				double progress = (time - keyTimes[i - 1]) / (keyTimes[i] - keyTimes[i - 1]);
				return Mth.lerp(Mth.clamp(progress, 0.0D, 1.0D), values[i - 1], values[i]);
			}
		}
		return values[values.length - 1];
	}

	private double getBodyScale() {
		return 1.0D + this.knight.getAbsorbedSoulOrbs() * MODEL_SCALE_PER_ORB;
	}

	private void spawnHandParticles() {
		if (!(this.knight.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		Vec3 hand = this.getLeftHandTipPosition();
		serverLevel.sendParticles(SOUL_DUST, hand.x, hand.y, hand.z, 3, 0.035D, 0.035D, 0.035D, 0.004D);
	}

	private void addAbsorbedSoulOrb() {
		int current = this.knight.getAbsorbedSoulOrbs();
		if (current >= MAX_ABSORBED_ORBS) {
			return;
		}

		this.knight.setAbsorbedSoulOrbsData(current + 1);
		this.updateAttributeModifiers();
		this.knight.refreshDimensions();
	}

	private void updateAttributeModifiers() {
		int absorbedOrbs = this.knight.getAbsorbedSoulOrbs();
		boolean hasAbsorbedOrbs = absorbedOrbs > 0;
		this.updateAttributeModifier(Attributes.MAX_HEALTH, MAX_HEALTH_MODIFIER_ID,
				absorbedOrbs * MAX_HEALTH_PER_ORB, AttributeModifier.Operation.ADD_VALUE, hasAbsorbedOrbs);
		this.updateAttributeModifier(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED_MODIFIER_ID,
				absorbedOrbs * MOVEMENT_SPEED_PENALTY_PER_ORB, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, hasAbsorbedOrbs);
		this.updateAttributeModifier(Attributes.SCALE, SCALE_MODIFIER_ID,
				absorbedOrbs * MODEL_SCALE_PER_ORB, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, hasAbsorbedOrbs);
	}

	private void updateAttributeModifier(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, ResourceLocation modifierId, double amount,
			AttributeModifier.Operation operation, boolean shouldHaveModifier) {
		AttributeInstance attributeInstance = this.knight.getAttribute(attribute);
		if (attributeInstance == null) {
			return;
		}

		attributeInstance.removeModifier(modifierId);
		if (shouldHaveModifier) {
			attributeInstance.addTransientModifier(new AttributeModifier(modifierId, amount, operation));
		}
	}
}
