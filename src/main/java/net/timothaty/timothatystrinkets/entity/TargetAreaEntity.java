package net.timothaty.timothatystrinkets.entity;

import net.neoforged.neoforge.fluids.FluidType;

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
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModEntities;

public class TargetAreaEntity extends Entity {
	private static final float MIN_SIZE = 0.05F;
	private static final float DEFAULT_RADIUS = 1.0F;
	private static final float DEFAULT_HALF_SIZE = 0.5F;
	private static final float DEFAULT_HEIGHT = 0.75F;
	private static final int DEFAULT_DURATION = 40;
	private static final int DEFAULT_COLOR = 0xFFFFFF;

	private static final EntityDataAccessor<Integer> SHAPE_TYPE = SynchedEntityData.defineId(TargetAreaEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(TargetAreaEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> HALF_WIDTH = SynchedEntityData.defineId(TargetAreaEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> HALF_DEPTH = SynchedEntityData.defineId(TargetAreaEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(TargetAreaEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(TargetAreaEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(TargetAreaEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> FADE_IN_TICKS = SynchedEntityData.defineId(TargetAreaEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> FADE_OUT_TICKS = SynchedEntityData.defineId(TargetAreaEntity.class, EntityDataSerializers.INT);

	public TargetAreaEntity(EntityType<TargetAreaEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(SHAPE_TYPE, AreaShape.CIRCLE.ordinal());
		builder.define(RADIUS, DEFAULT_RADIUS);
		builder.define(HALF_WIDTH, DEFAULT_HALF_SIZE);
		builder.define(HALF_DEPTH, DEFAULT_HALF_SIZE);
		builder.define(HEIGHT, DEFAULT_HEIGHT);
		builder.define(COLOR, DEFAULT_COLOR);
		builder.define(DURATION, DEFAULT_DURATION);
		builder.define(FADE_IN_TICKS, 0);
		builder.define(FADE_OUT_TICKS, 0);
	}

	@Override
	public void tick() {
		super.tick();
		this.setDeltaMovement(Vec3.ZERO);
		this.noPhysics = true;
		this.setNoGravity(true);

		if (!this.level().isClientSide() && this.tickCount >= this.getDuration()) {
			this.discard();
		}
	}

	public AreaShape getShape() {
		int index = Mth.clamp(this.entityData.get(SHAPE_TYPE), 0, AreaShape.values().length - 1);
		return AreaShape.values()[index];
	}

	public void setShape(AreaShape shape) {
		this.entityData.set(SHAPE_TYPE, shape.ordinal());
		this.refreshDimensions();
	}

	public float getRadius() {
		return Math.max(MIN_SIZE, this.entityData.get(RADIUS));
	}

	public void setRadius(float radius) {
		this.entityData.set(RADIUS, Math.max(MIN_SIZE, radius));
		this.refreshDimensions();
	}

	public float getHalfWidth() {
		return Math.max(MIN_SIZE, this.entityData.get(HALF_WIDTH));
	}

	public void setHalfWidth(float halfWidth) {
		this.entityData.set(HALF_WIDTH, Math.max(MIN_SIZE, halfWidth));
		this.refreshDimensions();
	}

	public float getHalfDepth() {
		return Math.max(MIN_SIZE, this.entityData.get(HALF_DEPTH));
	}

	public void setHalfDepth(float halfDepth) {
		this.entityData.set(HALF_DEPTH, Math.max(MIN_SIZE, halfDepth));
		this.refreshDimensions();
	}

	public float getAreaHeight() {
		return Math.max(MIN_SIZE, this.entityData.get(HEIGHT));
	}

	public void setAreaHeight(float height) {
		this.entityData.set(HEIGHT, Math.max(MIN_SIZE, height));
		this.refreshDimensions();
	}

	public int getColor() {
		return this.entityData.get(COLOR);
	}

	public void setColor(int color) {
		this.entityData.set(COLOR, color);
	}

	public int getDuration() {
		return Math.max(1, this.entityData.get(DURATION));
	}

	public void setDuration(int duration) {
		this.entityData.set(DURATION, Math.max(1, duration));
	}

	public int getFadeInTicks() {
		return Math.max(0, this.entityData.get(FADE_IN_TICKS));
	}

	public void setFadeInTicks(int fadeInTicks) {
		this.entityData.set(FADE_IN_TICKS, Math.max(0, fadeInTicks));
	}

	public int getFadeOutTicks() {
		return Math.max(0, this.entityData.get(FADE_OUT_TICKS));
	}

	public void setFadeOutTicks(int fadeOutTicks) {
		this.entityData.set(FADE_OUT_TICKS, Math.max(0, fadeOutTicks));
	}

	public void configure(AreaShape shape, float radius, float halfWidth, float halfDepth, int color, int duration, float height, int fadeInTicks, int fadeOutTicks) {
		this.entityData.set(SHAPE_TYPE, shape.ordinal());
		this.entityData.set(RADIUS, Math.max(MIN_SIZE, radius));
		this.entityData.set(HALF_WIDTH, Math.max(MIN_SIZE, halfWidth));
		this.entityData.set(HALF_DEPTH, Math.max(MIN_SIZE, halfDepth));
		this.entityData.set(COLOR, color);
		this.entityData.set(DURATION, Math.max(1, duration));
		this.entityData.set(HEIGHT, Math.max(MIN_SIZE, height));
		this.entityData.set(FADE_IN_TICKS, Math.max(0, fadeInTicks));
		this.entityData.set(FADE_OUT_TICKS, Math.max(0, fadeOutTicks));
		this.refreshDimensions();
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		float width = switch (this.getShape()) {
			case CIRCLE -> this.getRadius() * 2.0F;
			case SQUARE, RECTANGLE -> Math.max(this.getHalfWidth(), this.getHalfDepth()) * 2.0F;
		};
		return EntityDimensions.fixed(Math.max(MIN_SIZE, width), this.getAreaHeight());
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		if (SHAPE_TYPE.equals(key) || RADIUS.equals(key) || HALF_WIDTH.equals(key) || HALF_DEPTH.equals(key) || HEIGHT.equals(key)) {
			this.refreshDimensions();
		}
		super.onSyncedDataUpdated(key);
	}

	@Override
	public void refreshDimensions() {
		double x = this.getX();
		double y = this.getY();
		double z = this.getZ();
		super.refreshDimensions();
		this.setPos(x, y, z);
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
	protected void doWaterSplashEffect() {
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
	public void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("ShapeType", this.entityData.get(SHAPE_TYPE));
		tag.putFloat("Radius", this.entityData.get(RADIUS));
		tag.putFloat("HalfWidth", this.entityData.get(HALF_WIDTH));
		tag.putFloat("HalfDepth", this.entityData.get(HALF_DEPTH));
		tag.putFloat("Height", this.entityData.get(HEIGHT));
		tag.putInt("Color", this.entityData.get(COLOR));
		tag.putInt("Duration", this.entityData.get(DURATION));
		tag.putInt("FadeInTicks", this.entityData.get(FADE_IN_TICKS));
		tag.putInt("FadeOutTicks", this.entityData.get(FADE_OUT_TICKS));
		tag.putInt("Age", this.tickCount);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		this.entityData.set(SHAPE_TYPE, Mth.clamp(tag.getInt("ShapeType"), 0, AreaShape.values().length - 1));
		this.entityData.set(RADIUS, Math.max(MIN_SIZE, tag.getFloat("Radius")));
		this.entityData.set(HALF_WIDTH, Math.max(MIN_SIZE, tag.getFloat("HalfWidth")));
		this.entityData.set(HALF_DEPTH, Math.max(MIN_SIZE, tag.getFloat("HalfDepth")));
		this.entityData.set(HEIGHT, Math.max(MIN_SIZE, tag.getFloat("Height")));
		this.entityData.set(COLOR, tag.contains("Color") ? tag.getInt("Color") : DEFAULT_COLOR);
		this.entityData.set(DURATION, Math.max(1, tag.contains("Duration") ? tag.getInt("Duration") : DEFAULT_DURATION));
		this.entityData.set(FADE_IN_TICKS, Math.max(0, tag.getInt("FadeInTicks")));
		this.entityData.set(FADE_OUT_TICKS, Math.max(0, tag.getInt("FadeOutTicks")));
		this.tickCount = Math.max(0, tag.getInt("Age"));
		this.refreshDimensions();
	}

	public static TargetAreaEntity spawnCircle(ServerLevel level, Vec3 position, float radius, int color, int duration, float height) {
		return spawnCircle(level, position, radius, color, duration, height, 0, 0);
	}

	public static TargetAreaEntity spawnCircle(ServerLevel level, Vec3 position, float radius, int color, int duration, float height, int fadeInTicks, int fadeOutTicks) {
		TargetAreaEntity entity = create(level, position);
		float safeRadius = Math.max(MIN_SIZE, radius);
		entity.configure(AreaShape.CIRCLE, safeRadius, safeRadius, safeRadius, color, duration, height, fadeInTicks, fadeOutTicks);
		level.addFreshEntity(entity);
		return entity;
	}

	public static TargetAreaEntity spawnSquare(ServerLevel level, Vec3 position, float size, int color, int duration, float height) {
		return spawnSquare(level, position, size, color, duration, height, 0, 0);
	}

	public static TargetAreaEntity spawnSquare(ServerLevel level, Vec3 position, float size, int color, int duration, float height, int fadeInTicks, int fadeOutTicks) {
		TargetAreaEntity entity = create(level, position);
		float halfSize = Math.max(MIN_SIZE, size * 0.5F);
		entity.configure(AreaShape.SQUARE, halfSize, halfSize, halfSize, color, duration, height, fadeInTicks, fadeOutTicks);
		level.addFreshEntity(entity);
		return entity;
	}

	public static TargetAreaEntity spawnSquareOnBlock(ServerLevel level, BlockPos center, float size, int color, int duration, float height, int fadeInTicks, int fadeOutTicks) {
		Vec3 position = new Vec3(center.getX() + 0.5D, center.getY() + 1.01D, center.getZ() + 0.5D);
		return spawnSquare(level, position, size, color, duration, height, fadeInTicks, fadeOutTicks);
	}

	public static TargetAreaEntity spawnRectangle(ServerLevel level, Vec3 position, float width, float depth, int color, int duration, float height) {
		return spawnRectangle(level, position, width, depth, color, duration, height, 0, 0);
	}

	public static TargetAreaEntity spawnRectangle(ServerLevel level, Vec3 position, float width, float depth, int color, int duration, float height, int fadeInTicks, int fadeOutTicks) {
		TargetAreaEntity entity = create(level, position);
		float halfWidth = Math.max(MIN_SIZE, width * 0.5F);
		float halfDepth = Math.max(MIN_SIZE, depth * 0.5F);
		entity.configure(AreaShape.RECTANGLE, Math.max(halfWidth, halfDepth), halfWidth, halfDepth, color, duration, height, fadeInTicks, fadeOutTicks);
		level.addFreshEntity(entity);
		return entity;
	}

	private static TargetAreaEntity create(ServerLevel level, Vec3 position) {
		TargetAreaEntity entity = new TargetAreaEntity(TimothatysTrinketsModEntities.TARGET_AREA.get(), level);
		entity.moveTo(position.x, position.y, position.z, 0.0F, 0.0F);
		return entity;
	}
}
