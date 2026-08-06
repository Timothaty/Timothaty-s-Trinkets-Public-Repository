package net.timothaty.timothatystrinkets.entity;

import net.timothaty.timothatystrinkets.entity.ai.NecromancerAi;
import net.timothaty.timothatystrinkets.entity.ai.NecromancerSpellParticles;
import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModEntities;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.necromancer.NecromancerConfig;
import net.timothaty.timothatystrinkets.mechanics.necromancer.UnholyAuraEvents;

import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

public class NecromancerEntity extends Monster {
	private static final int XP_REWARD = 12;
	private static final double VANILLA_ZOMBIE_MOVEMENT_SPEED = 0.23D;
	public static final int UNDEADIFICATION_CAST_ANIMATION_TICKS = 35;
	private static final int SUMMON_START_ANIMATION_TICKS = 14;
	private static final int SUMMON_LOOP_ANIMATION_TICKS = 40;
	private static final int SUMMON_END_ANIMATION_TICKS = 15;

	private static final int FIRST_NATURAL_SPAWN_DAY = 3;
	private static final long TICKS_PER_DAY = 24000L;
	public static final int SUMMON_CAST_STAGE_NONE = 0;
	public static final int SUMMON_CAST_STAGE_START = 1;
	public static final int SUMMON_CAST_STAGE_LOOP = 2;
	public static final int SUMMON_CAST_STAGE_END = 3;

	private static final ResourceLocation SUMMON_LOOP_KNOCKBACK_RESISTANCE_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "necromancer_summon_loop_knockback_resistance");
	private static final EntityDataAccessor<Long> UNDEADIFICATION_CAST_END_GAME_TIME = SynchedEntityData.defineId(NecromancerEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Integer> SUMMON_CAST_STAGE = SynchedEntityData.defineId(NecromancerEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> SUMMON_CAST_END_GAME_TIME = SynchedEntityData.defineId(NecromancerEntity.class, EntityDataSerializers.LONG);

	public final AnimationState undeadificationAnimationState = new AnimationState();
	public final AnimationState summonStartAnimationState = new AnimationState();
	public final AnimationState summonLoopAnimationState = new AnimationState();
	public final AnimationState summonEndAnimationState = new AnimationState();
	private long clientUndeadificationEndGameTime;
	private int clientSummonCastStage = SUMMON_CAST_STAGE_NONE;
	private long clientSummonCastEndGameTime;
	private boolean summonLoopKnockbackResistanceActive;

	public NecromancerEntity(EntityType<NecromancerEntity> type, Level level) {
		super(type, level);
		xpReward = XP_REWARD;
		setNoAi(false);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(UNDEADIFICATION_CAST_END_GAME_TIME, 0L);
		builder.define(SUMMON_CAST_STAGE, SUMMON_CAST_STAGE_NONE);
		builder.define(SUMMON_CAST_END_GAME_TIME, 0L);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		NecromancerAi.registerGoals(this, this.goalSelector, this.targetSelector);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		return super.hurt(source, amount);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return TimothatysTrinketsModSounds.NECROMANCER_HURT.get();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return TimothatysTrinketsModSounds.NECROMANCER_HURT.get();
	}

	@Override
	public void tick() {
		super.tick();
		tickUnholyAura();
		tickUndeadificationAnimation();
		tickSummonCastAnimation();
		updateSummonLoopKnockbackResistance();
	}

	private void tickUnholyAura() {
		if (level().isClientSide()
			|| Math.floorMod(tickCount + getId(), NecromancerConfig.UNHOLY_AURA_INTERVAL_TICKS) != 0) {
			return;
		}

		applyUnholyAuraTo(this);

		AABB auraBounds = getBoundingBox().inflate(NecromancerConfig.UNHOLY_AURA_RADIUS);
		for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, auraBounds, this::canApplyUnholyAuraTo)) {
			applyUnholyAuraTo(entity);
		}
	}

	private boolean canApplyUnholyAuraTo(LivingEntity entity) {
		return entity != this && entity.isAlive() && distanceToSqr(entity) <= NecromancerConfig.UNHOLY_AURA_RADIUS_SQR;
	}

	private void applyUnholyAuraTo(LivingEntity entity) {
		MobEffectInstance activeAura = entity.getEffect(TimothatysTrinketsModMobEffects.UNHOLY_AURA);
		if (activeAura != null && activeAura.getDuration() > NecromancerConfig.UNHOLY_AURA_REFRESH_THRESHOLD_TICKS) {
			UnholyAuraEvents.refreshUnholyAuraModifiers(entity);
			return;
		}

		boolean effectChanged = entity.addEffect(new MobEffectInstance(
			TimothatysTrinketsModMobEffects.UNHOLY_AURA,
			NecromancerConfig.UNHOLY_AURA_EFFECT_TICKS,
			0,
			true,
			false,
			true
		), this);
		MobEffectInstance refreshedAura = entity.getEffect(TimothatysTrinketsModMobEffects.UNHOLY_AURA);
		if (!effectChanged && refreshedAura == null) {
			UnholyAuraEvents.refreshUnholyAuraModifiers(entity);
			return;
		}
		UnholyAuraEvents.refreshUnholyAuraModifiers(entity);
	}

	private void tickUndeadificationAnimation() {
		long endGameTime = getUndeadificationCastEndGameTime();
		long gameTime = level().getGameTime();
		if (!level().isClientSide()) {
			if (endGameTime != 0L && endGameTime <= gameTime) {
				entityData.set(UNDEADIFICATION_CAST_END_GAME_TIME, 0L);
			}
			return;
		}

		if (!isAlive() || endGameTime <= gameTime) {
			clientUndeadificationEndGameTime = 0L;
			undeadificationAnimationState.stop();
			return;
		}

		if (endGameTime != clientUndeadificationEndGameTime) {
			clientUndeadificationEndGameTime = endGameTime;
			int elapsedTicks = UNDEADIFICATION_CAST_ANIMATION_TICKS
					- remainingCastTicks(endGameTime, UNDEADIFICATION_CAST_ANIMATION_TICKS);
			undeadificationAnimationState.start(tickCount - elapsedTicks);
		} else {
			undeadificationAnimationState.startIfStopped(tickCount);
		}
		NecromancerSpellParticles.spawnUndeadificationHandParticles(this);
	}

	public void startUndeadificationCast() {
		entityData.set(
				UNDEADIFICATION_CAST_END_GAME_TIME,
				level().getGameTime() + UNDEADIFICATION_CAST_ANIMATION_TICKS
		);
	}

	private long getUndeadificationCastEndGameTime() {
		return entityData.get(UNDEADIFICATION_CAST_END_GAME_TIME);
	}

	public boolean isCastingUndeadification() {
		return isAlive() && getUndeadificationCastEndGameTime() > level().getGameTime();
	}

	public boolean isUndeadificationAnimationActive() {
		return isCastingUndeadification();
	}

	public float getUndeadificationAnimationProgress() {
		int remainingTicks = remainingCastTicks(getUndeadificationCastEndGameTime(), UNDEADIFICATION_CAST_ANIMATION_TICKS);
		return 1.0F - remainingTicks / (float) UNDEADIFICATION_CAST_ANIMATION_TICKS;
	}


	public void startSummonStartCast(int ticks) {
		setSummonCast(SUMMON_CAST_STAGE_START, ticks);
	}

	public void startSummonLoop(int ticks) {
		setSummonCast(SUMMON_CAST_STAGE_LOOP, ticks);
	}

	public void startSummonEndCast(int ticks) {
		setSummonCast(SUMMON_CAST_STAGE_END, ticks);
	}

	public void clearSummonCast() {
		setSummonCast(SUMMON_CAST_STAGE_NONE, 0);
	}

	private void setSummonCast(int stage, int ticks) {
		entityData.set(SUMMON_CAST_STAGE, stage);
		entityData.set(
				SUMMON_CAST_END_GAME_TIME,
				stage == SUMMON_CAST_STAGE_NONE || ticks <= 0 ? 0L : level().getGameTime() + ticks
		);
	}

	public int getSummonCastStage() {
		return entityData.get(SUMMON_CAST_STAGE);
	}

	private long getSummonCastEndGameTime() {
		return entityData.get(SUMMON_CAST_END_GAME_TIME);
	}

	public boolean isSummoningRitual() {
		return isAlive()
				&& getSummonCastStage() != SUMMON_CAST_STAGE_NONE
				&& (!level().isClientSide() || getSummonCastEndGameTime() > level().getGameTime());
	}

	public boolean isSummonLooping() {
		return getSummonCastStage() == SUMMON_CAST_STAGE_LOOP;
	}

	public boolean isCastingAnySpell() {
		return isCastingUndeadification() || isSummoningRitual();
	}

	public void stopControlledMovement() {
		getNavigation().stop();
		getMoveControl().strafe(0.0F, 0.0F);
		setSpeed(0.0F);
	}

	@Override
	public boolean isInvertedHealAndHarm() {
		return true;
	}

	private void tickSummonCastAnimation() {
		if (!level().isClientSide()) {
			return;
		}

		int syncedStage = getSummonCastStage();
		long syncedEndGameTime = getSummonCastEndGameTime();
		if (!isAlive() || syncedStage == SUMMON_CAST_STAGE_NONE
				|| syncedEndGameTime <= level().getGameTime()) {
			stopClientSummonCastAnimation();
			return;
		}

		if (syncedStage != clientSummonCastStage || syncedEndGameTime != clientSummonCastEndGameTime) {
			startClientSummonCastAnimation(syncedStage, syncedEndGameTime);
		}
	}

	private void startClientSummonCastAnimation(int stage, long endGameTime) {
		clientSummonCastStage = stage;
		clientSummonCastEndGameTime = endGameTime;
		int remainingTicks = remainingCastTicks(endGameTime, Integer.MAX_VALUE);

		if (stage == SUMMON_CAST_STAGE_START) {
			int elapsedTicks = Math.max(0, SUMMON_START_ANIMATION_TICKS - remainingTicks);
			summonStartAnimationState.start(tickCount - elapsedTicks);
			summonLoopAnimationState.stop();
			summonEndAnimationState.stop();
		} else if (stage == SUMMON_CAST_STAGE_LOOP) {
			int elapsedTicks = Math.floorMod(-remainingTicks, SUMMON_LOOP_ANIMATION_TICKS);
			summonStartAnimationState.stop();
			summonLoopAnimationState.start(tickCount - elapsedTicks);
			summonEndAnimationState.stop();
		} else if (stage == SUMMON_CAST_STAGE_END) {
			int elapsedTicks = Math.max(0, SUMMON_END_ANIMATION_TICKS - remainingTicks);
			summonStartAnimationState.stop();
			summonLoopAnimationState.stop();
			summonEndAnimationState.start(tickCount - elapsedTicks);
		} else {
			stopClientSummonCastAnimation();
		}
	}

	private void stopClientSummonCastAnimation() {
		clientSummonCastStage = SUMMON_CAST_STAGE_NONE;
		clientSummonCastEndGameTime = 0L;
		summonStartAnimationState.stop();
		summonLoopAnimationState.stop();
		summonEndAnimationState.stop();
	}

	private int remainingCastTicks(long endGameTime, int maximum) {
		long remaining = Math.max(0L, endGameTime - level().getGameTime());
		return (int) Math.min(remaining, maximum);
	}

	private void updateSummonLoopKnockbackResistance() {
		if (level().isClientSide()) {
			return;
		}

		AttributeInstance knockbackResistance = getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (knockbackResistance == null) {
			summonLoopKnockbackResistanceActive = false;
			return;
		}

		boolean shouldHaveModifier = isSummonLooping() && isAlive();
		if (shouldHaveModifier == summonLoopKnockbackResistanceActive) {
			if (!shouldHaveModifier || knockbackResistance.getModifier(SUMMON_LOOP_KNOCKBACK_RESISTANCE_ID) != null) {
				return;
			}
		}

		knockbackResistance.removeModifier(SUMMON_LOOP_KNOCKBACK_RESISTANCE_ID);
		if (shouldHaveModifier) {
			knockbackResistance.addTransientModifier(new AttributeModifier(SUMMON_LOOP_KNOCKBACK_RESISTANCE_ID, 1.0D, AttributeModifier.Operation.ADD_VALUE));
		}
		summonLoopKnockbackResistanceActive = shouldHaveModifier;
	}

	public boolean shouldRetreat() {
		return false;
	}

	@Nullable
	public LivingEntity getRetreatTarget() {
		return null;
	}

	public String getTexture() {
		return "necromancer";
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	public static void init(RegisterSpawnPlacementsEvent event) {
		event.register(
			TimothatysTrinketsModEntities.NECROMANCER.get(),
			SpawnPlacementTypes.ON_GROUND,
			Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			NecromancerEntity::checkNecromancerSpawnRules,
			RegisterSpawnPlacementsEvent.Operation.REPLACE
		);
	}

	private static boolean checkNecromancerSpawnRules(EntityType<NecromancerEntity> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
		if (level.getDifficulty() == Difficulty.PEACEFUL) {
			return false;
		}

		if (spawnType == MobSpawnType.NATURAL) {
			long currentDay = level.getLevel().getDayTime() / TICKS_PER_DAY + 1L;
			if (currentDay < FIRST_NATURAL_SPAWN_DAY || !level.getLevel().isNight() || hasNearbyLivingNecromancer(level, pos)) {
				return false;
			}
		}

		return Monster.checkMonsterSpawnRules(entityType, level, spawnType, pos, random);
	}

	private static boolean hasNearbyLivingNecromancer(ServerLevelAccessor level, BlockPos pos) {
		double radius = NecromancerConfig.NATURAL_SPAWN_EXCLUSION_RADIUS;
		double centerX = pos.getX() + 0.5D;
		double centerY = pos.getY() + 0.5D;
		double centerZ = pos.getZ() + 0.5D;
		AABB searchBounds = new AABB(pos).inflate(radius);

		return !level.getLevel().getEntitiesOfClass(
			NecromancerEntity.class,
			searchBounds,
			necromancer -> necromancer.isAlive()
				&& necromancer.distanceToSqr(centerX, centerY, centerZ) <= NecromancerConfig.NATURAL_SPAWN_EXCLUSION_RADIUS_SQR
		).isEmpty();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 80.0D)
			.add(Attributes.ARMOR, 6.0D)
			.add(Attributes.MOVEMENT_SPEED, VANILLA_ZOMBIE_MOVEMENT_SPEED)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.65D)
			.add(Attributes.FOLLOW_RANGE, 24.0D);
	}
}
