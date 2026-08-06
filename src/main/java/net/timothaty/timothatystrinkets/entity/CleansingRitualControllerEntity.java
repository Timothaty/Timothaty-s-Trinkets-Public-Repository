package net.timothaty.timothatystrinkets.entity;

import net.neoforged.neoforge.fluids.FluidType;

import net.timothaty.timothatystrinkets.block.IncenseTrailBlock;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModEntities;
import net.timothaty.timothatystrinkets.mechanics.cleansing.ritual.CleansingRitualPattern;
import net.timothaty.timothatystrinkets.mechanics.cleansing.ritual.CleansingRitualService;
import net.timothaty.timothatystrinkets.mechanics.cleansing.ritual.CleansingRitualSounds;
import net.timothaty.timothatystrinkets.mechanics.cleansing.ritual.CleansingRitualValidator;
import net.timothaty.timothatystrinkets.mechanics.cleansing.ritual.CleansingRitualVisualBridge;
import net.timothaty.timothatystrinkets.particle.BabahParticleOptions;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

import java.util.UUID;

public final class CleansingRitualControllerEntity extends Entity {
	public static final int BURN_STEP_TICKS = 5;
	public static final int CONSECRATION_TICKS = 60;
	private static final int COMPLETE_VISUAL_TICKS = 2;
	private static final EntityDataAccessor<Integer> PHASE_DATA = SynchedEntityData.defineId(CleansingRitualControllerEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> START_ROUTE_INDEX_DATA = SynchedEntityData.defineId(CleansingRitualControllerEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> COMPLETED_STEPS_DATA = SynchedEntityData.defineId(CleansingRitualControllerEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> PHASE_AGE_SNAPSHOT_DATA = SynchedEntityData.defineId(CleansingRitualControllerEntity.class, EntityDataSerializers.INT);

	private BlockPos center = BlockPos.ZERO;
	private UUID initiatorUuid;
	private int startRouteIndex;
	private int completedSteps;
	private Phase phase = Phase.BURNING;
	private int phaseAge;
	private boolean consumed;
	private boolean finalized;

	private int clientStepSyncTick;
	private int clientPhaseSyncTick;
	private int clientObservedCompletedSteps;
	private boolean clientFinalBurstShown;

	public CleansingRitualControllerEntity(EntityType<? extends CleansingRitualControllerEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	public void configure(BlockPos center, UUID initiatorUuid, int startRouteIndex) {
		this.center = center.immutable();
		this.initiatorUuid = initiatorUuid;
		this.startRouteIndex = Math.floorMod(startRouteIndex, CleansingRitualPattern.INCENSE_COUNT);
		this.completedSteps = 0;
		this.phase = Phase.BURNING;
		this.phaseAge = 0;
		this.consumed = true;
		this.finalized = false;
		this.setPos(center.getX() + 0.5D, center.getY() + 0.08D, center.getZ() + 0.5D);
		this.entityData.set(START_ROUTE_INDEX_DATA, this.startRouteIndex);
		this.entityData.set(COMPLETED_STEPS_DATA, 0);
		this.entityData.set(PHASE_DATA, Phase.BURNING.id);
		this.entityData.set(PHASE_AGE_SNAPSHOT_DATA, 0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(PHASE_DATA, Phase.BURNING.id);
		builder.define(START_ROUTE_INDEX_DATA, 0);
		builder.define(COMPLETED_STEPS_DATA, 0);
		builder.define(PHASE_AGE_SNAPSHOT_DATA, 0);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (!this.level().isClientSide()) return;
		if (key.equals(COMPLETED_STEPS_DATA)) this.clientStepSyncTick = this.tickCount;
		if (key.equals(PHASE_DATA) || key.equals(PHASE_AGE_SNAPSHOT_DATA)) this.clientPhaseSyncTick = this.tickCount;
	}

	@Override
	public void tick() {
		super.tick();
		this.noPhysics = true;
		this.setNoGravity(true);
		this.setDeltaMovement(Vec3.ZERO);

		if (this.level().isClientSide()) {
			CleansingRitualVisualBridge.tickClient(this);
			return;
		}
		if (!(this.level() instanceof ServerLevel serverLevel) || !this.consumed) {
			this.discard();
			return;
		}
		if (this.phase == Phase.COMPLETE) {
			if (++this.phaseAge >= COMPLETE_VISUAL_TICKS) this.discard();
			return;
		}
		if (!CleansingRitualValidator.allRequiredChunksLoaded(serverLevel, this.center)) {
			return;
		}

		this.phaseAge++;
		if (this.phase == Phase.BURNING) tickBurning(serverLevel);
		else tickConsecration(serverLevel);
	}

	private void tickBurning(ServerLevel level) {
		if (this.phaseAge < BURN_STEP_TICKS) return;
		this.phaseAge = 0;
		int routeIndex = CleansingRitualPattern.wrappedRouteIndex(this.startRouteIndex, this.completedSteps);
		if (!CleansingRitualValidator.validateBurningBase(level, this.center)
				|| !CleansingRitualValidator.validateFreshSegment(level, this.center, routeIndex)) {
			abortRitual(level);
			return;
		}

		BlockPos offset = CleansingRitualPattern.CLOCKWISE_ROUTE.get(routeIndex);
		BlockPos trailPos = this.center.offset(offset.getX(), 0, offset.getZ());
		BlockState state = level.getBlockState(trailPos);
		level.setBlock(trailPos, state.setValue(IncenseTrailBlock.ASH, true), Block.UPDATE_CLIENTS);
		if ((this.completedSteps & 3) == 1) CleansingRitualSounds.smolder(level, trailPos);

		this.completedSteps++;
		this.entityData.set(COMPLETED_STEPS_DATA, this.completedSteps);
		if (this.completedSteps < CleansingRitualPattern.INCENSE_COUNT) return;
		if (!CleansingRitualValidator.validateCompletedPattern(level, this.center)) {
			abortRitual(level);
			return;
		}
		setPhase(Phase.CONSECRATION);
		CleansingRitualSounds.consecrationPulse(level, this.center, 0);
	}

	private void tickConsecration(ServerLevel level) {
		if (this.phaseAge % 5 == 0 && !CleansingRitualValidator.validateBurningBase(level, this.center)) {
			abortRitual(level);
			return;
		}
		if (this.phaseAge == 20) CleansingRitualSounds.consecrationPulse(level, this.center, 1);
		if (this.phaseAge == 40) CleansingRitualSounds.consecrationPulse(level, this.center, 2);
		if (this.phaseAge < CONSECRATION_TICKS) return;
		finishRitual(level);
	}

	private void finishRitual(ServerLevel level) {
		if (this.finalized || !CleansingRitualValidator.validateCompletedPattern(level, this.center)
				|| !CleansingRitualService.isOnlyActiveController(level, this.center, this)
				|| hasExistingManifestation(level)) {
			abortRitual(level);
			return;
		}

		this.finalized = true;
		setPhase(Phase.COMPLETE);
		for (BlockPos offset : CleansingRitualPattern.CANDLE_OFFSETS) {
			BlockPos candlePos = this.center.offset(offset.getX(), 0, offset.getZ());
			BlockState candle = level.getBlockState(candlePos);
			level.setBlock(candlePos, candle.setValue(AbstractCandleBlock.LIT, false), Block.UPDATE_ALL);
		}
		level.removeBlock(this.center, false);

		level.sendParticles(new BabahParticleOptions(new Vector3f(1.0F, 0.24F, 0.66F), 0.72F),
				this.center.getX() + 0.5D, this.center.getY() + 0.82D, this.center.getZ() + 0.5D,
				1, 0.0D, 0.0D, 0.0D, 0.0D);
		CleansingRitualSounds.finalFlash(level, this.center);

		CleansingDustManifestationEntity manifestation = new CleansingDustManifestationEntity(
				TimothatysTrinketsModEntities.CLEANSING_DUST_MANIFESTATION.get(), level);
		manifestation.setPos(this.center.getX() + 0.5D, this.center.getY() + 0.72D, this.center.getZ() + 0.5D);
		if (level.addFreshEntity(manifestation)) CleansingRitualSounds.manifestation(level, this.center);
	}

	private boolean hasExistingManifestation(ServerLevel level) {
		AABB bounds = new AABB(this.center).inflate(2.0D);
		return !level.getEntitiesOfClass(CleansingDustManifestationEntity.class, bounds, entity -> !entity.isRemoved()).isEmpty();
	}

	private void abortRitual(ServerLevel level) {
		CleansingRitualSounds.abort(level, this.center);
		this.discard();
	}

	private void setPhase(Phase phase) {
		this.phase = phase;
		this.phaseAge = 0;
		this.entityData.set(PHASE_DATA, phase.id);
		this.entityData.set(PHASE_AGE_SNAPSHOT_DATA, 0);
	}

	public BlockPos getCenter() {
		return this.level().isClientSide() ? this.blockPosition() : this.center;
	}

	public int getStartRouteIndex() {
		return this.entityData.get(START_ROUTE_INDEX_DATA);
	}

	public int getCompletedSteps() {
		return this.entityData.get(COMPLETED_STEPS_DATA);
	}

	public Phase getPhase() {
		return Phase.byId(this.entityData.get(PHASE_DATA));
	}

	public float getClientStepProgress(float partialTick) {
		return Mth.clamp((this.tickCount + partialTick - this.clientStepSyncTick) / BURN_STEP_TICKS, 0.0F, 1.0F);
	}

	public float getClientPhaseAge(float partialTick) {
		return this.entityData.get(PHASE_AGE_SNAPSHOT_DATA) + Math.max(0.0F, this.tickCount + partialTick - this.clientPhaseSyncTick);
	}

	public boolean markClientFinalBurstShown() {
		if (this.clientFinalBurstShown) return false;
		this.clientFinalBurstShown = true;
		return true;
	}

	public boolean consumeClientStepChange() {
		int steps = getCompletedSteps();
		if (steps == this.clientObservedCompletedSteps) return false;
		this.clientObservedCompletedSteps = steps;
		return steps > 0 && steps <= CleansingRitualPattern.INCENSE_COUNT;
	}

	public boolean isActiveAt(BlockPos center) {
		return !this.isRemoved() && this.consumed && this.phase != Phase.COMPLETE && this.center.equals(center);
	}

	@Override
	protected MovementEmission getMovementEmission() {
		return MovementEmission.NONE;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.0D;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public void push(Entity entity) {
	}

	@Override
	public boolean canCollideWith(Entity entity) {
		return false;
	}

	@Override
	public void playerTouch(Player player) {
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		return InteractionResult.PASS;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return true;
	}

	@Override
	public boolean isPushedByFluid(FluidType type) {
		return false;
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE;
	}

	@Override
	public boolean isOnFire() {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putLong("Center", this.center.asLong());
		if (this.initiatorUuid != null) tag.putUUID("Initiator", this.initiatorUuid);
		tag.putInt("StartRouteIndex", this.startRouteIndex);
		tag.putInt("CompletedSteps", this.completedSteps);
		tag.putInt("Phase", this.phase.id);
		tag.putInt("PhaseAge", this.phaseAge);
		tag.putBoolean("Consumed", this.consumed);
		tag.putBoolean("Finalized", this.finalized);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		this.center = BlockPos.of(tag.getLong("Center"));
		this.initiatorUuid = tag.hasUUID("Initiator") ? tag.getUUID("Initiator") : null;
		this.startRouteIndex = Math.floorMod(tag.getInt("StartRouteIndex"), CleansingRitualPattern.INCENSE_COUNT);
		this.completedSteps = Mth.clamp(tag.getInt("CompletedSteps"), 0, CleansingRitualPattern.INCENSE_COUNT);
		this.phase = Phase.byId(tag.getInt("Phase"));
		this.phaseAge = Math.max(0, tag.getInt("PhaseAge"));
		this.consumed = tag.getBoolean("Consumed");
		this.finalized = tag.getBoolean("Finalized");
		this.setPos(this.center.getX() + 0.5D, this.center.getY() + 0.08D, this.center.getZ() + 0.5D);
		this.entityData.set(START_ROUTE_INDEX_DATA, this.startRouteIndex);
		this.entityData.set(COMPLETED_STEPS_DATA, this.completedSteps);
		this.entityData.set(PHASE_DATA, this.phase.id);
		this.entityData.set(PHASE_AGE_SNAPSHOT_DATA, this.phaseAge);
	}

	public enum Phase {
		BURNING(0), CONSECRATION(1), COMPLETE(2);

		private final int id;

		Phase(int id) {
			this.id = id;
		}

		private static Phase byId(int id) {
			Phase[] values = values();
			return values[Mth.clamp(id, 0, values.length - 1)];
		}
	}
}
