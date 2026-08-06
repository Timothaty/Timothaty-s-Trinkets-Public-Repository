package net.timothaty.timothatystrinkets.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModEntities;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.item.BottleOfSoulOrbItem;

import java.util.UUID;

public class SoulOrbEntity extends AbstractOrbEntity {
	private static final String SOUL_VALUE_TAG = "SoulValue";
	private static final int LIFETIME_TICKS = 15 * 20;
	private static final EntityDimensions SOUL_ORB_DIMENSIONS = EntityDimensions.fixed(0.35F, 0.35F).withEyeHeight(0.18F);
	private static final DustParticleOptions SOUL_DUST = new DustParticleOptions(new Vector3f(0.0F, 1.0F, 0.6549F), 1.0F);
	private static final EntityDataAccessor<Boolean> SOUL_ABSORPTION_TRAIL = SynchedEntityData.defineId(SoulOrbEntity.class, EntityDataSerializers.BOOLEAN);
	private int age;
	private int soulValue = 1;
	private long lastSoulUnitConsumptionTick = Long.MIN_VALUE;
	private UUID soulAbsorptionTargetUuid;
	private float soulAbsorptionSpeedMultiplier = 1.0F;

	public SoulOrbEntity(EntityType<SoulOrbEntity> type, Level level) {
		super(type, level);
	}

	public SoulOrbEntity(Level level, double x, double y, double z) {
		this(TimothatysTrinketsModEntities.SOUL_ORB.get(), level);
		this.setPos(x, y, z);
		this.setYRot((float) (this.random.nextDouble() * 360.0D));
		this.setDeltaMovement((this.random.nextDouble() * 0.2D - 0.1D) * 1.35D, this.random.nextDouble() * 0.18D + 0.08D, (this.random.nextDouble() * 0.2D - 0.1D) * 1.35D);
	}

	@Override
	protected double getDefaultGravity() {
		return 0.03D;
	}

	@Override
	protected EntityDimensions getOrbDimensions(Pose pose) {
		return SOUL_ORB_DIMENSIONS;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SOUL_ABSORPTION_TRAIL, false);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (this.level().isClientSide() && SOUL_ABSORPTION_TRAIL.equals(key)) {
			SoulOrbTrailStateDispatcher.notifyChanged(this);
		}
	}

	@Override
	public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
		this.setPos(x, y, z);
		this.setRot(yRot, xRot);
	}

	@Override
	public void tick() {
		super.tick();
		this.xo = this.getX();
		this.yo = this.getY();
		this.zo = this.getZ();

		if (this.isEyeInFluid(FluidTags.WATER)) {
			this.setUnderwaterMovement();
		} else {
			this.applyGravity();
		}

		if (this.level().getFluidState(this.blockPosition()).is(FluidTags.LAVA)) {
			this.setDeltaMovement((this.random.nextFloat() - this.random.nextFloat()) * 0.2F, 0.2D, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
		}

		if (!this.level().noCollision(this.getBoundingBox())) {
			this.moveTowardsClosestSpace(this.getX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) * 0.5D, this.getZ());
		}

		this.move(MoverType.SELF, this.getDeltaMovement());

		float friction = 0.98F;
		if (this.onGround()) {
			BlockPos pos = this.getBlockPosBelowThatAffectsMyMovement();
			friction = this.level().getBlockState(pos).getFriction(this.level(), pos, this) * 0.98F;
		}

		this.setDeltaMovement(this.getDeltaMovement().multiply(friction, 0.98D, friction));
		if (this.onGround()) {
			this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, -0.9D, 1.0D));
		}

		if (!this.level().isClientSide() && ++this.age >= LIFETIME_TICKS) {
			this.discardRemainingSoulValue();
		}
	}

	@Override
	public BlockPos getBlockPosBelowThatAffectsMyMovement() {
		return this.getOnPos(0.999999F);
	}

	private void setUnderwaterMovement() {
		Vec3 motion = this.getDeltaMovement();
		this.setDeltaMovement(motion.x * 0.99D, Math.min(motion.y + 5.0E-4D, 0.06D), motion.z * 0.99D);
	}

	public boolean isAvailableForSoulAbsorption() {
		return this.hasSoulValue() && this.soulAbsorptionTargetUuid == null && !this.isRemoved();
	}

	public int getSoulValue() {
		return Math.max(0, this.soulValue);
	}

	public void setSoulValue(int value) {
		this.soulValue = Math.max(1, value);
	}

	public boolean hasSoulValue() {
		return this.soulValue > 0;
	}

	public boolean consumeOneSoulUnit() {
		if (this.level().isClientSide() || this.isRemoved() || !this.hasSoulValue()) {
			return false;
		}

		long gameTime = this.level().getGameTime();
		if (this.lastSoulUnitConsumptionTick == gameTime) {
			return false;
		}

		this.lastSoulUnitConsumptionTick = gameTime;
		this.soulValue--;
		return true;
	}

	public boolean isSoulAbsorbedBy(Entity target) {
		return target != null && this.isSoulAbsorbedBy(target.getUUID());
	}

	public boolean isSoulAbsorbedBy(UUID targetUuid) {
		return targetUuid != null && targetUuid.equals(this.soulAbsorptionTargetUuid);
	}

	public void setSoulAbsorptionTarget(LivingEntity target) {
		this.setSoulAbsorptionTarget(target, 1.0F);
	}

	public void setSoulAbsorptionTarget(LivingEntity target, float speedMultiplier) {
		this.soulAbsorptionTargetUuid = target == null ? null : target.getUUID();
		this.soulAbsorptionSpeedMultiplier = target == null ? 1.0F : Mth.clamp(speedMultiplier, 0.65F, 1.45F);
		this.entityData.set(SOUL_ABSORPTION_TRAIL, target != null);
	}

	public void clearSoulAbsorptionTarget() {
		this.soulAbsorptionTargetUuid = null;
		this.soulAbsorptionSpeedMultiplier = 1.0F;
		this.entityData.set(SOUL_ABSORPTION_TRAIL, false);
	}

	public float getSoulAbsorptionSpeedMultiplier() {
		return this.soulAbsorptionSpeedMultiplier;
	}

	public boolean hasSoulAbsorptionTrail() {
		return this.entityData.get(SOUL_ABSORPTION_TRAIL);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (this.isInvulnerableTo(source) || !canDestroySoulOrb(source)) {
			return false;
		}
		if (this.level().isClientSide()) {
			return true;
		}

		this.destroySoulOrb(true);
		return true;
	}

	private static boolean canDestroySoulOrb(DamageSource source) {
		return source != null && (isExplosionDamage(source) || isProjectileDamage(source) || isDirectMeleeDamage(source));
	}

	private static boolean isExplosionDamage(DamageSource source) {
		return source.is(DamageTypeTags.IS_EXPLOSION);
	}

	private static boolean isProjectileDamage(DamageSource source) {
		return source.getDirectEntity() instanceof Projectile;
	}

	private static boolean isDirectMeleeDamage(DamageSource source) {
		if (!source.is(DamageTypes.PLAYER_ATTACK) && !source.is(DamageTypes.MOB_ATTACK) && !source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)) {
			return false;
		}
		Entity attacker = source.getEntity();
		return attacker instanceof LivingEntity && source.getDirectEntity() == attacker;
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		if (!player.getItemInHand(hand).is(Items.GLASS_BOTTLE)) {
			return InteractionResult.PASS;
		}
		if (!this.level().isClientSide()) {
			this.fillSoulBottle(player, hand);
			this.spawnSoulDust(18, 0.18D, 0.16D, 0.18D, 0.02D);
			this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
					TimothatysTrinketsModSounds.BOTTLE_SOUL_ORB_CATCH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
			this.discardRemainingSoulValue();
		}
		return InteractionResult.sidedSuccess(this.level().isClientSide());
	}

	private void fillSoulBottle(Player player, InteractionHand hand) {
		ItemStack result = ItemUtils.createFilledResult(player.getItemInHand(hand), player,
				BottleOfSoulOrbItem.createFilledBottle(this.getSoulValue()));
		player.setItemInHand(hand, result);
	}

	private void destroySoulOrb(boolean playSound) {
		if (playSound) {
			this.level().playSound(null, this.getX(), this.getY(), this.getZ(), TimothatysTrinketsModSounds.SOUL_DESTRUCTION.get(), SoundSource.NEUTRAL, 0.9F, 1.0F + this.random.nextFloat() * 0.3F);
		}
		this.spawnSoulDust(24, 0.22D, 0.18D, 0.22D, 0.035D);
		this.discardRemainingSoulValue();
	}

	private void discardRemainingSoulValue() {
		this.clearSoulAbsorptionTarget();
		this.discard();
	}

	private void spawnSoulDust(int count, double xSpread, double ySpread, double zSpread, double speed) {
		if (this.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(SOUL_DUST, this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(), count, xSpread, ySpread, zSpread, speed);
		}
	}

	public int getIcon() {
		return Mth.clamp(this.tickCount / 2 % 4, 0, 3);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		compound.putInt("Age", this.age);
		compound.putInt(SOUL_VALUE_TAG, Math.max(1, this.soulValue));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		this.age = compound.getInt("Age");
		this.soulValue = compound.contains(SOUL_VALUE_TAG) ? Math.max(1, compound.getInt(SOUL_VALUE_TAG)) : 1;
		this.lastSoulUnitConsumptionTick = Long.MIN_VALUE;
		this.soulAbsorptionTargetUuid = null;
		this.soulAbsorptionSpeedMultiplier = 1.0F;
		this.entityData.set(SOUL_ABSORPTION_TRAIL, false);
	}

}
