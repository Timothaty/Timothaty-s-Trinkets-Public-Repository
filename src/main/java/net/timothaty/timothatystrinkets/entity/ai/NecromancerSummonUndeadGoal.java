package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.NecromancerEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.necromancer.NecromancerConfig;
import net.timothaty.timothatystrinkets.mechanics.necromancer.NecromancerSummonedMinionEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

public class NecromancerSummonUndeadGoal extends Goal {
	private static final int START_CAST_TICKS = 14;
	private static final int END_CAST_TICKS = 15;
	private static final int LOOP_ANIMATION_TICKS = 20 * 2;
	private static final int MIN_LOOP_CYCLES = 1;
	private static final int RANDOM_EXTRA_LOOP_CYCLES = 1;
	private static final int SUMMON_INTERVAL_TICKS = 20;
	private static final int MIN_SUMMONS_PER_WAVE = 1;
	private static final int RANDOM_SUMMONS_PER_WAVE = 1;
	private static final int POSITIVE_PLACEMENT_CACHE_TICKS = 18;
	private static final int NEGATIVE_PLACEMENT_CACHE_TICKS = 10;
	public static final double CAST_RANGE = 18.0D;
	private static final double CAST_RANGE_SQR = CAST_RANGE * CAST_RANGE;
	private static final TargetingConditions RITUAL_TARGETING = TargetingConditions.forCombat().range(CAST_RANGE);

	private static final int STAGE_IDLE = 0;
	private static final int STAGE_START = 1;
	private static final int STAGE_LOOP = 2;
	private static final int STAGE_END = 3;

	private final NecromancerEntity necromancer;
	private int stage = STAGE_IDLE;
	private int stageTicks;
	private int loopTicks;
	private int nextSummonWaveTicks;
	private LivingEntity target;
	private boolean summonedAnyMob;
	private NecromancerSummonSpot cachedRitualSpot;
	private BlockPos cachedPlacementOrigin;
	private long placementCacheExpiresAt = Long.MIN_VALUE;
	private boolean placementCacheInitialized;
	private boolean cachedPlacementOnGround;
	private boolean cachedPlacementInWater;
	private int ritualParticleTicks;
	private int cachedActiveMinionCount = -1;
	private long cachedActiveMinionCountTick = Long.MIN_VALUE;

	public NecromancerSummonUndeadGoal(NecromancerEntity necromancer) {
		this.necromancer = necromancer;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public boolean canUse() {
		LivingEntity currentTarget = necromancer.getTarget();
		if (!isTargetAvailable(currentTarget) || necromancer.shouldRetreat() || necromancer.isCastingAnySpell() || !canStartSummonCast()) {
			return false;
		}

		target = currentTarget;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return stage != STAGE_IDLE && stageTicks > 0 && necromancer.isAlive() && isTargetAvailable(target);
	}

	@Override
	public void start() {
		stage = STAGE_START;
		stageTicks = START_CAST_TICKS;
		loopTicks = LOOP_ANIMATION_TICKS * (MIN_LOOP_CYCLES + necromancer.getRandom().nextInt(RANDOM_EXTRA_LOOP_CYCLES + 1));
		nextSummonWaveTicks = SUMMON_INTERVAL_TICKS;
		summonedAnyMob = false;
		ritualParticleTicks = 0;
		necromancer.stopControlledMovement();
		necromancer.startSummonStartCast(START_CAST_TICKS);
		playSound(TimothatysTrinketsModSounds.UNDEADIFICATION_START.get(), 0.8F, 0.7F);
	}

	@Override
	public void tick() {
		if (target == null) {
			return;
		}

		necromancer.stopControlledMovement();
		necromancer.getLookControl().setLookAt(target, 30.0F, 30.0F);

		if (stage == STAGE_START) {
			tickStartStage();
		} else if (stage == STAGE_LOOP) {
			tickLoopStage();
		} else if (stage == STAGE_END) {
			tickEndStage();
		}
	}

	@Override
	public void stop() {
		boolean completed = stage == STAGE_END && stageTicks <= 0;
		boolean hadSummonedAnyMob = summonedAnyMob;
		stage = STAGE_IDLE;
		stageTicks = 0;
		loopTicks = 0;
		nextSummonWaveTicks = 0;
		target = null;
		summonedAnyMob = false;
		invalidatePlacementCache();
		ritualParticleTicks = 0;
		necromancer.clearSummonCast();

		if (completed && hadSummonedAnyMob && necromancer.isAlive()) {
			playSound(TimothatysTrinketsModSounds.UNDEADIFICATION_SUCCESFUL.get(), 0.75F, 0.8F);
		}
	}

	public boolean isReadyToApproachTarget(LivingEntity candidate) {
		return candidate != null && candidate.isAlive() && canStartSummonCast();
	}

	private void tickStartStage() {
		stageTicks--;
		if (stageTicks <= 0) {
			stage = STAGE_LOOP;
			stageTicks = loopTicks;
			nextSummonWaveTicks = SUMMON_INTERVAL_TICKS;
			necromancer.startSummonLoop(loopTicks);
			playSound(TimothatysTrinketsModSounds.UNDEADIFICATION_LOOP.get(), 0.9F, 0.75F);
		}
	}

	private void tickLoopStage() {
		stageTicks--;
		nextSummonWaveTicks--;
		spawnRitualBlockParticles();

		if (nextSummonWaveTicks <= 0) {
			nextSummonWaveTicks = SUMMON_INTERVAL_TICKS;
			if (summonWave()) {
				summonedAnyMob = true;
			}
		}

		if (stageTicks <= 0) {
			stage = STAGE_END;
			stageTicks = END_CAST_TICKS;
			necromancer.startSummonEndCast(END_CAST_TICKS);
			playSound(TimothatysTrinketsModSounds.UNDEADIFICATION_FAILED.get(), 0.65F, 0.65F);
		}
	}

	private void tickEndStage() {
		stageTicks--;
	}

	private boolean isTargetAvailable(LivingEntity candidate) {
		if (candidate == null || !candidate.isAlive()) {
			return false;
		}

		if (necromancer.distanceToSqr(candidate) > CAST_RANGE_SQR) {
			return false;
		}

		Level level = necromancer.level();
		return level.isClientSide() || RITUAL_TARGETING.test(necromancer, candidate);
	}

	private boolean summonWave() {
		if (!(necromancer.level() instanceof ServerLevel serverLevel)) {
			return false;
		}

		int activeMinions = getCachedActiveMinionCount(serverLevel, true);
		if (activeMinions >= NecromancerConfig.SUMMONED_MINION_CAP) {
			return false;
		}

		RandomSource random = necromancer.getRandom();
		boolean spawnedAny = false;
		int summonCount = MIN_SUMMONS_PER_WAVE + random.nextInt(RANDOM_SUMMONS_PER_WAVE + 1);
		summonCount = Math.min(summonCount, NecromancerConfig.SUMMONED_MINION_CAP - activeMinions);
		for (int i = 0; i < summonCount; i++) {
			NecromancerSummonSpot summonSpot = getCachedSummonSpot(serverLevel, random);
			if (summonSpot != null && spawnSummonedMob(serverLevel, summonSpot, random)) {
				invalidatePlacementCache();
				cachedActiveMinionCount++;
				spawnedAny = true;
			}
		}
		return spawnedAny;
	}

	private boolean canStartSummonCast() {
		if (!(necromancer.level() instanceof ServerLevel serverLevel)) {
			return false;
		}

		return getCachedActiveMinionCount(serverLevel, false) == 0
			&& getCachedSummonSpot(serverLevel, necromancer.getRandom()) != null
			&& NecromancerSummonTypes.hasAvailableSummonType(serverLevel);
	}

	private boolean spawnSummonedMob(ServerLevel serverLevel, NecromancerSummonSpot summonSpot, RandomSource random) {
		EntityType<?> entityType = NecromancerSummonTypes.pickSummonType(serverLevel, random);
		if (entityType == null || !(entityType.create(serverLevel) instanceof Mob summoned)) {
			return false;
		}

		BlockPos groundPos = summonSpot.groundPos();
		summoned.moveTo(groundPos.getX() + 0.5D, summonSpot.spawnY(), groundPos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
		DifficultyInstance difficulty = serverLevel.getCurrentDifficultyAt(groundPos.above());
		summoned.finalizeSpawn(serverLevel, difficulty, MobSpawnType.MOB_SUMMONED, (SpawnGroupData) null);
		NecromancerSummonEquipment.apply(serverLevel, summoned);
		NecromancerSummonedMinionEvents.markSummonedMob(summoned, necromancer);
		if (!serverLevel.addFreshEntity(summoned)) {
			return false;
		}
		playSound(TimothatysTrinketsModSounds.LAND_BLIGHTED.get(), 0.65F, 0.75F + random.nextFloat() * 0.25F);
		return true;
	}

	private void spawnRitualBlockParticles() {
		ritualParticleTicks++;
		if (ritualParticleTicks < NecromancerConfig.SUMMON_RITUAL_PARTICLE_INTERVAL_TICKS) {
			return;
		}
		ritualParticleTicks = 0;

		if (!(necromancer.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		RandomSource random = necromancer.getRandom();
		NecromancerSummonSpot summonSpot = getCachedSummonSpot(serverLevel, random);
		if (summonSpot == null) {
			return;
		}

		NecromancerSummonParticles.spawnRitualBlockParticles(serverLevel, summonSpot);
	}

	private NecromancerSummonSpot getCachedSummonSpot(ServerLevel serverLevel, RandomSource random) {
		boolean onGround = necromancer.onGround();
		boolean inWater = necromancer.isInWaterOrBubble();
		if (!onGround || inWater) {
			invalidatePlacementCache();
			return null;
		}

		BlockPos currentOrigin = necromancer.blockPosition();
		long gameTime = serverLevel.getGameTime();
		boolean cacheContextMatches = placementCacheInitialized
				&& currentOrigin.equals(cachedPlacementOrigin)
				&& cachedPlacementOnGround == onGround
				&& cachedPlacementInWater == inWater;
		if (cacheContextMatches && gameTime < placementCacheExpiresAt) {
			if (cachedRitualSpot == null
					|| NecromancerSummonPlacement.isSummonSpotStillValid(necromancer, serverLevel, cachedRitualSpot)) {
				return cachedRitualSpot;
			}
		}

		cachedRitualSpot = NecromancerSummonPlacement.findSummonSpot(necromancer, serverLevel, random);
		cachedPlacementOrigin = currentOrigin.immutable();
		cachedPlacementOnGround = onGround;
		cachedPlacementInWater = inWater;
		placementCacheInitialized = true;
		placementCacheExpiresAt = gameTime + (cachedRitualSpot == null
				? NEGATIVE_PLACEMENT_CACHE_TICKS
				: POSITIVE_PLACEMENT_CACHE_TICKS);
		return cachedRitualSpot;
	}

	private void invalidatePlacementCache() {
		cachedRitualSpot = null;
		cachedPlacementOrigin = null;
		placementCacheExpiresAt = Long.MIN_VALUE;
		placementCacheInitialized = false;
		cachedPlacementOnGround = false;
		cachedPlacementInWater = false;
	}

	private int getCachedActiveMinionCount(ServerLevel serverLevel, boolean forceRefresh) {
		long gameTime = serverLevel.getGameTime();
		if (forceRefresh
			|| cachedActiveMinionCount < 0
			|| gameTime - cachedActiveMinionCountTick >= NecromancerConfig.SUMMONED_MINION_COUNT_CACHE_TICKS) {
			cachedActiveMinionCount = NecromancerSummonedMinionEvents.countActiveMinions(necromancer, serverLevel);
			cachedActiveMinionCountTick = gameTime;
		}

		return cachedActiveMinionCount;
	}

	private void playSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
		necromancer.level().playSound(null, necromancer.blockPosition(), sound, SoundSource.HOSTILE, volume, pitch);
	}
}
