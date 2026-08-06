package net.timothaty.timothatystrinkets.entity;

import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModEntities;

public class DebtlordGroundDebrisEntity extends Monster {
	private static final int DEFAULT_ANIMATION_DURATION = 14;

	private static final EntityDataAccessor<BlockState> BLOCK_STATE = SynchedEntityData.defineId(DebtlordGroundDebrisEntity.class, EntityDataSerializers.BLOCK_STATE);
	private static final EntityDataAccessor<Integer> START_DELAY = SynchedEntityData.defineId(DebtlordGroundDebrisEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> ANIMATION_DURATION = SynchedEntityData.defineId(DebtlordGroundDebrisEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> LIFT_HEIGHT = SynchedEntityData.defineId(DebtlordGroundDebrisEntity.class, EntityDataSerializers.FLOAT);

	public DebtlordGroundDebrisEntity(EntityType<DebtlordGroundDebrisEntity> type, Level level) {
		super(type, level);
		xpReward = 0;
		setNoAi(true);
		setSilent(true);
		setNoGravity(true);
		noPhysics = true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(BLOCK_STATE, Blocks.DIRT.defaultBlockState());
		builder.define(START_DELAY, 0);
		builder.define(ANIMATION_DURATION, DEFAULT_ANIMATION_DURATION);
		builder.define(LIFT_HEIGHT, 0.75F);
	}

	public void configure(BlockState blockState, int startDelay, int animationDuration, float liftHeight) {
		entityData.set(BLOCK_STATE, blockState);
		entityData.set(START_DELAY, Math.max(0, startDelay));
		entityData.set(ANIMATION_DURATION, Math.max(1, animationDuration));
		entityData.set(LIFT_HEIGHT, Math.max(0.0F, liftHeight));
	}

	public BlockState getDebrisBlockState() {
		return entityData.get(BLOCK_STATE);
	}

	public boolean isDebrisVisible(float partialTick) {
		return tickCount + partialTick >= entityData.get(START_DELAY);
	}

	public float getRenderYOffset(float partialTick) {
		float age = tickCount + partialTick - entityData.get(START_DELAY);
		float progress = Mth.clamp(age / entityData.get(ANIMATION_DURATION), 0.0F, 1.0F);
		return 4.0F * entityData.get(LIFT_HEIGHT) * progress * (1.0F - progress);
	}

	@Override
	protected void registerGoals() {
	}

	@Override
	public void tick() {
		super.tick();
		setDeltaMovement(Vec3.ZERO);
		setNoGravity(true);

		int lifetime = entityData.get(START_DELAY) + entityData.get(ANIMATION_DURATION);
		if (!level().isClientSide() && tickCount >= lifetime) {
			discard();
		}
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	protected void pushEntities() {
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean canBeAffected(MobEffectInstance effectInstance) {
		return false;
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return true;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.put("DebrisBlock", NbtUtils.writeBlockState(getDebrisBlockState()));
		tag.putInt("StartDelay", entityData.get(START_DELAY));
		tag.putInt("AnimationDuration", entityData.get(ANIMATION_DURATION));
		tag.putFloat("LiftHeight", entityData.get(LIFT_HEIGHT));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.contains("DebrisBlock")) {
			BlockState state = NbtUtils.readBlockState(level().holderLookup(Registries.BLOCK), tag.getCompound("DebrisBlock"));
			entityData.set(BLOCK_STATE, state);
		}
		entityData.set(START_DELAY, Math.max(0, tag.getInt("StartDelay")));
		entityData.set(ANIMATION_DURATION, Math.max(1, tag.getInt("AnimationDuration")));
		entityData.set(LIFT_HEIGHT, Math.max(0.0F, tag.getFloat("LiftHeight")));
	}

	public static DebtlordGroundDebrisEntity create(Level level, BlockPos groundPos, BlockState blockState, int startDelay, int animationDuration, float liftHeight) {
		DebtlordGroundDebrisEntity debris = new DebtlordGroundDebrisEntity(TimothatysTrinketsModEntities.DEBTLORD_GROUND_DEBRIS.get(), level);
		debris.configure(blockState, startDelay, animationDuration, liftHeight);
		debris.setPos(groundPos.getX() + 0.5D, groundPos.getY() + 1.0D, groundPos.getZ() + 0.5D);
		return debris;
	}

	public static void init(RegisterSpawnPlacementsEvent event) {
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.0D)
			.add(Attributes.MAX_HEALTH, 1.0D)
			.add(Attributes.ARMOR, 0.0D)
			.add(Attributes.ATTACK_DAMAGE, 0.0D)
			.add(Attributes.FOLLOW_RANGE, 0.0D)
			.add(Attributes.STEP_HEIGHT, 0.0D);
	}
}
