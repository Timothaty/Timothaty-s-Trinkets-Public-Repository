package net.timothaty.timothatystrinkets.entity;

import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.state.BlockState;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.entity.ai.UndeadKnightBreakDoorGoal;
import net.timothaty.timothatystrinkets.entity.ai.UndeadKnightFollowNecromancerGoal;
import net.timothaty.timothatystrinkets.entity.ai.UndeadKnightMeleeAttackGoal;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModEntities;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;

import java.util.List;
import java.util.UUID;

public class UndeadKnightEntity extends Monster {
	public static final int DEATH_ANIMATION_NONE = 0;
	public static final int DEATH_ANIMATION_ONE = 1;
	public static final int DEATH_ANIMATION_TWO = 2;
	public static final int DEATH_ANIMATION_THREE = 3;
	private static final int DEATH_ONE_DURATION_TICKS = 70;
	private static final int DEATH_TWO_DURATION_TICKS = 50;
	private static final int DEATH_THREE_DURATION_TICKS = 45;
	private static final int EMPOWER_DURATION_TICKS = 20;
	private static final int ATTACK_INTERVAL_TICKS = 30;
	private static final int EMPOWER_HEALTH_COST_TICK = 10;
	private static final int EMPOWER_SOUND_TICK = 11;
	private static final int EMPOWER_BLOOD_START_TICK = 12;
	private static final int EMPOWER_BLOOD_END_TICK = 15;
	private static final float EMPOWER_CHANCE = 0.25F;
	private static final float EMPOWER_HEALTH_COST = 4.0F;
	private static final double EMPOWER_MOVEMENT_SPEED_PENALTY = -0.0668D;
	private static final int BLOCK_DURATION_TICKS = 15;
	private static final float REINCARNATION_HEALTH = 40.0F;
	private static final int REINCARNATION_ONE_DURATION_TICKS = 160;
	private static final int REINCARNATION_TWO_DURATION_TICKS = 165;
	private static final int REINCARNATION_THREE_DURATION_TICKS = 88;
	private static final int MIN_REINCARNATIONS = 1;
	private static final int MAX_REINCARNATIONS = 3;
	private static final float SOUL_ABSORPTION_SPAWN_CHANCE = 0.2F;
	private static final String CAN_ABSORB_SOULS_TAG = "CanAbsorbSouls";
	private static final String SOUL_ABSORBED_ORBS_TAG = "SoulAbsorbedOrbs";
	private static final String NECROMANCER_OWNER_UUID_TAG = "NecromancerOwner";
	private static final String NECROMANCER_FORMATION_SLOT_TAG = "NecromancerFormationSlot";
	public static final int NO_NECROMANCER_FORMATION_SLOT = -1;
	private static final ResourceLocation MAIN_HAND_LOOT_TABLE = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "gameplay/undead_knight_main_hand");
	private static final ResourceKey<LootTable> MAIN_HAND_LOOT_TABLE_KEY = ResourceKey.create(Registries.LOOT_TABLE, MAIN_HAND_LOOT_TABLE);
	private static final ResourceLocation EMPOWER_ARMOR_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "undead_knight_empower_armor");
	private static final ResourceLocation EMPOWER_ATTACK_DAMAGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "undead_knight_empower_attack_damage");
	private static final ResourceLocation EMPOWER_MOVEMENT_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "undead_knight_empower_movement_speed");

	public static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.defineId(UndeadKnightEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<Integer> ANIM = SynchedEntityData.defineId(UndeadKnightEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DEATH_ANIMATION_VARIANT = SynchedEntityData.defineId(UndeadKnightEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> EMPOWERING = SynchedEntityData.defineId(UndeadKnightEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> EMPOWERED = SynchedEntityData.defineId(UndeadKnightEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> EMPOWER_TICKS = SynchedEntityData.defineId(UndeadKnightEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> BLOCKING = SynchedEntityData.defineId(UndeadKnightEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> BLOCK_TICKS = SynchedEntityData.defineId(UndeadKnightEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> REINCARNATING = SynchedEntityData.defineId(UndeadKnightEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> REINCARNATIONS_REMAINING = SynchedEntityData.defineId(UndeadKnightEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> REINCARNATION_TICKS = SynchedEntityData.defineId(UndeadKnightEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> REINCARNATION_VARIANT = SynchedEntityData.defineId(UndeadKnightEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> CAN_ABSORB_SOULS = SynchedEntityData.defineId(UndeadKnightEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> SOUL_ABSORPTION_PHASE = SynchedEntityData.defineId(UndeadKnightEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SOUL_ABSORPTION_TICKS = SynchedEntityData.defineId(UndeadKnightEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SOUL_ABSORBED_ORBS = SynchedEntityData.defineId(UndeadKnightEntity.class, EntityDataSerializers.INT);
	public final AnimationState deathOneAnimationState = new AnimationState();
	public final AnimationState deathTwoAnimationState = new AnimationState();
	public final AnimationState deathThreeAnimationState = new AnimationState();
	public final AnimationState empowerAnimationState = new AnimationState();
	public final AnimationState blockAnimationState = new AnimationState();
	public final AnimationState undyingOneAnimationState = new AnimationState();
	public final AnimationState undyingTwoAnimationState = new AnimationState();
	public final AnimationState undyingThreeAnimationState = new AnimationState();
	public final AnimationState soulAbsorptionStartAnimationState = new AnimationState();
	public final AnimationState soulAbsorptionLoopAnimationState = new AnimationState();
	public final AnimationState soulAbsorptionEndAnimationState = new AnimationState();
	private int empowerTicks;
	private int blockTicks;
	private int lastEmpowerTargetId = -1;
	private final UndeadKnightSoulAbsorption soulAbsorption = new UndeadKnightSoulAbsorption(this);
	private UUID necromancerOwnerUuid;
	private int necromancerFormationSlot = NO_NECROMANCER_FORMATION_SLOT;
	private boolean noAiBeforeReincarnation;

	public UndeadKnightEntity(EntityType<UndeadKnightEntity> type, Level world) {
		super(type, world);
		xpReward = 0;
		setNoAi(false);
		if (this.getNavigation() instanceof GroundPathNavigation navigation) {
			navigation.setCanOpenDoors(true);
			navigation.setCanPassDoors(true);
		}
		this.entityData.set(REINCARNATIONS_REMAINING, this.rollReincarnationLimit());
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(TEXTURE, "undead_knight");
		builder.define(ANIM, 0);
		builder.define(DEATH_ANIMATION_VARIANT, DEATH_ANIMATION_NONE);
		builder.define(EMPOWERING, false);
		builder.define(EMPOWERED, false);
		builder.define(EMPOWER_TICKS, 0);
		builder.define(BLOCKING, false);
		builder.define(BLOCK_TICKS, 0);
		builder.define(REINCARNATING, false);
		builder.define(REINCARNATIONS_REMAINING, MIN_REINCARNATIONS);
		builder.define(REINCARNATION_TICKS, 0);
		builder.define(REINCARNATION_VARIANT, 0);
		builder.define(CAN_ABSORB_SOULS, false);
		builder.define(SOUL_ABSORPTION_PHASE, UndeadKnightSoulAbsorption.PHASE_NONE);
		builder.define(SOUL_ABSORPTION_TICKS, 0);
		builder.define(SOUL_ABSORBED_ORBS, 0);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (this.level().isClientSide() && (SOUL_ABSORPTION_PHASE.equals(key) || SOUL_ABSORPTION_TICKS.equals(key))) {
			this.soulAbsorption.updateClientAnimation();
		}
	}

	public void setTexture(String texture) {
		this.entityData.set(TEXTURE, texture);
	}

	public String getTexture() {
		return this.entityData.get(TEXTURE);
	}

	public int getDeathAnimationVariant() {
		return this.entityData.get(DEATH_ANIMATION_VARIANT);
	}

	public boolean isEmpowering() {
		return this.entityData.get(EMPOWERING);
	}

	public boolean isEmpowered() {
		return this.entityData.get(EMPOWERED);
	}

	public int getEmpowerTicks() {
		return this.entityData.get(EMPOWER_TICKS);
	}

	public boolean isBlocking() {
		return this.entityData.get(BLOCKING);
	}

	public int getBlockTicks() {
		return this.entityData.get(BLOCK_TICKS);
	}

	public boolean isReincarnating() {
		return this.entityData.get(REINCARNATING);
	}

	@Override
	public boolean canBeSeenAsEnemy() {
		return !this.isReincarnating() && super.canBeSeenAsEnemy();
	}

	public int getReincarnationsRemaining() {
		return this.entityData.get(REINCARNATIONS_REMAINING);
	}

	public boolean canReincarnate() {
		return this.getReincarnationsRemaining() > 0;
	}

	public boolean canAbsorbSouls() {
		return this.entityData.get(CAN_ABSORB_SOULS);
	}

	public void setCanAbsorbSouls(boolean canAbsorbSouls) {
		this.entityData.set(CAN_ABSORB_SOULS, canAbsorbSouls);
	}

	public int getReincarnationTicks() {
		return this.entityData.get(REINCARNATION_TICKS);
	}

	public int getReincarnationVariant() {
		return this.entityData.get(REINCARNATION_VARIANT);
	}

	public boolean isSoulAbsorbing() {
		return this.soulAbsorption.isActive();
	}

	public boolean isSoulAbsorptionStarting() {
		return this.soulAbsorption.isStarting();
	}

	public boolean isSoulAbsorptionLooping() {
		return this.soulAbsorption.isLooping();
	}

	public boolean isSoulAbsorptionEnding() {
		return this.soulAbsorption.isEnding();
	}

	public int getSoulAbsorptionPhase() {
		return this.entityData.get(SOUL_ABSORPTION_PHASE);
	}

	public int getSoulAbsorptionTicks() {
		return this.entityData.get(SOUL_ABSORPTION_TICKS);
	}

	public int getAbsorbedSoulOrbs() {
		return this.entityData.get(SOUL_ABSORBED_ORBS);
	}

	void setSoulAbsorptionPhaseData(int phase) {
		this.entityData.set(SOUL_ABSORPTION_PHASE, phase);
	}

	void setSoulAbsorptionTicksData(int ticks) {
		this.entityData.set(SOUL_ABSORPTION_TICKS, ticks);
	}

	void setAbsorbedSoulOrbsData(int absorbedOrbs) {
		this.entityData.set(SOUL_ABSORBED_ORBS, absorbedOrbs);
	}

	public UUID getNecromancerOwnerUuid() {
		return necromancerOwnerUuid;
	}

	public void setNecromancerOwner(NecromancerEntity necromancer) {
		this.setNecromancerOwner(necromancer, NO_NECROMANCER_FORMATION_SLOT);
	}

	public void setNecromancerOwner(NecromancerEntity necromancer, int formationSlot) {
		necromancerOwnerUuid = necromancer == null ? null : necromancer.getUUID();
		necromancerFormationSlot = necromancer == null ? NO_NECROMANCER_FORMATION_SLOT : formationSlot;
	}

	public void clearNecromancerOwner() {
		necromancerOwnerUuid = null;
		necromancerFormationSlot = NO_NECROMANCER_FORMATION_SLOT;
	}

	public int getNecromancerFormationSlot() {
		return necromancerFormationSlot;
	}

	private void setDeathAnimationVariant(int variant) {
		this.entityData.set(DEATH_ANIMATION_VARIANT, variant);
	}

	private int rollReincarnationLimit() {
		return MIN_REINCARNATIONS + this.random.nextInt(MAX_REINCARNATIONS - MIN_REINCARNATIONS + 1);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.isDeathAnimationActive()) {
			if (this.level().isClientSide()) {
				this.updateClientDeathAnimation();
				this.empowerAnimationState.stop();
				this.blockAnimationState.stop();
				this.soulAbsorption.stopClientAnimations();
			} else {
				this.soulAbsorption.clear(true);
				this.clearBlockState();
				this.clearEmpowerState();
				this.ensureDeathAnimationVariant();
				this.getNavigation().stop();
				this.setDeltaMovement(0.0D, Math.min(0.0D, this.getDeltaMovement().y), 0.0D);
			}
			return;
		}

		this.stopDeathAnimationStates();
		if (this.isReincarnating()) {
			if (this.level().isClientSide()) {
				this.updateClientReincarnationAnimation();
				this.empowerAnimationState.stop();
				this.blockAnimationState.stop();
				this.soulAbsorption.stopClientAnimations();
			} else {
				this.soulAbsorption.clear(true);
				this.lockReincarnationControl();
			}
			return;
		}

		this.stopReincarnationAnimationStates();
		if (!this.level().isClientSide()) {
			this.soulAbsorption.tickPassiveRegeneration();
			this.soulAbsorption.tickIdleScan();
		}
		if (this.isSoulAbsorbing()) {
			if (this.level().isClientSide()) {
				this.soulAbsorption.updateClientAnimation();
				this.empowerAnimationState.stop();
				this.blockAnimationState.stop();
			} else {
				this.soulAbsorption.tickActive();
			}
			return;
		}

		this.soulAbsorption.stopClientAnimations();
		if (this.isBlocking()) {
			if (this.level().isClientSide()) {
				this.updateClientBlockAnimation();
			} else {
				this.tickActiveBlock();
			}
			return;
		}

		this.blockAnimationState.stop();
		if (this.level().isClientSide()) {
			this.updateClientEmpowerAnimation();
		} else {
			this.tickEmpowerLogic();
		}
	}

	@Override
	public void travel(Vec3 travelVector) {
		if (this.isSoulAbsorbing()) {
			this.soulAbsorption.clearTravelInput();
			this.soulAbsorption.freezeHorizontalMotion();
			super.travel(Vec3.ZERO);
			this.soulAbsorption.freezeHorizontalMotion();
			return;
		}

		if (this.isReincarnating()) {
			this.clearReincarnationTravelInput();
			this.freezeReincarnationHorizontalMotion();
			super.travel(Vec3.ZERO);
			this.freezeReincarnationHorizontalMotion();
			return;
		}

		super.travel(travelVector);
	}

	public boolean isDeathAnimationActive() {
		return !this.isAlive() || this.deathTime > 0;
	}

	@Override
	public void die(DamageSource source) {
		if (!this.level().isClientSide()) {
			this.soulAbsorption.clear(true);
			this.clearEmpowerState();
			this.clearBlockState();
			this.ensureDeathAnimationVariant();
			if (this.deathTime == 0) {
				this.playDeathAnimationSound();
			}
		}
		super.die(source);
	}

	@Override
	protected void tickDeath() {
		if (!this.level().isClientSide()) {
			this.ensureDeathAnimationVariant();
		}

		this.deathTime++;
		if (this.deathTime >= this.getDeathAnimationDurationTicks() && !this.level().isClientSide() && !this.isRemoved()) {
			this.level().broadcastEntityEvent(this, (byte) 60);
			this.remove(Entity.RemovalReason.KILLED);
		}
	}

	private void ensureDeathAnimationVariant() {
		if (this.getDeathAnimationVariant() == DEATH_ANIMATION_NONE) {
			this.setDeathAnimationVariant(1 + this.random.nextInt(3));
		}
	}

	private void playDeathAnimationSound() {
		SoundEvent deathSound = switch (this.getDeathAnimationVariant()) {
			case DEATH_ANIMATION_TWO -> TimothatysTrinketsModSounds.UNDEAD_KNIGHT_DEATH_2.get();
			case DEATH_ANIMATION_THREE -> TimothatysTrinketsModSounds.UNDEAD_KNIGHT_DEATH_3.get();
			case DEATH_ANIMATION_ONE -> TimothatysTrinketsModSounds.UNDEAD_KNIGHT_DEATH_1.get();
			default -> null;
		};
		if (deathSound != null) {
			this.level().playSound(null, this.getX(), this.getY(), this.getZ(), deathSound, this.getSoundSource(), 1.0F, 1.0F);
		}
	}

	private int getDeathAnimationDurationTicks() {
		return switch (this.getDeathAnimationVariant()) {
			case DEATH_ANIMATION_TWO -> DEATH_TWO_DURATION_TICKS;
			case DEATH_ANIMATION_THREE -> DEATH_THREE_DURATION_TICKS;
			case DEATH_ANIMATION_ONE -> DEATH_ONE_DURATION_TICKS;
			default -> DEATH_ONE_DURATION_TICKS;
		};
	}

	private void updateClientDeathAnimation() {
		int variant = this.getDeathAnimationVariant();
		if (variant == DEATH_ANIMATION_NONE) {
			variant = DEATH_ANIMATION_ONE;
		}

		if (variant == DEATH_ANIMATION_ONE) {
			this.deathOneAnimationState.startIfStopped(this.tickCount);
		} else {
			this.deathOneAnimationState.stop();
		}

		if (variant == DEATH_ANIMATION_TWO) {
			this.deathTwoAnimationState.startIfStopped(this.tickCount);
		} else {
			this.deathTwoAnimationState.stop();
		}

		if (variant == DEATH_ANIMATION_THREE) {
			this.deathThreeAnimationState.startIfStopped(this.tickCount);
		} else {
			this.deathThreeAnimationState.stop();
		}
	}

	private void stopDeathAnimationStates() {
		this.deathOneAnimationState.stop();
		this.deathTwoAnimationState.stop();
		this.deathThreeAnimationState.stop();
	}

	private void updateClientReincarnationAnimation() {
		int startTick = this.tickCount - this.getReincarnationTicks();
		switch (this.getReincarnationVariant()) {
			case 1:
				this.undyingOneAnimationState.stop();
				this.undyingTwoAnimationState.startIfStopped(startTick);
				this.undyingThreeAnimationState.stop();
				break;
			case 2:
				this.undyingOneAnimationState.stop();
				this.undyingTwoAnimationState.stop();
				this.undyingThreeAnimationState.startIfStopped(startTick);
				break;
			case 0:
			default:
				this.undyingOneAnimationState.startIfStopped(startTick);
				this.undyingTwoAnimationState.stop();
				this.undyingThreeAnimationState.stop();
				break;
		}
	}

	private void stopReincarnationAnimationStates() {
		this.undyingOneAnimationState.stop();
		this.undyingTwoAnimationState.stop();
		this.undyingThreeAnimationState.stop();
	}

	public boolean canStartSoulAbsorption() {
		return this.soulAbsorption.canStart();
	}

	public boolean startSoulAbsorption() {
		return this.soulAbsorption.start();
	}

	public void interruptSoulAbsorption() {
		if (!this.level().isClientSide()) {
			this.soulAbsorption.clear(true);
		}
	}

	private void updateClientEmpowerAnimation() {
		if (this.isEmpowering()) {
			this.empowerAnimationState.startIfStopped(this.tickCount);
		} else {
			this.empowerAnimationState.stop();
		}
	}

	private void updateClientBlockAnimation() {
		if (this.isBlocking()) {
			this.blockAnimationState.startIfStopped(this.tickCount - this.getBlockTicks());
		} else {
			this.blockAnimationState.stop();
		}
	}

	private void tickEmpowerLogic() {
		if (this.isEmpowering()) {
			this.tickActiveEmpower();
			return;
		}

		if (this.isEmpowered()) {
			this.spawnEmpoweredParticles();
			return;
		}

		LivingEntity target = this.getTarget();
		int targetId = target == null ? -1 : target.getId();
		if (targetId != this.lastEmpowerTargetId) {
			this.lastEmpowerTargetId = targetId;
			if (target != null && this.canTargetLivingEntity(target) && this.random.nextFloat() < EMPOWER_CHANCE) {
				this.startEmpower();
			}
		}
	}

	private void startEmpower() {
		this.entityData.set(EMPOWERING, true);
		this.empowerTicks = 0;
		this.entityData.set(EMPOWER_TICKS, this.empowerTicks);
		this.getNavigation().stop();
		this.setDeltaMovement(0.0D, Math.min(0.0D, this.getDeltaMovement().y), 0.0D);
	}

	private void tickActiveEmpower() {
		LivingEntity target = this.getTarget();
		if (target != null && target.isAlive()) {
			this.getLookControl().setLookAt(target, 30.0F, 30.0F);
		}

		this.getNavigation().stop();
		this.setDeltaMovement(0.0D, Math.min(0.0D, this.getDeltaMovement().y), 0.0D);
		this.empowerTicks++;
		this.entityData.set(EMPOWER_TICKS, this.empowerTicks);

		if (this.empowerTicks == EMPOWER_HEALTH_COST_TICK) {
			this.applyEmpowerHealthCost();
			if (this.isReincarnating() || this.isDeathAnimationActive()) {
				return;
			}
		}
		if (this.empowerTicks == EMPOWER_SOUND_TICK) {
			this.playEmpowerSound();
		}
		if (this.empowerTicks >= EMPOWER_BLOOD_START_TICK && this.empowerTicks <= EMPOWER_BLOOD_END_TICK) {
			this.spawnEmpowerBloodParticles();
		}
		if (this.empowerTicks >= EMPOWER_DURATION_TICKS) {
			this.finishEmpower();
		}
	}

	private void finishEmpower() {
		this.entityData.set(EMPOWERING, false);
		this.setEmpowered(true);
		this.empowerTicks = 0;
		this.entityData.set(EMPOWER_TICKS, this.empowerTicks);
	}

	private void setEmpowered(boolean empowered) {
		this.entityData.set(EMPOWERED, empowered);
		this.updateEmpowerAttributeModifiers(empowered);
	}

	void clearEmpowerState() {
		this.entityData.set(EMPOWERING, false);
		this.setEmpowered(false);
		this.empowerTicks = 0;
		this.entityData.set(EMPOWER_TICKS, this.empowerTicks);
		this.lastEmpowerTargetId = -1;
	}

	public boolean startBlock() {
		if (this.level().isClientSide() || this.isBlocking() || this.isSoulAbsorbing() || this.isEmpowering() || this.isReincarnating() || this.isDeathAnimationActive() || this.isNoAi())
			return false;

		this.entityData.set(BLOCKING, true);
		this.blockTicks = 0;
		this.entityData.set(BLOCK_TICKS, this.blockTicks);
		this.getNavigation().stop();
		this.setAggressive(false);
		this.setDeltaMovement(0.0D, Math.min(0.0D, this.getDeltaMovement().y), 0.0D);
		this.level().playSound(null, this.blockPosition(), TimothatysTrinketsModSounds.SWORD_PARRY.get(), SoundSource.HOSTILE, 0.8F, 0.75F + this.random.nextFloat() * 0.18F);
		return true;
	}

	public void tickActiveBlock() {
		if (!this.isBlocking() || this.level().isClientSide())
			return;

		this.getNavigation().stop();
		this.setAggressive(false);
		this.setDeltaMovement(0.0D, Math.min(0.0D, this.getDeltaMovement().y), 0.0D);
		this.blockTicks++;
		this.entityData.set(BLOCK_TICKS, this.blockTicks);
		if (this.blockTicks >= BLOCK_DURATION_TICKS) {
			this.clearBlockState();
		}
	}

	void clearBlockState() {
		this.entityData.set(BLOCKING, false);
		this.blockTicks = 0;
		this.entityData.set(BLOCK_TICKS, this.blockTicks);
	}

	public void startReincarnation(int variant) {
		if (this.level().isClientSide() || this.isReincarnating() || !this.canReincarnate())
			return;

		int reincarnationVariant = Mth.clamp(variant, 0, 2);
		this.noAiBeforeReincarnation = this.isNoAi();
		this.soulAbsorption.clear(true);
		this.clearBlockState();
		this.clearEmpowerState();
		this.entityData.set(REINCARNATING, true);
		this.entityData.set(REINCARNATIONS_REMAINING, Math.max(0, this.getReincarnationsRemaining() - 1));
		this.entityData.set(REINCARNATION_VARIANT, reincarnationVariant);
		this.entityData.set(REINCARNATION_TICKS, 0);
		this.setHealth(1.0F);
		this.lockReincarnationControl();
		this.playReincarnationSound(reincarnationVariant);
	}

	private void playReincarnationSound(int variant) {
		SoundEvent reincarnationSound = switch (variant) {
			case 1 -> TimothatysTrinketsModSounds.UNDEAD_KNIGHT_REINCARNATION_2.get();
			case 2 -> TimothatysTrinketsModSounds.UNDEAD_KNIGHT_REINCARNATION_3.get();
			default -> TimothatysTrinketsModSounds.UNDEAD_KNIGHT_REINCARNATION_1.get();
		};
		this.level().playSound(null, this.getX(), this.getY(), this.getZ(), reincarnationSound, this.getSoundSource(), 1.0F, 1.0F);
	}

	public void tickReincarnation() {
		if (!this.isReincarnating() || this.level().isClientSide())
			return;

		this.lockReincarnationControl();
		if (this.getHealth() < 1.0F) {
			this.setHealth(1.0F);
		}

		int ticks = this.getReincarnationTicks() + 1;
		this.entityData.set(REINCARNATION_TICKS, ticks);
		if (ticks >= this.getReincarnationDurationTicks()) {
			this.finishReincarnation();
		}
	}

	private int getReincarnationDurationTicks() {
		return switch (this.getReincarnationVariant()) {
			case 1 -> REINCARNATION_TWO_DURATION_TICKS;
			case 2 -> REINCARNATION_THREE_DURATION_TICKS;
			default -> REINCARNATION_ONE_DURATION_TICKS;
		};
	}

	private void finishReincarnation() {
		this.entityData.set(REINCARNATING, false);
		this.entityData.set(REINCARNATION_TICKS, 0);
		this.setNoAi(this.noAiBeforeReincarnation);
		this.setHealth(Math.min(REINCARNATION_HEALTH, this.getMaxHealth()));
		this.invulnerableTime = 0;
	}

	private void lockReincarnationControl() {
		this.getNavigation().stop();
		this.setTarget(null);
		this.setLastHurtByMob(null);
		this.setAggressive(false);
		this.clearReincarnationTravelInput();
		this.freezeReincarnationHorizontalMotion();
	}

	private void clearReincarnationTravelInput() {
		this.xxa = 0.0F;
		this.yya = 0.0F;
		this.zza = 0.0F;
	}

	private void freezeReincarnationHorizontalMotion() {
		Vec3 motion = this.getDeltaMovement();
		this.setDeltaMovement(0.0D, Math.min(0.0D, motion.y), 0.0D);
		this.hurtMarked = true;
	}

	private void updateEmpowerAttributeModifiers(boolean empowered) {
		this.updateEmpowerAttributeModifier(Attributes.ARMOR, EMPOWER_ARMOR_MODIFIER_ID, 4.0D, AttributeModifier.Operation.ADD_VALUE, empowered);
		this.updateEmpowerAttributeModifier(Attributes.ATTACK_DAMAGE, EMPOWER_ATTACK_DAMAGE_MODIFIER_ID, 3.0D, AttributeModifier.Operation.ADD_VALUE, empowered);
		this.updateEmpowerAttributeModifier(Attributes.MOVEMENT_SPEED, EMPOWER_MOVEMENT_SPEED_MODIFIER_ID, EMPOWER_MOVEMENT_SPEED_PENALTY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, empowered);
	}

	private void updateEmpowerAttributeModifier(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, ResourceLocation modifierId, double amount, AttributeModifier.Operation operation, boolean shouldHaveModifier) {
		AttributeInstance attributeInstance = this.getAttribute(attribute);
		if (attributeInstance == null) {
			return;
		}

		attributeInstance.removeModifier(modifierId);
		if (shouldHaveModifier) {
			attributeInstance.addTransientModifier(new AttributeModifier(modifierId, amount, operation));
		}
	}

	private void playEmpowerSound() {
		this.level().playSound(null, this.blockPosition(), SoundEvents.TRIAL_SPAWNER_OMINOUS_ACTIVATE, SoundSource.HOSTILE, 0.9F, 0.75F + this.random.nextFloat() * 0.1F);
	}

	private void applyEmpowerHealthCost() {
		if (this.getHealth() > EMPOWER_HEALTH_COST) {
			this.setHealth(this.getHealth() - EMPOWER_HEALTH_COST);
			return;
		}

		this.invulnerableTime = 0;
		this.hurt(this.damageSources().genericKill(), EMPOWER_HEALTH_COST);
	}

	private void spawnEmpowerBloodParticles() {
		if (!(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		Vec3 leftArm = this.getLeftArmParticlePosition();
		serverLevel.sendParticles(TimothatysTrinketsModParticleTypes.BLOOD_BIT.get(), leftArm.x, leftArm.y, leftArm.z, 5, 0.08D, 0.12D, 0.08D, 0.045D);
	}

	private Vec3 getLeftArmParticlePosition() {
		Vec3 forward = this.getLookAngle();
		Vec3 left = new Vec3(forward.z, 0.0D, -forward.x);
		if (left.lengthSqr() < 1.0E-4D) {
			left = new Vec3(1.0D, 0.0D, 0.0D);
		} else {
			left = left.normalize();
		}
		Vec3 flatForward = new Vec3(forward.x, 0.0D, forward.z);
		if (flatForward.lengthSqr() > 1.0E-4D) {
			flatForward = flatForward.normalize();
		}

		return this.position()
				.add(left.scale(0.42D))
				.add(flatForward.scale(0.05D))
				.add(0.0D, 1.15D + this.random.nextDouble() * 0.25D, 0.0D);
	}

	private void spawnEmpoweredParticles() {
		if (!(this.level() instanceof ServerLevel serverLevel) || this.tickCount % 10 != 0) {
			return;
		}

		serverLevel.sendParticles(ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS, this.getX(), this.getY() + 1.05D, this.getZ(), 3, 0.35D, 0.45D, 0.35D, 0.015D);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new UndeadKnightBreakDoorGoal(this));
		this.goalSelector.addGoal(2, new UndeadKnightMeleeAttackGoal(this, 1.2, false, ATTACK_INTERVAL_TICKS));
		this.goalSelector.addGoal(3, new UndeadKnightFollowNecromancerGoal(this, 1.1D));
		this.goalSelector.addGoal(4, new MoveThroughVillageGoal(this, 1.0D, true, 4, () -> true));
		this.goalSelector.addGoal(5, new RandomStrollGoal(this, 1));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
			@Override
			public boolean canUse() {
				return super.canUse() && UndeadKnightEntity.this.canTargetLivingEntity(UndeadKnightEntity.this.getLastHurtByMob());
			}
		});
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, this::canTargetLivingEntity));
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
	}

	private boolean canTargetLivingEntity(LivingEntity target) {
		return target != null
				&& target != this
				&& target.isAlive()
				&& target.canBeSeenAsEnemy()
				&& !(target instanceof Enemy)
				&& !isUndeadTarget(target)
				&& !isAnimalTarget(target)
				&& !this.isAlliedTo(target)
				&& EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target);
	}

	private static boolean isUndeadTarget(LivingEntity target) {
		return target.isInvertedHealAndHarm() || target.getType().is(EntityTypeTags.UNDEAD);
	}

	private static boolean isAnimalTarget(LivingEntity target) {
		return target instanceof Animal || target instanceof WaterAnimal || target instanceof AmbientCreature;
	}

	@Override
	public boolean doHurtTarget(Entity entity) {
		if (this.isBlocking() || this.isSoulAbsorbing() || this.isEmpowering() || this.isReincarnating())
			return false;
		return super.doHurtTarget(entity);
	}

	@Override
	public HumanoidArm getMainArm() {
		return HumanoidArm.RIGHT;
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnGroupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
		this.setCanAbsorbSouls(this.random.nextFloat() < SOUL_ABSORPTION_SPAWN_CHANCE);
		if (this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
			this.equipSpawnWeapon(level.getLevel());
		}
		return data;
	}

	private void equipSpawnWeapon(ServerLevel level) {
		ItemStack weapon = this.rollMainHandWeapon(level);
		if (weapon.isEmpty()) {
			weapon = new ItemStack(Items.IRON_SWORD);
		}

		this.setItemSlot(EquipmentSlot.MAINHAND, weapon);
		this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
	}

	private ItemStack rollMainHandWeapon(ServerLevel level) {
		LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(MAIN_HAND_LOOT_TABLE_KEY);
		if (lootTable == LootTable.EMPTY) {
			return ItemStack.EMPTY;
		}

		LootParams lootParams = new LootParams.Builder(level)
				.withParameter(LootContextParams.THIS_ENTITY, this)
				.withParameter(LootContextParams.ORIGIN, this.position())
				.create(LootContextParamSets.GIFT);

		List<ItemStack> generatedLoot = lootTable.getRandomItems(lootParams);
		for (ItemStack stack : generatedLoot) {
			if (stack != null && !stack.isEmpty()) {
				return stack.copy();
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canUseSlot(EquipmentSlot slot) {
		return !isArmorSlot(slot) && super.canUseSlot(slot);
	}

	@Override
	public boolean canHoldItem(ItemStack stack) {
		return !isArmorItem(stack) && super.canHoldItem(stack);
	}

	@Override
	public boolean wantsToPickUp(ItemStack stack) {
		return !isArmorItem(stack) && super.wantsToPickUp(stack);
	}

	@Override
	public boolean canTakeItem(ItemStack stack) {
		return !isArmorItem(stack) && super.canTakeItem(stack);
	}

	@Override
	public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
		if (!stack.isEmpty() && (isArmorSlot(slot) || isArmorItem(stack))) {
			return;
		}
		super.setItemSlot(slot, stack);
	}

	private static boolean isArmorItem(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}

		EquipmentSlot directSlot = stack.getEquipmentSlot();
		if (isArmorSlot(directSlot)) {
			return true;
		}

		Equipable equipable = Equipable.get(stack);
		return equipable != null && isArmorSlot(equipable.getEquipmentSlot());
	}

	private static boolean isArmorSlot(EquipmentSlot slot) {
		return slot != null && slot.isArmor();
	}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.generic.hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return null;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState blockState) {
		this.level().playSound(null, this.getX(), this.getY(), this.getZ(), TimothatysTrinketsModSounds.UNDEAD_KNIGHT_STEP.get(), SoundSource.HOSTILE, 0.75F, 0.9F + this.random.nextFloat() * 0.2F);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putString("Texture", this.getTexture());
		compound.putInt("DeathAnimationVariant", this.getDeathAnimationVariant());
		compound.putBoolean("Empowered", this.isEmpowered());
		compound.putBoolean(CAN_ABSORB_SOULS_TAG, this.canAbsorbSouls());
		compound.putInt(SOUL_ABSORBED_ORBS_TAG, this.getAbsorbedSoulOrbs());
		compound.putInt("ReincarnationsRemaining", this.getReincarnationsRemaining());
		compound.putBoolean("Reincarnating", this.isReincarnating());
		compound.putInt("ReincarnationTicks", this.getReincarnationTicks());
		compound.putInt("ReincarnationVariant", this.getReincarnationVariant());
		compound.putBoolean("ReincarnationOldNoAi", this.noAiBeforeReincarnation);
		if (necromancerOwnerUuid != null) {
			compound.putUUID(NECROMANCER_OWNER_UUID_TAG, necromancerOwnerUuid);
			compound.putInt(NECROMANCER_FORMATION_SLOT_TAG, necromancerFormationSlot);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("Texture"))
			this.setTexture(compound.getString("Texture"));
		if (compound.contains("DeathAnimationVariant"))
			this.setDeathAnimationVariant(compound.getInt("DeathAnimationVariant"));
		this.setCanAbsorbSouls(compound.contains(CAN_ABSORB_SOULS_TAG) && compound.getBoolean(CAN_ABSORB_SOULS_TAG));
		necromancerOwnerUuid = compound.hasUUID(NECROMANCER_OWNER_UUID_TAG) ? compound.getUUID(NECROMANCER_OWNER_UUID_TAG) : null;
		necromancerFormationSlot = necromancerOwnerUuid != null && compound.contains(NECROMANCER_FORMATION_SLOT_TAG) ? compound.getInt(NECROMANCER_FORMATION_SLOT_TAG) : NO_NECROMANCER_FORMATION_SLOT;
		if (compound.contains("ReincarnationsRemaining")) {
			this.entityData.set(REINCARNATIONS_REMAINING, Mth.clamp(compound.getInt("ReincarnationsRemaining"), 0, MAX_REINCARNATIONS));
		} else if (compound.getBoolean("ReincarnationUsed")) {
			this.entityData.set(REINCARNATIONS_REMAINING, 0);
		}
		this.entityData.set(REINCARNATING, compound.getBoolean("Reincarnating"));
		this.entityData.set(REINCARNATION_TICKS, compound.getInt("ReincarnationTicks"));
		this.entityData.set(REINCARNATION_VARIANT, Mth.clamp(compound.getInt("ReincarnationVariant"), 0, 2));
		this.noAiBeforeReincarnation = compound.getBoolean("ReincarnationOldNoAi");
		this.entityData.set(EMPOWERING, false);
		this.empowerTicks = 0;
		this.entityData.set(EMPOWER_TICKS, this.empowerTicks);
		this.soulAbsorption.clear(true);
		this.soulAbsorption.setAbsorbedOrbs(this.canAbsorbSouls() ? compound.getInt(SOUL_ABSORBED_ORBS_TAG) : 0);
		this.clearBlockState();
		this.setEmpowered(compound.contains("Empowered") && compound.getBoolean("Empowered"));
		if (this.isReincarnating()) {
			this.setHealth(Math.max(1.0F, this.getHealth()));
			this.setNoAi(this.noAiBeforeReincarnation);
			this.lockReincarnationControl();
		}
	}

	public static void init(RegisterSpawnPlacementsEvent event) {
		event.register(TimothatysTrinketsModEntities.UNDEAD_KNIGHT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				UndeadKnightEntity::checkUndeadKnightSpawnRules,
				RegisterSpawnPlacementsEvent.Operation.REPLACE);
	}

	private static boolean checkUndeadKnightSpawnRules(EntityType<UndeadKnightEntity> entityType, ServerLevelAccessor world,
			MobSpawnType reason, BlockPos pos, net.minecraft.util.RandomSource random) {
		if (world.getDifficulty() == Difficulty.PEACEFUL) {
			return false;
		}

		if (reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION) {
			if (world.getBiome(pos).is(Biomes.DEEP_DARK)) {
				return false;
			}
			if (world.getLevel().dimension().equals(Level.NETHER) && !world.getBiome(pos).is(Biomes.SOUL_SAND_VALLEY)) {
				return false;
			}
		}

		return Monster.isDarkEnoughToSpawn(world, pos, random)
				&& Mob.checkMobSpawnRules(entityType, world, reason, pos, random);
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.24);
		builder = builder.add(Attributes.MAX_HEALTH, 60);
		builder = builder.add(Attributes.ARMOR, 7);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 3);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 1);
		builder = builder.add(Attributes.FOLLOW_RANGE, 24);
		builder = builder.add(Attributes.STEP_HEIGHT, 0.6);
		builder = builder.add(Attributes.SCALE, 1.0);
		return builder;
	}
}
