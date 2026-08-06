package net.timothaty.timothatystrinkets.entity;

import net.neoforged.neoforge.fluids.FluidType;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractItemManifestationEntity extends Entity {
	private static final EntityDataAccessor<Integer> AGE_SNAPSHOT = SynchedEntityData.defineId(AbstractItemManifestationEntity.class, EntityDataSerializers.INT);
	private int manifestationAge;
	private boolean rewardClaimed;
	private boolean collecting;
	private ItemStack displayStack = ItemStack.EMPTY;
	private int clientAgeSyncTick;

	protected AbstractItemManifestationEntity(EntityType<? extends AbstractItemManifestationEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	protected abstract ItemStack createRewardStack();

	protected int getPickupDelayTicks() {
		return 25;
	}

	protected int getManifestationLifetimeTicks() {
		return 12 * 20;
	}

	protected void onCollected(Player player) {
	}

	protected void onExpired(ItemEntity fallback) {
	}

	public final ItemStack getDisplayStack() {
		if (this.displayStack.isEmpty()) this.displayStack = this.createRewardStack();
		return this.displayStack;
	}

	public final int getManifestationAge() {
		return this.level().isClientSide()
				? this.entityData.get(AGE_SNAPSHOT) + Math.max(0, this.tickCount - this.clientAgeSyncTick)
				: this.manifestationAge;
	}

	public final int getConfiguredLifetime() {
		return getManifestationLifetimeTicks();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(AGE_SNAPSHOT, 0);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (this.level().isClientSide() && key.equals(AGE_SNAPSHOT)) this.clientAgeSyncTick = this.tickCount;
	}

	@Override
	public void tick() {
		super.tick();
		this.noPhysics = true;
		this.setNoGravity(true);
		this.setDeltaMovement(Vec3.ZERO);
		if (this.level().isClientSide()) return;
		if (this.rewardClaimed) {
			this.discard();
			return;
		}
		if (++this.manifestationAge >= getManifestationLifetimeTicks()) expireToItem();
	}

	@Override
	public void playerTouch(Player player) {
		if (!this.level().isClientSide() && this.manifestationAge >= getPickupDelayTicks()) tryCollect(player);
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		if (this.level().isClientSide()) return InteractionResult.SUCCESS;
		if (this.manifestationAge >= getPickupDelayTicks()) tryCollect(player);
		return InteractionResult.CONSUME;
	}

	public final boolean tryCollect(Player player) {
		if (this.level().isClientSide() || this.rewardClaimed || this.collecting || this.isRemoved()) return false;
		this.collecting = true;
		ItemStack reward = this.createRewardStack();
		if (reward.isEmpty()) {
			this.rewardClaimed = true;
			this.discard();
			return false;
		}

		this.rewardClaimed = true;
		ItemStack remainder = reward.copy();
		player.addItem(remainder);
		if (!remainder.isEmpty()) spawnFallback(remainder);
		onCollected(player);
		this.discard();
		return true;
	}

	private void expireToItem() {
		if (this.rewardClaimed || this.isRemoved()) return;
		this.rewardClaimed = true;
		ItemStack reward = this.createRewardStack();
		ItemEntity fallback = reward.isEmpty() ? null : spawnFallback(reward);
		if (fallback != null) onExpired(fallback);
		this.discard();
	}

	private ItemEntity spawnFallback(ItemStack stack) {
		ItemEntity item = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), stack);
		item.setDefaultPickUpDelay();
		return this.level().addFreshEntity(item) ? item : null;
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
		return true;
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
	public BlockPos getBlockPosBelowThatAffectsMyMovement() {
		return this.blockPosition();
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("ManifestationAge", this.manifestationAge);
		tag.putBoolean("RewardClaimed", this.rewardClaimed);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		this.manifestationAge = Math.max(0, tag.getInt("ManifestationAge"));
		this.rewardClaimed = tag.getBoolean("RewardClaimed");
		this.collecting = false;
		this.displayStack = ItemStack.EMPTY;
		this.entityData.set(AGE_SNAPSHOT, this.manifestationAge);
	}
}
