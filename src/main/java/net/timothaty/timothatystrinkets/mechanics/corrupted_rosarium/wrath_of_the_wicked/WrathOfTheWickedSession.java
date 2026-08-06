package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.wrath_of_the_wicked;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class WrathOfTheWickedSession {
	private final ResourceKey<Level> dimension;
	private final long startGameTime;
	private final int rosariumRevision;
	private final String sourceSlotIdentifier;
	private final int sourceSlotIndex;
	private final boolean venomSphereSynergy;
	private final Map<UUID, Integer> laserHitCounts = new HashMap<>();
	private final Set<UUID> pulseHits = new HashSet<>();
	private final List<FireWave> activeFireWaves = new ArrayList<>();

	private float initialYaw;
	private boolean rotationLocked;
	private boolean flamingEmberSynergy;
	private Vec3 decelerationVelocity;
	private Vec3 anchor;
	private Vec3 pulseOrigin;
	private boolean previousNoGravity;
	private boolean anchorApplied;
	private boolean movementReleased;
	private int nextLaserStage;
	private int nextFireWaveIndex;
	private long lastAuraTick = Long.MIN_VALUE;

	WrathOfTheWickedSession(
			ResourceKey<Level> dimension,
			long startGameTime,
			int rosariumRevision,
			String sourceSlotIdentifier,
			int sourceSlotIndex,
			boolean venomSphereSynergy
	) {
		this.dimension = dimension;
		this.startGameTime = startGameTime;
		this.rosariumRevision = rosariumRevision;
		this.sourceSlotIdentifier = sourceSlotIdentifier;
		this.sourceSlotIndex = sourceSlotIndex;
		this.venomSphereSynergy = venomSphereSynergy;
	}

	ResourceKey<Level> dimension() {
		return dimension;
	}

	long startGameTime() {
		return startGameTime;
	}

	float initialYaw() {
		return initialYaw;
	}

	boolean rotationLocked() {
		return rotationLocked;
	}

	void lockRotation(float yaw) {
		this.initialYaw = yaw;
		this.rotationLocked = true;
	}

	int rosariumRevision() {
		return rosariumRevision;
	}

	boolean matchesSourceSlot(String slotIdentifier, int slotIndex) {
		return sourceSlotIdentifier.equals(slotIdentifier) && sourceSlotIndex == slotIndex;
	}

	boolean flamingEmberSynergy() {
		return flamingEmberSynergy;
	}

	void enableFlamingEmberSynergy() {
		this.flamingEmberSynergy = true;
	}

	boolean venomSphereSynergy() {
		return venomSphereSynergy;
	}

	int laserHitCount(LivingEntity target) {
		return target == null ? 0 : laserHitCounts.getOrDefault(target.getUUID(), 0);
	}

	void recordLaserHit(LivingEntity target) {
		if (target != null)
			laserHitCounts.merge(target.getUUID(), 1, Integer::sum);
	}

	Set<UUID> pulseHits() {
		return pulseHits;
	}

	Vec3 decelerationVelocity() {
		return decelerationVelocity;
	}

	void setDecelerationVelocity(Vec3 velocity) {
		this.decelerationVelocity = velocity;
	}

	Vec3 anchor() {
		return anchor;
	}

	void applyAnchor(Vec3 position, boolean oldNoGravity) {
		this.anchor = position;
		this.previousNoGravity = oldNoGravity;
		this.anchorApplied = true;
	}

	boolean previousNoGravity() {
		return previousNoGravity;
	}

	boolean anchorApplied() {
		return anchorApplied;
	}

	void clearAnchor() {
		this.anchor = null;
		this.anchorApplied = false;
	}

	Vec3 pulseOrigin() {
		return pulseOrigin;
	}

	void setPulseOrigin(Vec3 pulseOrigin) {
		this.pulseOrigin = pulseOrigin;
	}

	boolean movementReleased() {
		return movementReleased;
	}

	void markMovementReleased() {
		this.movementReleased = true;
	}

	int nextLaserStage() {
		return nextLaserStage;
	}

	void advanceLaserStage() {
		this.nextLaserStage++;
	}

	int nextFireWaveIndex() {
		return nextFireWaveIndex;
	}

	void advanceFireWave() {
		this.nextFireWaveIndex++;
	}

	FireWave startFireWave(int startElapsedTick, Vec3 origin) {
		FireWave wave = new FireWave(startElapsedTick, origin);
		activeFireWaves.add(wave);
		return wave;
	}

	List<FireWave> activeFireWaves() {
		return activeFireWaves;
	}

	long lastAuraTick() {
		return lastAuraTick;
	}

	void setLastAuraTick(long gameTime) {
		this.lastAuraTick = gameTime;
	}

	static final class FireWave {
		private final int startElapsedTick;
		private final Vec3 origin;
		private final Set<UUID> hits = new HashSet<>();
		private double processedRadius;

		private FireWave(int startElapsedTick, Vec3 origin) {
			this.startElapsedTick = startElapsedTick;
			this.origin = origin;
		}

		int startElapsedTick() {
			return startElapsedTick;
		}

		Vec3 origin() {
			return origin;
		}

		boolean hasHit(LivingEntity target) {
			return target != null && hits.contains(target.getUUID());
		}

		void markHit(LivingEntity target) {
			if (target != null)
				hits.add(target.getUUID());
		}

		double processedRadius() {
			return processedRadius;
		}

		void setProcessedRadius(double processedRadius) {
			this.processedRadius = processedRadius;
		}
	}
}
