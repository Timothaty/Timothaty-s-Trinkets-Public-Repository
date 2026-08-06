package net.timothaty.timothatystrinkets.entity;

import net.neoforged.neoforge.fluids.FluidType;

import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.beatific_pallium.BeatificPalliumData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;

public final class BeatificPalliumEntity extends Entity {
	private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(BeatificPalliumEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> VISUAL_PHASE = SynchedEntityData.defineId(BeatificPalliumEntity.class, EntityDataSerializers.INT);

	public final AnimationState appearanceAnimationState = new AnimationState();
	public final AnimationState loopAnimationState = new AnimationState();
	public final AnimationState fadeAnimationState = new AnimationState();
	public final AnimationState burstAnimationState = new AnimationState();
	private VisualPhase animationPhase;
	private int visualPhaseClientStartTick;

	public BeatificPalliumEntity(EntityType<BeatificPalliumEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(TARGET_ID, 0);
		builder.define(VISUAL_PHASE, VisualPhase.APPEARING.id());
	}

	public void configure(LivingEntity target) {
		this.entityData.set(TARGET_ID, target.getId());
		this.setVisualPhase(VisualPhase.APPEARING);
		followTarget(target);
		this.setOldPosAndRot();
	}

	public int getTargetId() {
		return this.entityData.get(TARGET_ID);
	}

	public LivingEntity getTarget() {
		Entity entity = this.level().getEntity(this.getTargetId());
		return entity instanceof LivingEntity living ? living : null;
	}

	public VisualPhase getVisualPhase() {
		return VisualPhase.byId(this.entityData.get(VISUAL_PHASE));
	}

	public void setVisualPhase(VisualPhase phase) {
		if (phase != null)
			this.entityData.set(VISUAL_PHASE, phase.id());
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (this.level().isClientSide() && VISUAL_PHASE.equals(key))
			updateAnimationState();
	}

	@Override
	public void tick() {
		super.tick();
		this.noPhysics = true;
		this.setNoGravity(true);
		this.setDeltaMovement(Vec3.ZERO);

		LivingEntity target = this.getTarget();
		if (target != null && target.isAlive() && !target.isRemoved()) {
			followTarget(target);
		} else if (!this.level().isClientSide()) {
			this.discard();
		}

		if (this.level().isClientSide())
			updateAnimationState();
	}

	private void followTarget(LivingEntity target) {
		this.setPos(
				target.getX(),
				target.getY() + target.getBbHeight() * BeatificPalliumData.VISUAL_CENTER_HEIGHT_FACTOR,
				target.getZ()
		);
		this.setYRot(target.yBodyRot);
		this.setXRot(0.0F);
	}

	public void ensureAnimationStateInitialized() {
		if (this.animationPhase == null)
			updateAnimationState();
	}

	private void updateAnimationState() {
		VisualPhase phase = this.getVisualPhase();
		if (phase == this.animationPhase)
			return;

		this.appearanceAnimationState.stop();
		this.loopAnimationState.stop();
		this.fadeAnimationState.stop();
		this.burstAnimationState.stop();
		this.animationPhase = phase;
		this.visualPhaseClientStartTick = this.tickCount;
		switch (phase) {
			case APPEARING -> this.appearanceAnimationState.start(this.tickCount);
			case LOOP -> this.loopAnimationState.start(this.tickCount);
			case FADING -> this.fadeAnimationState.start(this.tickCount);
			case BURST -> this.burstAnimationState.start(this.tickCount);
		}
	}

	public float getVisualPhaseAge(float partialTick) {
		return Math.max(0.0F, this.tickCount + partialTick - this.visualPhaseClientStartTick);
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
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
	}

	public enum VisualPhase {
		APPEARING(0),
		LOOP(1),
		FADING(2),
		BURST(3);

		private final int id;

		VisualPhase(int id) {
			this.id = id;
		}

		public int id() {
			return id;
		}

		public static VisualPhase byId(int id) {
			VisualPhase[] values = values();
			return values[Mth.clamp(id, 0, values.length - 1)];
		}
	}
}
