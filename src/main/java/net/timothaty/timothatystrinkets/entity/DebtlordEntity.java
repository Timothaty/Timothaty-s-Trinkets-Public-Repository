package net.timothaty.timothatystrinkets.entity;

import net.timothaty.timothatystrinkets.entity.ai.DebtlordStompGoal;
import net.timothaty.timothatystrinkets.entity.ai.DebtlordApproachTargetGoal;
import net.timothaty.timothatystrinkets.entity.ai.DebtlordHornsGoal;
import net.timothaty.timothatystrinkets.entity.ai.DebtlordFearGoal;
import net.timothaty.timothatystrinkets.entity.ai.DebtlordDisarmAbility;
import net.timothaty.timothatystrinkets.entity.ai.DebtlordClawGoal;
import net.timothaty.timothatystrinkets.entity.ai.DebtlordClawFollowupQueue;
import net.timothaty.timothatystrinkets.entity.ai.DebtlordDesolationGoal;
import net.timothaty.timothatystrinkets.entity.ai.DebtlordChainsGoal;
import net.timothaty.timothatystrinkets.entity.ai.DebtlordFingerOfDeathGoal;
import net.timothaty.timothatystrinkets.entity.ai.DebtlordLeaveWaterGoal;
import net.timothaty.timothatystrinkets.entity.ai.DebtlordAntiPillarGoal;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.dialogue.DialogueHudSender;
import net.timothaty.timothatystrinkets.mechanics.debtlord.DebtlordSummonManager;

import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.BossEvent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModEntities;
import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DebtlordEntity extends Monster {
	public static final int DEATH_ANIMATION_DURATION_TICKS = 10 * 20;
	private static final TagKey<Block> DEBTLORD_BREAKABLE_BLOCKS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "debtlord_blocks"));
	private static final double CLAW_PATH_PUSH_RADIUS = 0.85D;
	private static final double CLAW_PATH_SIDE_PUSH = 0.62D;
	private static final double CLAW_PATH_FORWARD_PUSH = 0.16D;
	private static final double CLAW_PATH_GROUND_LIFT = 0.18D;
	private static final double CLAW_PATH_AIR_LIFT = 0.05D;
	private static final double CLAW_MAX_STEP_UP = 1.05D;
	private static final double FINGER_OF_DEATH_SOURCE_TIP_OFFSET = 0.34D;
	private static final float MAGIC_DAMAGE_MULTIPLIER = 0.75F;
	private static final float MOVEMENT_ANIMATION_BLEND_STEP = 0.1F;
	private static final int BOSS_BAR_UPDATE_INTERVAL_TICKS = 4;
	private static final int WALK_ANIMATION_LENGTH_TICKS = 20;
	private static final int WALK_RIGHT_HOOF_STEP_TICK = 10;
	private static final double WALK_STEP_MOVEMENT_THRESHOLD = 1.0E-5D;
	private static final double DIRECTOR_HORNS_RANGE = 4.0D;
	private static final double DIRECTOR_HORNS_RANGE_SQR = DIRECTOR_HORNS_RANGE * DIRECTOR_HORNS_RANGE;
	private static final double DIRECTOR_CLAW_RANGE = 11.0D;
	private static final double DIRECTOR_CLAW_RANGE_SQR = DIRECTOR_CLAW_RANGE * DIRECTOR_CLAW_RANGE;
	private static final double DIRECTOR_PHASE_ONE_CHAIN_RANGE = 14.0D;
	private static final double DIRECTOR_PHASE_TWO_CHAIN_RANGE = 13.0D;
	private static final double DIRECTOR_PHASE_THREE_CHAIN_RANGE = 11.0D;
	private static final double DIRECTOR_PHASE_TWO_FINGER_MIN_RANGE = 9.0D;
	private static final double DIRECTOR_PHASE_THREE_FINGER_MIN_RANGE = 7.0D;
	private static final double DIRECTOR_HIGH_TARGET_DELTA = 3.0D;
	private static final float DIRECTOR_WATER_CHAIN_PREFERENCE = 0.65F;
	private static final byte STOMP_ANIMATION_EVENT = 70;
	private static final byte HORNS_ANIMATION_EVENT = 71;
	private static final byte FEAR_ANIMATION_EVENT = 72;
	private static final byte DISARM_ANIMATION_EVENT = 73;
	private static final byte CLAW_ANIMATION_EVENT = 74;
	private static final byte DESOLATION_ANIMATION_EVENT = 75;
	private static final byte CLAW_SECOND_ANIMATION_EVENT = 76;
	private static final byte CLAW_THIRD_ANIMATION_EVENT = 77;
	private static final byte CHAIN_ANIMATION_EVENT = 78;
	private static final byte CHAIN_SUCCESS_ANIMATION_EVENT = 79;
	private static final byte CHAIN_FAILED_ANIMATION_EVENT = 80;
	private static final byte FINGER_OF_DEATH_CHARGE_ANIMATION_EVENT = 81;
	private static final byte FINGER_OF_DEATH_IDLE_ANIMATION_EVENT = 82;
	private static final byte FINGER_OF_DEATH_SHOT_ANIMATION_EVENT = 83;
	private static final byte APPEARANCE_ANIMATION_EVENT = 84;
	private static final byte TALKING_ANIMATION_EVENT = 85;
	private static final byte STOMP_STOP_ANIMATION_EVENT = 86;
	private static final byte FAST_STOMP_ANIMATION_EVENT = 87;

	private static final int ALTAR_SUMMON_STATE_NONE = 0;
	private static final int ALTAR_SUMMON_STATE_APPEARANCE = 1;
	private static final int ALTAR_SUMMON_STATE_TALKING = 2;
	private static final int ALTAR_SUMMON_STATE_PRE_FIGHT = 3;
	private static final int ALTAR_SUMMON_STATE_FIGHT = 4;
	private static final int ALTAR_SUMMON_STATE_DISMISSING = 5;
	private static final int APPEARANCE_DURATION_TICKS = 40;
	private static final int TALKING_DURATION_TICKS = 40;
	private static final int PRE_FIGHT_DELAY_TICKS = 40;
	private static final int DISMISS_DURATION_TICKS = 40;

	public static final int CHAIN_PHASE_NONE = 0;
	public static final int CHAIN_PHASE_CAST = 1;
	public static final int CHAIN_PHASE_SUCCESS = 2;
	public static final int CHAIN_PHASE_FAILED = 3;
	public static final int FINGER_OF_DEATH_PHASE_NONE = 0;
	public static final int FINGER_OF_DEATH_PHASE_CHARGE = 1;
	public static final int FINGER_OF_DEATH_PHASE_IDLE = 2;
	public static final int FINGER_OF_DEATH_PHASE_SHOT = 3;

	public enum CombatIntent {
		PRESSURE,
		HORNS,
		CLAWS,
		STOMP,
		CHAINS,
		FINGER_OF_DEATH
	}

	public static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<Integer> ANIM = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> STOMP_TICKS = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> STOMP_FAST = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> ANTI_PILLAR_PHASE = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> ANTI_PILLAR_TICKS = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> HORNS_TICKS = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> FEAR_TICKS = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DISARM_TICKS = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> CLAW_TICKS = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DESOLATION_TICKS = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> CLAW_CAST_INDEX = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> CHAIN_TICKS = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> CHAIN_PHASE = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> CHAIN_TARGET_ID = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> FINGER_OF_DEATH_TICKS = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> FINGER_OF_DEATH_PHASE = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> FINGER_OF_DEATH_TARGET_ID = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> FINGER_OF_DEATH_LASER_YAW = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> FINGER_OF_DEATH_LASER_PITCH = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Vector3f> FINGER_OF_DEATH_LASER_TARGET = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.VECTOR3);
	private static final EntityDataAccessor<Boolean> ALTAR_SUMMONED = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<BlockPos> ALTAR_POS = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.BLOCK_POS);
	private static final EntityDataAccessor<Integer> ALTAR_SUMMON_STATE = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> ALTAR_SUMMON_TICKS = SynchedEntityData.defineId(DebtlordEntity.class, EntityDataSerializers.INT);
	public final AnimationState appearanceAnimationState = new AnimationState();
	public final AnimationState talkingAnimationState = new AnimationState();
	public final AnimationState idleAnimationState = new AnimationState();
	public final AnimationState walkAnimationState = new AnimationState();
	public final AnimationState stompAnimationState = new AnimationState();
	public final AnimationState hornsAnimationState = new AnimationState();
	public final AnimationState fearAnimationState = new AnimationState();
	public final AnimationState disarmAnimationState = new AnimationState();
	public final AnimationState clawAnimationState = new AnimationState();
	public final AnimationState desolationAnimationState = new AnimationState();
	public final AnimationState chainAnimationState = new AnimationState();
	public final AnimationState chainSuccessAnimationState = new AnimationState();
	public final AnimationState chainFailedAnimationState = new AnimationState();
	public final AnimationState fingerOfDeathChargeAnimationState = new AnimationState();
	public final AnimationState fingerOfDeathIdleAnimationState = new AnimationState();
	public final AnimationState fingerOfDeathShotAnimationState = new AnimationState();
	public final AnimationState deathAnimationState = new AnimationState();
	private float previousMovementAnimationBlend;
	private float movementAnimationBlend;
	private int lastWalkStepPhase = -1;
	private int clientStompAnimationTicks;
	private boolean clientStompFast;
	private boolean clientStompNeedsTimelineAnchor;
	private int lastSyncedStompTicks;
	private int clientHornsAnimationTicks;
	private int lastSyncedHornsTicks;
	private int clientFearAnimationTicks;
	private int lastSyncedFearTicks;
	private int clientDisarmAnimationTicks;
	private int lastSyncedDisarmTicks;
	private int clientClawAnimationTicks;
	private int lastSyncedClawTicks;
	private int clientClawCastIndex = 1;
	private int clientDesolationAnimationTicks;
	private int lastSyncedDesolationTicks;
	private int clientChainAnimationTicks;
	private int lastSyncedChainTicks;
	private int clientChainPhase;
	private int clientFingerOfDeathAnimationTicks;
	private int lastSyncedFingerOfDeathTicks;
	private int clientFingerOfDeathPhase;
	private int clientAppearanceAnimationTicks;
	private int lastSyncedAppearanceTicks;
	private int clientTalkingAnimationTicks;
	private int lastSyncedTalkingTicks;
	private LivingEntity hornsTarget;
	private LivingEntity disarmTarget;
	private LivingEntity chainTarget;
	private LivingEntity fingerOfDeathTarget;
	private final DebtlordClawFollowupQueue clawFollowups = new DebtlordClawFollowupQueue();
	private DebtlordAntiPillarGoal antiPillarGoal;
	private boolean desolationAbilityPending;
	private boolean desolationAbilityUsed;
	private boolean secondPhaseDialoguePlayed;
	private LivingEntity fearTriggerTarget;
	private LivingEntity fearTrackingTarget;
	private int fearHitCount;
	private long lastFearHitGameTime = -100000L;
	private long fearCooldownUntil;
	private boolean fearAbilityPending;
	private String textureBeforeFear;
	private double abilityAnchorX;
	private double abilityAnchorZ;
	private float abilityLockedYRot;
	private float abilityLockedXRot;
	private float abilityLockedBodyYRot;
	private float abilityLockedHeadYRot;
	private CombatIntent combatIntent = CombatIntent.PRESSURE;
	private long nextCombatDecisionGameTime;
	private final ServerBossEvent bossEvent;
	private UUID altarSummonerUuid;
	private boolean altarOutcomeHandled;

	public DebtlordEntity(EntityType<DebtlordEntity> type, Level world) {
		super(type, world);
		xpReward = 63;
		setNoAi(false);
		setPersistenceRequired();
		setPathfindingMalus(PathType.WATER, -1.0F);
		setPathfindingMalus(PathType.WATER_BORDER, 16.0F);
		bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
	}

	@Override
	public boolean isAlliedTo(Entity entity) {
		if (entity == this || super.isAlliedTo(entity))
			return true;
		if (entity instanceof LivingEntity livingEntity)
			return livingEntity.getType().is(EntityTypeTags.UNDEAD)
				|| livingEntity.hasEffect(TimothatysTrinketsModMobEffects.UNDEADIFICATION);
		return false;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(TEXTURE, "debtlord");
		builder.define(ANIM, 0);
		builder.define(STOMP_TICKS, 0);
		builder.define(STOMP_FAST, false);
		builder.define(ANTI_PILLAR_PHASE, DebtlordAntiPillarGoal.PHASE_NONE);
		builder.define(ANTI_PILLAR_TICKS, 0);
		builder.define(HORNS_TICKS, 0);
		builder.define(FEAR_TICKS, 0);
		builder.define(DISARM_TICKS, 0);
		builder.define(CLAW_TICKS, 0);
		builder.define(DESOLATION_TICKS, 0);
		builder.define(CLAW_CAST_INDEX, 1);
		builder.define(CHAIN_TICKS, 0);
		builder.define(CHAIN_PHASE, CHAIN_PHASE_NONE);
		builder.define(CHAIN_TARGET_ID, -1);
		builder.define(FINGER_OF_DEATH_TICKS, 0);
		builder.define(FINGER_OF_DEATH_PHASE, FINGER_OF_DEATH_PHASE_NONE);
		builder.define(FINGER_OF_DEATH_TARGET_ID, -1);
		builder.define(FINGER_OF_DEATH_LASER_YAW, 0.0F);
		builder.define(FINGER_OF_DEATH_LASER_PITCH, 0.0F);
		builder.define(FINGER_OF_DEATH_LASER_TARGET, new Vector3f());
		builder.define(ALTAR_SUMMONED, false);
		builder.define(ALTAR_POS, BlockPos.ZERO);
		builder.define(ALTAR_SUMMON_STATE, ALTAR_SUMMON_STATE_NONE);
		builder.define(ALTAR_SUMMON_TICKS, 0);
	}

	public void setTexture(String texture) {
		this.entityData.set(TEXTURE, texture);
	}

	public String getTexture() {
		return this.entityData.get(TEXTURE);
	}

	public DebtlordPhase getPhase() {
		float maxHealth = getMaxHealth();
		if (maxHealth <= 0.0F)
			return DebtlordPhase.PHASE_ONE;

		float healthRatio = getHealth() / maxHealth;
		if (healthRatio <= 0.25F)
			return DebtlordPhase.PHASE_THREE;
		if (healthRatio <= 0.5F)
			return DebtlordPhase.PHASE_TWO;
		return DebtlordPhase.PHASE_ONE;
	}

	public boolean isEnraged() {
		return getPhase() != DebtlordPhase.PHASE_ONE;
	}

	private void tickSecondPhaseDialogue() {
		if (secondPhaseDialoguePlayed || getPhase() == DebtlordPhase.PHASE_ONE)
			return;
		if (isAltarSummoned() && getAltarSummonState() != ALTAR_SUMMON_STATE_FIGHT)
			return;

		secondPhaseDialoguePlayed = true;
		DialogueHudSender.playDebtlordSecondPhaseLine(this, getSummonerPlayer());
	}

	public void startAltarSummon(ServerPlayer summoner, BlockPos altarPos) {
		if (level().isClientSide() || summoner == null || altarPos == null)
			return;

		altarSummonerUuid = summoner.getUUID();
		altarOutcomeHandled = false;
		entityData.set(ALTAR_SUMMONED, true);
		entityData.set(ALTAR_POS, altarPos.immutable());
		setAltarSummonState(ALTAR_SUMMON_STATE_APPEARANCE, APPEARANCE_DURATION_TICKS);
		setTarget(null);
		setHealth(getMaxHealth());
		setNoAi(true);
		setInvulnerable(true);
		setPersistenceRequired();
		getNavigation().stop();
		setDeltaMovement(Vec3.ZERO);
		turnToward(summoner);
		hideBossBar();
		DebtlordSummonManager.registerAltarLock(this);
		level().broadcastEntityEvent(this, APPEARANCE_ANIMATION_EVENT);
	}

	public boolean isAltarSummoned() {
		return entityData.get(ALTAR_SUMMONED);
	}

	public boolean isAltarOutcomeHandled() {
		return altarOutcomeHandled;
	}

	public void markAltarOutcomeHandled() {
		altarOutcomeHandled = true;
	}

	public boolean isSummonedBy(ServerPlayer player) {
		return player != null && altarSummonerUuid != null && altarSummonerUuid.equals(player.getUUID());
	}

	public ServerPlayer getSummonerPlayer() {
		if (!(level() instanceof ServerLevel serverLevel) || altarSummonerUuid == null)
			return null;
		return serverLevel.getServer().getPlayerList().getPlayer(altarSummonerUuid);
	}

	public BlockPos getBoundAltarPos() {
		return isAltarSummoned() ? entityData.get(ALTAR_POS) : null;
	}

	public boolean isBoundToAltar(BlockPos altarPos) {
		return isAltarSummoned() && altarPos != null && entityData.get(ALTAR_POS).equals(altarPos);
	}

	public boolean blocksBoundAltar() {
		return isAltarSummoned()
			&& !isRemoved()
			&& isAlive()
			&& !altarOutcomeHandled
			&& getAltarSummonState() != ALTAR_SUMMON_STATE_NONE;
	}

	public boolean hasActiveAltarVfx() {
		return isAltarSummoned()
			&& !isRemoved()
			&& isAlive()
			&& !altarOutcomeHandled
			&& getAltarSummonState() != ALTAR_SUMMON_STATE_NONE
			&& getAltarSummonState() != ALTAR_SUMMON_STATE_DISMISSING;
	}

	public boolean isAltarAppearanceVfxActive() {
		return isAltarSummoned()
			&& !isRemoved()
			&& isAlive()
			&& !altarOutcomeHandled
			&& getAltarSummonState() == ALTAR_SUMMON_STATE_APPEARANCE;
	}

	public boolean isAltarFightVfxActive() {
		return isAltarSummoned()
			&& !isRemoved()
			&& isAlive()
			&& !altarOutcomeHandled
			&& getAltarSummonState() == ALTAR_SUMMON_STATE_FIGHT;
	}

	public int getAltarSummonVisualTicks() {
		return isAltarSummoned() ? getAltarSummonTicks() : 0;
	}

	public boolean isAltarFightActive() {
		return !isAltarSummoned() || getAltarSummonState() == ALTAR_SUMMON_STATE_FIGHT;
	}

	public boolean isAltarIntroOrDismissalActive() {
		return isAltarSummoned() && getAltarSummonState() != ALTAR_SUMMON_STATE_FIGHT && getAltarSummonState() != ALTAR_SUMMON_STATE_NONE;
	}

	private int getAltarSummonState() {
		return entityData.get(ALTAR_SUMMON_STATE);
	}

	private int getAltarSummonTicks() {
		return entityData.get(ALTAR_SUMMON_TICKS);
	}

	private void setAltarSummonState(int state, int ticks) {
		if (!level().isClientSide()) {
			entityData.set(ALTAR_SUMMON_STATE, state);
			entityData.set(ALTAR_SUMMON_TICKS, Math.max(0, ticks));
		}
	}

	private void setAltarSummonTicks(int ticks) {
		if (!level().isClientSide())
			entityData.set(ALTAR_SUMMON_TICKS, Math.max(0, ticks));
	}

	private boolean shouldShowBossBar() {
		return !isAltarSummoned() || getAltarSummonState() == ALTAR_SUMMON_STATE_FIGHT;
	}

	private void hideBossBar() {
		bossEvent.removeAllPlayers();
	}

	private void showBossBarToNearbyPlayers() {
		if (!(level() instanceof ServerLevel serverLevel) || !shouldShowBossBar())
			return;

		for (ServerPlayer player : serverLevel.players()) {
			if (player.distanceToSqr(this) <= 96.0D * 96.0D)
				bossEvent.addPlayer(player);
		}
	}

	public float getAltarSummonAlpha(float partialTick) {
		if (!isAltarSummoned())
			return 1.0F;

		int state = getAltarSummonState();
		int ticks = getAltarSummonTicks();
		if (state == ALTAR_SUMMON_STATE_APPEARANCE) {
			float elapsed = APPEARANCE_DURATION_TICKS - ticks + partialTick;
			return Mth.clamp(elapsed / (float) APPEARANCE_DURATION_TICKS, 0.0F, 1.0F);
		}
		if (state == ALTAR_SUMMON_STATE_DISMISSING) {
			float remaining = ticks - partialTick;
			return Mth.clamp(remaining / (float) DISMISS_DURATION_TICKS, 0.0F, 1.0F);
		}
		return 1.0F;
	}

	public float getRenderAlpha(float partialTick) {
		return Math.min(getAltarSummonAlpha(partialTick), getAntiPillarFadeAlpha(partialTick));
	}

	private float getAntiPillarFadeAlpha(float partialTick) {
		int phase = getAntiPillarPhase();
		int ticks = getAntiPillarTicks();
		if (phase == DebtlordAntiPillarGoal.PHASE_FADE_OUT) {
			float progress = (DebtlordAntiPillarGoal.FADE_OUT_TICKS - ticks + partialTick) / DebtlordAntiPillarGoal.FADE_OUT_TICKS;
			return 1.0F - (float) Mth.smoothstep(Mth.clamp(progress, 0.0F, 1.0F));
		}
		if (phase == DebtlordAntiPillarGoal.PHASE_REPOSITION)
			return 0.0F;
		if (phase == DebtlordAntiPillarGoal.PHASE_FADE_IN) {
			float progress = (DebtlordAntiPillarGoal.FADE_IN_TICKS - ticks + partialTick) / DebtlordAntiPillarGoal.FADE_IN_TICKS;
			return (float) Mth.smoothstep(Mth.clamp(progress, 0.0F, 1.0F));
		}
		return 1.0F;
	}

	public float getAltarAppearanceAnimationWeight(float partialTick) {
		if (!isAltarSummoned() || getAltarSummonState() != ALTAR_SUMMON_STATE_APPEARANCE)
			return 0.0F;

		return 1.0F;
	}

	public float getAltarTalkingAnimationWeight(float partialTick) {
		if (!isAltarSummoned() || getAltarSummonState() != ALTAR_SUMMON_STATE_TALKING)
			return 0.0F;

		return 1.0F;
	}

	public float getAltarIdleAnimationWeight(float partialTick) {
		if (!isAltarSummoned())
			return 0.0F;

		int state = getAltarSummonState();
		if (state == ALTAR_SUMMON_STATE_PRE_FIGHT)
			return 1.0F;
		if (state == ALTAR_SUMMON_STATE_DISMISSING)
			return 1.0F;

		return 0.0F;
	}

	public boolean isAppearanceAnimationActive() {
		return isAltarSummoned()
			&& getAltarSummonState() == ALTAR_SUMMON_STATE_APPEARANCE
			&& (level().isClientSide() ? clientAppearanceAnimationTicks > 0 : getAltarSummonTicks() > 0);
	}

	public boolean isTalkingAnimationActive() {
		return isAltarSummoned()
			&& getAltarSummonState() == ALTAR_SUMMON_STATE_TALKING
			&& (level().isClientSide() ? clientTalkingAnimationTicks > 0 : getAltarSummonTicks() > 0);
	}

	public void startAltarDismissal() {
		if (level().isClientSide())
			return;

		setTarget(null);
		setLastHurtByMob(null);
		getNavigation().stop();
		setDeltaMovement(Vec3.ZERO);
		setNoAi(true);
		setInvulnerable(true);
		clearAllAbilityCasts();
		hideBossBar();
		setAltarSummonState(ALTAR_SUMMON_STATE_DISMISSING, DISMISS_DURATION_TICKS);
	}

	@Override
	public void tick() {
		super.tick();
		if (!isAlive()) {
			if (level().isClientSide()) {
				updateClientDeathAnimation();
			} else {
				getNavigation().stop();
				setDeltaMovement(0.0D, Math.min(0.0D, getDeltaMovement().y), 0.0D);
				textureBeforeFear = null;
				if (!"debtlord_dead".equals(getTexture()))
					setTexture("debtlord_dead");
				if (shouldShowBossBar() && tickCount % BOSS_BAR_UPDATE_INTERVAL_TICKS == 0)
					updateBossEvent();
			}
			return;
		}

		deathAnimationState.stop();
		if (isAltarIntroOrDismissalActive()) {
			if (level().isClientSide()) {
				updateClientAltarSummonAnimation();
			} else {
				tickAltarSummonState();
			}
			return;
		}
		if (!level().isClientSide() && isAltarSummoned())
			DebtlordSummonManager.registerAltarLock(this);
		if (!level().isClientSide() && !desolationAbilityUsed && isEnraged())
			desolationAbilityPending = true;
		if (!level().isClientSide())
			tickSecondPhaseDialogue();
		if (!level().isClientSide() && shouldShowBossBar() && tickCount % BOSS_BAR_UPDATE_INTERVAL_TICKS == 0)
			updateBossEvent();
		if (level().isClientSide()) {
			updateMovementAnimationStates();
			updateClientStompAnimation();
			updateClientHornsAnimation();
			updateClientFearAnimation();
			updateClientDisarmAnimation();
			updateClientClawAnimation();
			updateClientDesolationAnimation();
			updateClientChainAnimation();
			updateClientFingerOfDeathAnimation();
			syncClientFingerOfDeathBodyRotation();
		} else {
			tickCombatDirector();
			tickSynchronizedWalkSteps();
			tickServerAbilities();
		}
	}

	private void updateBossEvent() {
		bossEvent.setProgress(Math.max(0.0F, getHealth() / getMaxHealth()));
		bossEvent.setName(getDisplayName());
	}

	private void tickAltarSummonState() {
		DebtlordSummonManager.registerAltarLock(this);
		getNavigation().stop();
		setTarget(null);
		setLastHurtByMob(null);
		setNoAi(true);
		setInvulnerable(true);
		setDeltaMovement(Vec3.ZERO);
		hideBossBar();

		ServerPlayer summoner = getSummonerPlayer();
		if (summoner != null && summoner.isAlive())
			turnToward(summoner);

		int state = getAltarSummonState();
		int ticks = getAltarSummonTicks();
		if (state == ALTAR_SUMMON_STATE_APPEARANCE) {
			if (ticks <= 1) {
				setAltarSummonState(ALTAR_SUMMON_STATE_TALKING, TALKING_DURATION_TICKS);
				level().broadcastEntityEvent(this, TALKING_ANIMATION_EVENT);
				DialogueHudSender.playDebtlordSummonLine(this, summoner);
			} else {
				setAltarSummonTicks(ticks - 1);
			}
			return;
		}

		if (state == ALTAR_SUMMON_STATE_TALKING) {
			if (ticks <= 1) {
				setAltarSummonState(ALTAR_SUMMON_STATE_PRE_FIGHT, PRE_FIGHT_DELAY_TICKS);
			} else {
				setAltarSummonTicks(ticks - 1);
			}
			return;
		}

		if (state == ALTAR_SUMMON_STATE_PRE_FIGHT) {
			if (ticks <= 1) {
				startAltarFight(summoner);
			} else {
				setAltarSummonTicks(ticks - 1);
			}
			return;
		}

		if (state == ALTAR_SUMMON_STATE_DISMISSING) {
			if (ticks <= 1) {
				DebtlordSummonManager.releaseAltarLock(this);
				remove(Entity.RemovalReason.DISCARDED);
			} else {
				setAltarSummonTicks(ticks - 1);
			}
		}
	}

	private void startAltarFight(ServerPlayer summoner) {
		setAltarSummonState(ALTAR_SUMMON_STATE_FIGHT, 0);
		setNoAi(false);
		setInvulnerable(false);
		setTarget(summoner != null && summoner.isAlive() ? summoner : null);
		showBossBarToNearbyPlayers();
		updateBossEvent();
	}

	private void updateClientAltarSummonAnimation() {
		idleAnimationState.startIfStopped(tickCount);
		walkAnimationState.stop();
		movementAnimationBlend = 0.0F;
		previousMovementAnimationBlend = 0.0F;
		updateClientAppearanceAnimation();
		updateClientTalkingAnimation();
	}

	private void tickCombatDirector() {
		if (level().isClientSide())
			return;
		if (isUsingAbility())
			return;

		LivingEntity target = getTarget();
		if (!isValidCombatDirectorTarget(target)) {
			combatIntent = CombatIntent.PRESSURE;
			nextCombatDecisionGameTime = level().getGameTime() + getCombatDirectorIntervalTicks();
			return;
		}

		long gameTime = level().getGameTime();
		if (gameTime < nextCombatDecisionGameTime)
			return;

		combatIntent = chooseCombatIntent(target);
		nextCombatDecisionGameTime = gameTime + getCombatDirectorIntervalTicks();
	}

	private int getCombatDirectorIntervalTicks() {
		return switch (getPhase()) {
			case PHASE_THREE -> 4;
			case PHASE_TWO -> 6;
			case PHASE_ONE -> 12;
		};
	}

	private CombatIntent chooseCombatIntent(LivingEntity target) {
		DebtlordPhase phase = getPhase();
		double distanceSqr = distanceToSqr(target);
		double feetDelta = target.getBoundingBox().minY - getBoundingBox().minY;
		boolean bossInWater = isTouchingWaterForBossLogic();
		boolean targetInWater = isEntityTouchingWater(target);
		boolean targetIsHigh = feetDelta >= DIRECTOR_HIGH_TARGET_DELTA;

		if (targetIsHigh || distanceSqr >= getDirectorChainRangeSqr(phase))
			return CombatIntent.CHAINS;
		if (targetInWater && getRandom().nextFloat() < DIRECTOR_WATER_CHAIN_PREFERENCE)
			return CombatIntent.CHAINS;

		if (!bossInWater && !targetInWater && distanceSqr <= DIRECTOR_CLAW_RANGE_SQR && canClawBreakThroughToward(target))
			return CombatIntent.CLAWS;

		if (phase != DebtlordPhase.PHASE_ONE && distanceSqr >= getDirectorFingerMinRangeSqr(phase) && hasLineOfSight(target)) {
			float fingerChance = phase == DebtlordPhase.PHASE_THREE ? 0.38F : 0.24F;
			if (getRandom().nextFloat() < fingerChance)
				return CombatIntent.FINGER_OF_DEATH;
		}

		if (distanceSqr <= DIRECTOR_HORNS_RANGE_SQR) {
			float roll = getRandom().nextFloat();
			float hornsChance = getCloseHornsChance(phase);
			float stompChance = getCloseStompChance(phase);
			if (roll < hornsChance)
				return CombatIntent.HORNS;
			if (!bossInWater && !targetInWater && roll < hornsChance + stompChance)
				return CombatIntent.STOMP;
			return !bossInWater && !targetInWater ? CombatIntent.CLAWS : CombatIntent.PRESSURE;
		}

		double stompRadius = DebtlordStompGoal.EFFECT_RADIUS + (phase != DebtlordPhase.PHASE_ONE ? 7.0D : 0.0D);
		if (!bossInWater && !targetInWater && distanceSqr <= stompRadius * stompRadius && getRandom().nextFloat() < getStompIntentChance(phase))
			return CombatIntent.STOMP;

		if (!bossInWater && !targetInWater && distanceSqr <= DIRECTOR_CLAW_RANGE_SQR && getRandom().nextFloat() < getClawIntentChance(phase))
			return CombatIntent.CLAWS;

		return CombatIntent.PRESSURE;
	}

	private static double getDirectorChainRangeSqr(DebtlordPhase phase) {
		double range = switch (phase) {
			case PHASE_THREE -> DIRECTOR_PHASE_THREE_CHAIN_RANGE;
			case PHASE_TWO -> DIRECTOR_PHASE_TWO_CHAIN_RANGE;
			case PHASE_ONE -> DIRECTOR_PHASE_ONE_CHAIN_RANGE;
		};
		return range * range;
	}

	private static double getDirectorFingerMinRangeSqr(DebtlordPhase phase) {
		double range = phase == DebtlordPhase.PHASE_THREE
			? DIRECTOR_PHASE_THREE_FINGER_MIN_RANGE
			: DIRECTOR_PHASE_TWO_FINGER_MIN_RANGE;
		return range * range;
	}

	private static float getCloseHornsChance(DebtlordPhase phase) {
		return switch (phase) {
			case PHASE_THREE -> 0.28F;
			case PHASE_TWO -> 0.38F;
			case PHASE_ONE -> 0.62F;
		};
	}

	private static float getCloseStompChance(DebtlordPhase phase) {
		return switch (phase) {
			case PHASE_THREE -> 0.28F;
			case PHASE_TWO -> 0.34F;
			case PHASE_ONE -> 0.14F;
		};
	}

	private static float getStompIntentChance(DebtlordPhase phase) {
		return switch (phase) {
			case PHASE_THREE -> 0.30F;
			case PHASE_TWO -> 0.52F;
			case PHASE_ONE -> 0.30F;
		};
	}

	private static float getClawIntentChance(DebtlordPhase phase) {
		return switch (phase) {
			case PHASE_THREE -> 0.92F;
			case PHASE_TWO -> 0.82F;
			case PHASE_ONE -> 0.55F;
		};
	}

	private boolean isValidCombatDirectorTarget(LivingEntity target) {
		return target != null
			&& target.isAlive()
			&& target != this
			&& !isAlliedTo(target)
			&& (!(target instanceof Player player) || (!player.isCreative() && !player.isSpectator()));
	}

	public CombatIntent getCombatIntent() {
		return combatIntent;
	}

	public boolean wantsCombatIntent(CombatIntent intent) {
		return combatIntent == intent;
	}

	private void tickSynchronizedWalkSteps() {
		if (!canPlaySynchronizedWalkStep()) {
			lastWalkStepPhase = -1;
			return;
		}

		int currentPhase = Math.floorMod(tickCount, WALK_ANIMATION_LENGTH_TICKS);
		if (lastWalkStepPhase >= 0) {
			if (lastWalkStepPhase < WALK_RIGHT_HOOF_STEP_TICK && currentPhase >= WALK_RIGHT_HOOF_STEP_TICK) {
				playSynchronizedWalkStep();
			}
			if (currentPhase < lastWalkStepPhase) {
				playSynchronizedWalkStep();
			}
		}
		lastWalkStepPhase = currentPhase;
	}

	private boolean canPlaySynchronizedWalkStep() {
		return isAlive()
			&& !isSilent()
			&& onGround()
			&& !isPassenger()
			&& !isVehicle()
			&& !isUsingAbility()
			&& getDeltaMovement().horizontalDistanceSqr() > WALK_STEP_MOVEMENT_THRESHOLD;
	}

	private void playSynchronizedWalkStep() {
		level().playSound(null, getX(), getY(), getZ(), TimothatysTrinketsModSounds.HOOF_STEP.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
	}

	private void tickServerAbilities() {
		int remainingTicks = getFearCastTicks();
		if (remainingTicks > 0) {
			int elapsedTicks = DebtlordFearGoal.CAST_DURATION_TICKS - remainingTicks + 1;
			trackFearTarget();

			if (elapsedTicks == DebtlordFearGoal.FEAR_START_TICK) {
				setTexture("debtlord_fear");
				DebtlordFearGoal.playRoar(this);
			}
			if (elapsedTicks >= DebtlordFearGoal.FEAR_START_TICK && elapsedTicks <= DebtlordFearGoal.FEAR_END_TICK) {
				DebtlordFearGoal.performFearPulse(this);
			}

			if (remainingTicks == 1) {
				finishFearCast();
			} else {
				setFearCastTicks(remainingTicks - 1);
			}
			return;
		}

		remainingTicks = getHornsCastTicks();
		int disarmTicks = getDisarmCastTicks();
		if (disarmTicks > 0) {
			lockAbilityPosition();
			int elapsedTicks = DebtlordDisarmAbility.CAST_DURATION_TICKS - disarmTicks + 1;
			if (elapsedTicks == DebtlordDisarmAbility.SWING_SOUND_TICK) {
				DebtlordDisarmAbility.playSwingSound(this);
			}
			if (elapsedTicks == DebtlordDisarmAbility.IMPACT_TICK) {
				DebtlordDisarmAbility.performImpact(this, disarmTarget);
			}
			setDisarmCastTicks(disarmTicks - 1);
			if (disarmTicks == 1)
				disarmTarget = null;
			return;
		}

		if (remainingTicks > 0) {
			lockAbilityPosition();
			int elapsedTicks = DebtlordHornsGoal.CAST_DURATION_TICKS - remainingTicks + 1;
			if (elapsedTicks == DebtlordHornsGoal.IMPACT_TICK) {
				DebtlordHornsGoal.performImpact(this, hornsTarget);
			}
			setHornsCastTicks(remainingTicks - 1);
			if (remainingTicks == 1)
				hornsTarget = null;
		}
	}

	private void updateMovementAnimationStates() {
		previousMovementAnimationBlend = movementAnimationBlend;
		if (!isAlive()) {
			idleAnimationState.stop();
			walkAnimationState.stop();
			movementAnimationBlend = 0.0F;
			return;
		}

		idleAnimationState.startIfStopped(tickCount);
		walkAnimationState.startIfStopped(tickCount);
		float blendDelta = getDeltaMovement().horizontalDistanceSqr() > 1.0E-6D
			? MOVEMENT_ANIMATION_BLEND_STEP
			: -MOVEMENT_ANIMATION_BLEND_STEP;
		movementAnimationBlend = Mth.clamp(movementAnimationBlend + blendDelta, 0.0F, 1.0F);
	}

	public float getMovementAnimationBlend(float partialTick) {
		return Mth.lerp(partialTick, previousMovementAnimationBlend, movementAnimationBlend);
	}

	public boolean isDeathAnimationActive() {
		return !isAlive() || deathTime > 0;
	}

	public int getDeathAnimationTicks() {
		return deathTime;
	}

	private void updateClientDeathAnimation() {
		appearanceAnimationState.stop();
		talkingAnimationState.stop();
		clientAppearanceAnimationTicks = 0;
		clientTalkingAnimationTicks = 0;
		idleAnimationState.stop();
		walkAnimationState.stop();
		stompAnimationState.stop();
		hornsAnimationState.stop();
		fearAnimationState.stop();
		disarmAnimationState.stop();
		clawAnimationState.stop();
		desolationAnimationState.stop();
		chainAnimationState.stop();
		chainSuccessAnimationState.stop();
		chainFailedAnimationState.stop();
		fingerOfDeathChargeAnimationState.stop();
		fingerOfDeathIdleAnimationState.stop();
		fingerOfDeathShotAnimationState.stop();
		clientStompAnimationTicks = 0;
		clientStompNeedsTimelineAnchor = false;
		clientHornsAnimationTicks = 0;
		clientFearAnimationTicks = 0;
		clientDisarmAnimationTicks = 0;
		clientClawAnimationTicks = 0;
		clientClawCastIndex = 1;
		clientDesolationAnimationTicks = 0;
		clientChainAnimationTicks = 0;
		clientChainPhase = CHAIN_PHASE_NONE;
		clientFingerOfDeathAnimationTicks = 0;
		clientFingerOfDeathPhase = FINGER_OF_DEATH_PHASE_NONE;
		movementAnimationBlend = 0.0F;
		previousMovementAnimationBlend = 0.0F;
		deathAnimationState.startIfStopped(tickCount);
	}

	private void updateClientAppearanceAnimation() {
		if (!isAltarSummoned() || getAltarSummonState() != ALTAR_SUMMON_STATE_APPEARANCE) {
			clientAppearanceAnimationTicks = 0;
			appearanceAnimationState.stop();
			return;
		}

		int syncedTicks = getAltarSummonTicks();
		if (clientAppearanceAnimationTicks <= 0 && syncedTicks > 0)
			startClientAppearanceAnimation(syncedTicks);
		else if (syncedTicks > lastSyncedAppearanceTicks && syncedTicks > clientAppearanceAnimationTicks + 2)
			startClientAppearanceAnimation(syncedTicks);
		lastSyncedAppearanceTicks = syncedTicks;

		if (clientAppearanceAnimationTicks > 0) {
			appearanceAnimationState.startIfStopped(tickCount);
			clientAppearanceAnimationTicks--;
		} else {
			appearanceAnimationState.stop();
		}
	}

	private void startClientAppearanceAnimation(int ticks) {
		if (!level().isClientSide())
			return;

		stopClientChainAnimationStates();
		stopClientFingerOfDeathAnimationStates();
		stopAllClientAbilityAnimationStates();
		clientTalkingAnimationTicks = 0;
		talkingAnimationState.stop();
		clientAppearanceAnimationTicks = Math.max(clientAppearanceAnimationTicks, Math.max(1, ticks));
		appearanceAnimationState.start(tickCount);
	}

	private void updateClientTalkingAnimation() {
		if (!isAltarSummoned() || getAltarSummonState() != ALTAR_SUMMON_STATE_TALKING) {
			clientTalkingAnimationTicks = 0;
			talkingAnimationState.stop();
			return;
		}

		int syncedTicks = getAltarSummonTicks();
		if (clientTalkingAnimationTicks <= 0 && syncedTicks > 0)
			startClientTalkingAnimation(syncedTicks);
		else if (syncedTicks > lastSyncedTalkingTicks && syncedTicks > clientTalkingAnimationTicks + 2)
			startClientTalkingAnimation(syncedTicks);
		lastSyncedTalkingTicks = syncedTicks;

		if (clientTalkingAnimationTicks > 0) {
			talkingAnimationState.startIfStopped(tickCount);
			clientTalkingAnimationTicks--;
		} else {
			talkingAnimationState.stop();
		}
	}

	private void startClientTalkingAnimation(int ticks) {
		if (!level().isClientSide())
			return;

		stopClientChainAnimationStates();
		stopClientFingerOfDeathAnimationStates();
		stopAllClientAbilityAnimationStates();
		clientAppearanceAnimationTicks = 0;
		appearanceAnimationState.stop();
		clientTalkingAnimationTicks = Math.max(clientTalkingAnimationTicks, Math.max(1, ticks));
		talkingAnimationState.start(tickCount);
	}

	private void stopAllClientAbilityAnimationStates() {
		clientStompAnimationTicks = 0;
		clientStompNeedsTimelineAnchor = false;
		stompAnimationState.stop();
		clientHornsAnimationTicks = 0;
		hornsAnimationState.stop();
		clientFearAnimationTicks = 0;
		fearAnimationState.stop();
		clientDisarmAnimationTicks = 0;
		disarmAnimationState.stop();
		clientClawAnimationTicks = 0;
		clawAnimationState.stop();
		clientDesolationAnimationTicks = 0;
		desolationAnimationState.stop();
	}

	private void clearAllAbilityCasts() {
		if (antiPillarGoal != null)
			antiPillarGoal.forceCancel();
		if (isStomping()) {
			finishStompCast();
		} else {
			setStompCastTicks(0);
			entityData.set(STOMP_FAST, false);
		}
		setHornsCastTicks(0);
		setFearCastTicks(0);
		setDisarmCastTicks(0);
		setClawCastTicks(0);
		setDesolationCastTicks(0);
		setChainCastTicks(0);
		setFingerOfDeathCastTicks(0);
		entityData.set(CHAIN_PHASE, CHAIN_PHASE_NONE);
		entityData.set(CHAIN_TARGET_ID, -1);
		entityData.set(FINGER_OF_DEATH_PHASE, FINGER_OF_DEATH_PHASE_NONE);
		entityData.set(FINGER_OF_DEATH_TARGET_ID, -1);
		hornsTarget = null;
		disarmTarget = null;
		chainTarget = null;
		fingerOfDeathTarget = null;
		fearTriggerTarget = null;
		fearTrackingTarget = null;
		fearAbilityPending = false;
		desolationAbilityPending = false;
		clawFollowups.clear();
	}

	@Override
	protected void tickDeath() {
		deathTime++;
		if (deathTime >= DEATH_ANIMATION_DURATION_TICKS && !level().isClientSide()) {
			level().broadcastEntityEvent(this, (byte) 60);
			remove(Entity.RemovalReason.KILLED);
		}
	}

	public void startStompCast(int durationTicks, boolean fast) {
		if (level().isClientSide())
			return;

		captureAbilityLock();
		entityData.set(STOMP_FAST, fast);
		setStompCastTicks(durationTicks);
		lockAbilityPosition();
		level().broadcastEntityEvent(this, fast ? FAST_STOMP_ANIMATION_EVENT : STOMP_ANIMATION_EVENT);
	}

	public void setStompCastTicks(int ticks) {
		if (!level().isClientSide())
			entityData.set(STOMP_TICKS, Math.max(0, ticks));
	}

	public int getStompCastTicks() {
		return entityData.get(STOMP_TICKS);
	}

	public boolean isStomping() {
		return getStompCastTicks() > 0;
	}

	public boolean isStompAnimationActive() {
		return level().isClientSide() ? clientStompAnimationTicks > 0 : isStomping();
	}

	public boolean isCurrentStompFast() {
		return entityData.get(STOMP_FAST);
	}

	public float getCurrentStompAnimationSpeed() {
		boolean fast = level().isClientSide() && clientStompAnimationTicks > 0 ? clientStompFast : isCurrentStompFast();
		return fast ? DebtlordStompGoal.FAST_ANIMATION_SPEED : 1.0F;
	}

	public void startHornsCast(LivingEntity target, int durationTicks) {
		if (level().isClientSide() || target == null)
			return;

		turnToward(target);
		captureAbilityLock();
		hornsTarget = target;
		setHornsCastTicks(durationTicks);
		lockAbilityPosition();
		level().broadcastEntityEvent(this, HORNS_ANIMATION_EVENT);
	}

	public void setHornsCastTicks(int ticks) {
		if (!level().isClientSide())
			entityData.set(HORNS_TICKS, Math.max(0, ticks));
	}

	public int getHornsCastTicks() {
		return entityData.get(HORNS_TICKS);
	}

	public boolean isUsingHorns() {
		return getHornsCastTicks() > 0;
	}

	public boolean isHornsAnimationActive() {
		return level().isClientSide() ? clientHornsAnimationTicks > 0 : isUsingHorns();
	}

	public void startDisarmCast(LivingEntity target, int durationTicks) {
		if (level().isClientSide() || target == null)
			return;

		turnToward(target);
		captureAbilityLock();
		disarmTarget = target;
		setDisarmCastTicks(durationTicks);
		lockAbilityPosition();
		level().broadcastEntityEvent(this, DISARM_ANIMATION_EVENT);
	}

	public void setDisarmCastTicks(int ticks) {
		if (!level().isClientSide())
			entityData.set(DISARM_TICKS, Math.max(0, ticks));
	}

	public int getDisarmCastTicks() {
		return entityData.get(DISARM_TICKS);
	}

	public boolean isUsingDisarm() {
		return getDisarmCastTicks() > 0;
	}

	public boolean isDisarmAnimationActive() {
		return level().isClientSide() ? clientDisarmAnimationTicks > 0 : isUsingDisarm();
	}

	public void startClawCast(LivingEntity target, int durationTicks, int castIndex) {
		startClawCast(target, durationTicks, castIndex, false);
	}

	public void startControlledClawCast(LivingEntity target, int durationTicks, int castIndex) {
		startClawCast(target, durationTicks, castIndex, true);
	}

	private void startClawCast(LivingEntity target, int durationTicks, int castIndex, boolean ignoreWaterRestriction) {
		if (level().isClientSide() || target == null
			|| (!ignoreWaterRestriction && (isTouchingWaterForBossLogic() || isEntityTouchingWater(target))))
			return;

		turnToward(target);
		captureAbilityLock();
		entityData.set(CLAW_CAST_INDEX, Mth.clamp(castIndex, 1, 3));
		setClawCastTicks(durationTicks);
		lockAbilityPositionFacing(target);
		byte animationEvent = castIndex >= 3 ? CLAW_THIRD_ANIMATION_EVENT : castIndex == 2 ? CLAW_SECOND_ANIMATION_EVENT : CLAW_ANIMATION_EVENT;
		level().broadcastEntityEvent(this, animationEvent);
	}

	public void setClawCastTicks(int ticks) {
		if (!level().isClientSide())
			entityData.set(CLAW_TICKS, Math.max(0, ticks));
	}

	public int getClawCastTicks() {
		return entityData.get(CLAW_TICKS);
	}

	public boolean isUsingClaws() {
		return getClawCastTicks() > 0;
	}

	public boolean isClawAnimationActive() {
		return level().isClientSide() ? clientClawAnimationTicks > 0 : isUsingClaws();
	}

	public int getClawCastIndex() {
		return level().isClientSide() ? clientClawCastIndex : entityData.get(CLAW_CAST_INDEX);
	}

	public float getClawAnimationSpeed() {
		return DebtlordClawGoal.getAnimationSpeed(getClawCastIndex());
	}

	public boolean isTouchingWaterForBossLogic() {
		return isEntityTouchingWater(this);
	}

	public static boolean isEntityTouchingWater(LivingEntity entity) {
		if (entity == null)
			return false;
		if (entity.isInWaterOrBubble())
			return true;

		AABB box = entity.getBoundingBox().deflate(0.05D);
		double checkTop = Math.min(box.maxY, box.minY + Math.min(entity.getBbHeight(), 1.25D));
		int minX = Mth.floor(box.minX);
		int maxX = Mth.floor(box.maxX - 1.0E-7D);
		int minY = Mth.floor(box.minY);
		int maxY = Mth.floor(checkTop - 1.0E-7D);
		int minZ = Mth.floor(box.minZ);
		int maxZ = Mth.floor(box.maxZ - 1.0E-7D);
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					cursor.set(x, y, z);
					if (entity.level().getFluidState(cursor).is(FluidTags.WATER))
						return true;
				}
			}
		}
		return false;
	}

	public boolean canClawAdvanceToward(LivingEntity target) {
		if (level().isClientSide() || target == null || !target.isAlive() || isTouchingWaterForBossLogic() || isEntityTouchingWater(target))
			return false;

		double advanceFraction = isEnraged() ? DebtlordClawGoal.ENRAGED_ADVANCE_FRACTION : DebtlordClawGoal.ADVANCE_FRACTION;
		Vec3 advance = new Vec3(target.getX() - getX(), 0.0D, target.getZ() - getZ()).scale(advanceFraction);
		return canClawAdvanceAlong(advance);
	}

	public boolean canClawBreakThroughToward(LivingEntity target) {
		if (level().isClientSide() || !isEnraged() || target == null || !target.isAlive() || isTouchingWaterForBossLogic() || isEntityTouchingWater(target))
			return false;

		double advanceFraction = DebtlordClawGoal.ENRAGED_ADVANCE_FRACTION;
		Vec3 advance = new Vec3(target.getX() - getX(), 0.0D, target.getZ() - getZ()).scale(advanceFraction);
		return hasOnlyBreakableClawObstaclesAlong(advance);
	}

	public void advanceClawToward(LivingEntity target) {
		if (level().isClientSide() || target == null || !target.isAlive() || !isUsingClaws())
			return;

		boolean enraged = isEnraged();
		double advanceFraction = enraged ? DebtlordClawGoal.ENRAGED_ADVANCE_FRACTION : DebtlordClawGoal.ADVANCE_FRACTION;
		Vec3 advance = new Vec3(target.getX() - getX(), 0.0D, target.getZ() - getZ()).scale(advanceFraction);
		double stepUp = getClawAdvanceStepUp(advance);
		if (stepUp < 0.0D)
			return;
		if (enraged)
			breakBlocksAlongClawAdvance(advance);
		pushEntitiesAlongClawAdvance(advance, target);
		move(MoverType.SELF, advance.add(0.0D, stepUp, 0.0D));
		abilityAnchorX = getX();
		abilityAnchorZ = getZ();
		turnToward(target);
		abilityLockedYRot = getYRot();
		abilityLockedBodyYRot = yBodyRot;
		abilityLockedHeadYRot = yHeadRot;
		setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
		hasImpulse = true;
	}

	private boolean canClawAdvanceAlong(Vec3 advance) {
		return getClawAdvanceStepUp(advance) >= 0.0D;
	}

	private boolean hasOnlyBreakableClawObstaclesAlong(Vec3 advance) {
		if (!(level() instanceof ServerLevel serverLevel) || advance.horizontalDistanceSqr() < 1.0E-5D)
			return false;

		double distance = advance.horizontalDistance();
		int steps = Mth.clamp((int) Math.ceil(distance * 2.0D), 1, 24);
		Vec3 start = position();
		boolean foundBreakableObstacle = false;
		for (int step = 1; step <= steps; step++) {
			Vec3 offset = advance.scale(step / (double) steps);
			Vec3 center = start.add(offset);
			if (isWaterOnClawPath(center))
				return false;
			if (hasUnbreakableClawObstacle(serverLevel, center))
				return false;
			boolean hasBreakableObstacle = hasBreakableClawObstacle(serverLevel, center);
			if (hasBreakableObstacle)
				foundBreakableObstacle = true;
			if (getClawAdvanceYOffset(offset) < 0.0D && !hasBreakableObstacle)
				return false;
		}
		return foundBreakableObstacle;
	}

	private double getClawAdvanceStepUp(Vec3 advance) {
		if (advance.horizontalDistanceSqr() < 1.0E-5D)
			return 0.0D;

		double distance = advance.horizontalDistance();
		int steps = Mth.clamp((int) Math.ceil(distance * 2.0D), 1, 24);
		Vec3 start = position();
		double requiredStepUp = 0.0D;
		for (int step = 1; step <= steps; step++) {
			Vec3 offset = advance.scale(step / (double) steps);
			Vec3 center = start.add(offset);
			if (isWaterOnClawPath(center))
				return -1.0D;

			double stepUp = getClawAdvanceYOffset(offset);
			if (stepUp < 0.0D) {
				if (!isEnraged() || !(level() instanceof ServerLevel serverLevel) || hasUnbreakableClawObstacle(serverLevel, center))
					return -1.0D;
			} else {
				requiredStepUp = Math.max(requiredStepUp, stepUp);
			}
		}
		return requiredStepUp;
	}

	private double getClawAdvanceYOffset(Vec3 offset) {
		AABB movedBox = getBoundingBox().move(offset);
		if (level().noCollision(this, movedBox))
			return 0.0D;

		double maxStep = Math.max(maxUpStep(), CLAW_MAX_STEP_UP);
		int stepChecks = Mth.clamp((int) Math.ceil(maxStep * 8.0D), 1, 10);
		for (int i = 1; i <= stepChecks; i++) {
			double yOffset = maxStep * i / stepChecks;
			if (level().noCollision(this, movedBox.move(0.0D, yOffset, 0.0D)))
				return yOffset;
		}
		return -1.0D;
	}

	private boolean isWaterOnClawPath(Vec3 center) {
		int minY = Mth.floor(getY());
		int maxY = Mth.floor(getY() + Math.min(getBbHeight(), 1.2F));
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int y = minY; y <= maxY; y++) {
			cursor.set(Mth.floor(center.x), y, Mth.floor(center.z));
			if (level().getFluidState(cursor).is(FluidTags.WATER))
				return true;
		}
		return false;
	}

	private boolean hasUnbreakableClawObstacle(ServerLevel serverLevel, Vec3 center) {
		double halfWidth = getBbWidth() * 0.68D + 0.18D;
		int minX = Mth.floor(center.x - halfWidth);
		int maxX = Mth.floor(center.x + halfWidth);
		int minY = Mth.floor(getY());
		int maxY = Mth.floor(getY() + getBbHeight() * 0.92D);
		int minZ = Mth.floor(center.z - halfWidth);
		int maxZ = Mth.floor(center.z + halfWidth);
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					cursor.set(x, y, z);
					BlockState state = serverLevel.getBlockState(cursor);
					if (state.getCollisionShape(serverLevel, cursor).isEmpty())
						continue;
					if (!canClawBreakBlock(serverLevel, cursor, state))
						return true;
				}
			}
		}
		return false;
	}

	private boolean hasBreakableClawObstacle(ServerLevel serverLevel, Vec3 center) {
		double halfWidth = getBbWidth() * 0.68D + 0.18D;
		int minX = Mth.floor(center.x - halfWidth);
		int maxX = Mth.floor(center.x + halfWidth);
		int minY = Mth.floor(getY());
		int maxY = Mth.floor(getY() + getBbHeight() * 0.92D);
		int minZ = Mth.floor(center.z - halfWidth);
		int maxZ = Mth.floor(center.z + halfWidth);
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					cursor.set(x, y, z);
					BlockState state = serverLevel.getBlockState(cursor);
					if (!state.getCollisionShape(serverLevel, cursor).isEmpty() && canClawBreakBlock(serverLevel, cursor, state))
						return true;
				}
			}
		}
		return false;
	}

	private void pushEntitiesAlongClawAdvance(Vec3 advance, LivingEntity target) {
		if (!(level() instanceof ServerLevel serverLevel) || advance.horizontalDistanceSqr() < 1.0E-5D)
			return;

		Vec3 horizontal = new Vec3(advance.x, 0.0D, advance.z);
		double distance = horizontal.length();
		Vec3 forward = horizontal.normalize();
		Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
		Vec3 start = position();
		AABB sweepBounds = getBoundingBox().expandTowards(advance).inflate(CLAW_PATH_PUSH_RADIUS, 0.45D, CLAW_PATH_PUSH_RADIUS);
		for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, sweepBounds, candidate -> canClawPushEntity(candidate, target))) {
			Vec3 relative = entity.position().subtract(start);
			double forwardDistance = relative.dot(forward);
			if (forwardDistance < -entity.getBbWidth() || forwardDistance > distance + entity.getBbWidth())
				continue;

			double lateralDistance = relative.dot(right);
			double maxLateralDistance = getBbWidth() * 0.5D + entity.getBbWidth() * 0.5D + CLAW_PATH_PUSH_RADIUS;
			if (Math.abs(lateralDistance) > maxLateralDistance)
				continue;

			double sideSign = lateralDistance >= 0.0D ? 1.0D : -1.0D;
			Vec3 sidePush = right.scale(sideSign * CLAW_PATH_SIDE_PUSH);
			Vec3 forwardPush = forward.scale(CLAW_PATH_FORWARD_PUSH);
			double lift = entity.onGround() ? CLAW_PATH_GROUND_LIFT : CLAW_PATH_AIR_LIFT;
			entity.push(sidePush.x + forwardPush.x, lift, sidePush.z + forwardPush.z);
			entity.hurtMarked = true;
			entity.hasImpulse = true;
		}
	}

	private boolean canClawPushEntity(LivingEntity candidate, LivingEntity target) {
		return candidate != this
			&& candidate != target
			&& candidate.isAlive()
			&& !isAlliedTo(candidate)
			&& (!(candidate instanceof Player player) || (!player.isCreative() && !player.isSpectator()));
	}

	private void breakBlocksAlongClawAdvance(Vec3 advance) {
		if (!(level() instanceof ServerLevel serverLevel) || advance.horizontalDistanceSqr() < 1.0E-5D)
			return;

		double distance = advance.horizontalDistance();
		int steps = Mth.clamp((int) Math.ceil(distance * 2.0D), 1, 24);
		Set<BlockPos> visited = new HashSet<>();
		Vec3 start = position();
		for (int step = 1; step <= steps; step++) {
			Vec3 center = start.add(advance.scale(step / (double) steps));
			breakBlocksAroundClawPoint(serverLevel, center, visited);
		}
	}

	private void breakBlocksAroundClawPoint(ServerLevel serverLevel, Vec3 center, Set<BlockPos> visited) {
		double halfWidth = getBbWidth() * 0.68D + 0.18D;
		int minX = Mth.floor(center.x - halfWidth);
		int maxX = Mth.floor(center.x + halfWidth);
		int minY = Mth.floor(getY());
		int maxY = Mth.floor(getY() + getBbHeight() * 0.92D);
		int minZ = Mth.floor(center.z - halfWidth);
		int maxZ = Mth.floor(center.z + halfWidth);
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					cursor.set(x, y, z);
					BlockPos pos = cursor.immutable();
					if (!visited.add(pos))
						continue;

					BlockState state = serverLevel.getBlockState(pos);
					if (!canClawBreakBlock(serverLevel, pos, state))
						continue;

					serverLevel.levelEvent(2001, pos, Block.getId(state));
					serverLevel.destroyBlock(pos, false, this);
				}
			}
		}
	}

	private static boolean canClawBreakBlock(ServerLevel serverLevel, BlockPos pos, BlockState state) {
		return !state.isAir()
			&& !state.hasBlockEntity()
			&& state.getDestroySpeed(serverLevel, pos) >= 0.0F
			&& state.is(DEBTLORD_BREAKABLE_BLOCKS);
	}

	public void offerClawFollowup(LivingEntity target, int delayTicks, int windowTicks, DebtlordClawFollowupQueue.Reason reason) {
		if (!level().isClientSide())
			clawFollowups.offer(this, target, delayTicks, windowTicks, reason);
	}

	public DebtlordClawFollowupQueue.Entry peekReadyClawFollowup() {
		return level().isClientSide() ? null : clawFollowups.peekReady(this);
	}

	public DebtlordClawFollowupQueue.Entry consumeReadyClawFollowup() {
		return level().isClientSide() ? null : clawFollowups.consumeReady(this);
	}

	public void clearClawFollowup() {
		if (!level().isClientSide())
			clawFollowups.clear();
	}

	private void cancelPendingCombatFollowups() {
		clawFollowups.clear();
		if (antiPillarGoal != null)
			antiPillarGoal.clearPendingRequest();
	}

	public boolean canStartDesolationAbility() {
		return !level().isClientSide()
			&& desolationAbilityPending
			&& !desolationAbilityUsed
			&& isAlive()
			&& onGround()
			&& !isTouchingWaterForBossLogic()
			&& !isUsingAbility();
	}

	public void startDesolationCast(Player target, int durationTicks) {
		if (level().isClientSide() || target == null || desolationAbilityUsed)
			return;

		cancelPendingCombatFollowups();
		desolationAbilityPending = false;
		desolationAbilityUsed = true;
		turnToward(target);
		captureAbilityLock();
		setDesolationCastTicks(durationTicks);
		lockAbilityPosition();
		level().broadcastEntityEvent(this, DESOLATION_ANIMATION_EVENT);
	}

	public void setDesolationCastTicks(int ticks) {
		if (!level().isClientSide())
			entityData.set(DESOLATION_TICKS, Math.max(0, ticks));
	}

	public int getDesolationCastTicks() {
		return entityData.get(DESOLATION_TICKS);
	}

	public boolean isUsingDesolation() {
		return getDesolationCastTicks() > 0;
	}

	public boolean isDesolationAnimationActive() {
		return level().isClientSide() ? clientDesolationAnimationTicks > 0 : isUsingDesolation();
	}

	public void startChainCast(LivingEntity target) {
		startChainAnimation(target, DebtlordChainsGoal.CAST_DURATION_TICKS, CHAIN_PHASE_CAST, CHAIN_ANIMATION_EVENT);
	}

	public void startChainSuccess(LivingEntity target) {
		startChainAnimation(target, DebtlordChainsGoal.SUCCESS_DURATION_TICKS, CHAIN_PHASE_SUCCESS, CHAIN_SUCCESS_ANIMATION_EVENT);
	}

	public void startChainFailed(LivingEntity target) {
		startChainAnimation(target, DebtlordChainsGoal.FAILED_DURATION_TICKS, CHAIN_PHASE_FAILED, CHAIN_FAILED_ANIMATION_EVENT);
	}

	private void startChainAnimation(LivingEntity target, int durationTicks, int phase, byte animationEvent) {
		if (level().isClientSide())
			return;

		if (phase == CHAIN_PHASE_CAST)
			cancelPendingCombatFollowups();
		chainTarget = target;
		if (target != null)
			turnToward(target);
		captureAbilityLock();
		entityData.set(CHAIN_PHASE, phase);
		entityData.set(CHAIN_TARGET_ID, phase == CHAIN_PHASE_FAILED || target == null ? -1 : target.getId());
		setChainCastTicks(durationTicks);
		if (target != null)
			lockAbilityPositionFacing(target);
		else
			lockAbilityPosition();
		level().broadcastEntityEvent(this, animationEvent);
	}

	public void markChainCaptured(LivingEntity target) {
		if (!level().isClientSide() && target != null) {
			chainTarget = target;
			entityData.set(CHAIN_TARGET_ID, target.getId());
		}
	}

	public void setChainCastTicks(int ticks) {
		if (!level().isClientSide())
			entityData.set(CHAIN_TICKS, Math.max(0, ticks));
	}

	public int getChainCastTicks() {
		return entityData.get(CHAIN_TICKS);
	}

	public int getChainPhase() {
		return level().isClientSide() ? clientChainPhase : entityData.get(CHAIN_PHASE);
	}

	public int getChainTargetId() {
		return entityData.get(CHAIN_TARGET_ID);
	}

	public int getCurrentChainPhaseDuration() {
		return switch (getChainPhase()) {
			case CHAIN_PHASE_SUCCESS -> DebtlordChainsGoal.SUCCESS_DURATION_TICKS;
			case CHAIN_PHASE_FAILED -> DebtlordChainsGoal.FAILED_DURATION_TICKS;
			default -> DebtlordChainsGoal.CAST_DURATION_TICKS;
		};
	}

	public boolean isUsingChains() {
		return getChainCastTicks() > 0;
	}

	public boolean isChainCastAnimationActive() {
		return isUsingChains() && getChainPhase() == CHAIN_PHASE_CAST;
	}

	public boolean isChainSuccessAnimationActive() {
		return isUsingChains() && getChainPhase() == CHAIN_PHASE_SUCCESS;
	}

	public boolean isChainFailedAnimationActive() {
		return isUsingChains() && getChainPhase() == CHAIN_PHASE_FAILED;
	}

	public void startFingerOfDeathCharge(LivingEntity target) {
		startFingerOfDeathAnimation(target, DebtlordFingerOfDeathGoal.CHARGE_DURATION_TICKS, FINGER_OF_DEATH_PHASE_CHARGE, FINGER_OF_DEATH_CHARGE_ANIMATION_EVENT);
	}

	public void startFingerOfDeathIdle(LivingEntity target) {
		startFingerOfDeathAnimation(target, DebtlordFingerOfDeathGoal.LASER_DURATION_TICKS, FINGER_OF_DEATH_PHASE_IDLE, FINGER_OF_DEATH_IDLE_ANIMATION_EVENT);
	}

	public void startFingerOfDeathShot(LivingEntity target) {
		startFingerOfDeathAnimation(target, DebtlordFingerOfDeathGoal.SHOT_DURATION_TICKS, FINGER_OF_DEATH_PHASE_SHOT, FINGER_OF_DEATH_SHOT_ANIMATION_EVENT);
	}

	private void startFingerOfDeathAnimation(LivingEntity target, int durationTicks, int phase, byte animationEvent) {
		if (level().isClientSide())
			return;

		if (phase == FINGER_OF_DEATH_PHASE_CHARGE)
			cancelPendingCombatFollowups();
		fingerOfDeathTarget = target;
		if (target != null)
			turnToward(target);
		captureAbilityLock();
		entityData.set(FINGER_OF_DEATH_PHASE, phase);
		entityData.set(FINGER_OF_DEATH_TARGET_ID, target == null ? -1 : target.getId());
		setFingerOfDeathCastTicks(durationTicks);
		setFingerOfDeathLaserRotation(getYRot(), getXRot());
		setFingerOfDeathLaserTarget(getFingerOfDeathServerLaserSource().add(getFingerOfDeathLaserDirection().scale(DebtlordFingerOfDeathGoal.LASER_RANGE)));
		if (target != null)
			lockAbilityPositionFacing(target);
		else
			lockAbilityPosition();
		level().broadcastEntityEvent(this, animationEvent);
	}

	public void setFingerOfDeathCastTicks(int ticks) {
		if (!level().isClientSide())
			entityData.set(FINGER_OF_DEATH_TICKS, Math.max(0, ticks));
	}

	public int getFingerOfDeathCastTicks() {
		return entityData.get(FINGER_OF_DEATH_TICKS);
	}

	public int getFingerOfDeathPhase() {
		return level().isClientSide() ? clientFingerOfDeathPhase : entityData.get(FINGER_OF_DEATH_PHASE);
	}

	public int getFingerOfDeathTargetId() {
		return entityData.get(FINGER_OF_DEATH_TARGET_ID);
	}

	public int getCurrentFingerOfDeathPhaseDuration() {
		return switch (getFingerOfDeathPhase()) {
			case FINGER_OF_DEATH_PHASE_CHARGE -> DebtlordFingerOfDeathGoal.CHARGE_DURATION_TICKS;
			case FINGER_OF_DEATH_PHASE_IDLE -> DebtlordFingerOfDeathGoal.LASER_DURATION_TICKS;
			case FINGER_OF_DEATH_PHASE_SHOT -> DebtlordFingerOfDeathGoal.SHOT_DURATION_TICKS;
			default -> 0;
		};
	}

	public boolean isUsingFingerOfDeath() {
		return level().isClientSide() ? clientFingerOfDeathAnimationTicks > 0 : getFingerOfDeathCastTicks() > 0;
	}

	public boolean isFingerOfDeathChargeAnimationActive() {
		return isUsingFingerOfDeath() && getFingerOfDeathPhase() == FINGER_OF_DEATH_PHASE_CHARGE;
	}

	public boolean isFingerOfDeathIdleAnimationActive() {
		return isUsingFingerOfDeath() && getFingerOfDeathPhase() == FINGER_OF_DEATH_PHASE_IDLE;
	}

	public boolean isFingerOfDeathShotAnimationActive() {
		return isUsingFingerOfDeath() && getFingerOfDeathPhase() == FINGER_OF_DEATH_PHASE_SHOT;
	}

	public void setFingerOfDeathLaserRotation(float yaw, float pitch) {
		if (!level().isClientSide()) {
			entityData.set(FINGER_OF_DEATH_LASER_YAW, yaw);
			entityData.set(FINGER_OF_DEATH_LASER_PITCH, Mth.clamp(pitch, -70.0F, 55.0F));
		}
	}

	public float getFingerOfDeathLaserYaw() {
		return entityData.get(FINGER_OF_DEATH_LASER_YAW);
	}

	public float getFingerOfDeathLaserPitch() {
		return entityData.get(FINGER_OF_DEATH_LASER_PITCH);
	}

	public Vec3 getFingerOfDeathLaserDirection() {
		float yawRad = getFingerOfDeathLaserYaw() * Mth.DEG_TO_RAD;
		float pitchRad = getFingerOfDeathLaserPitch() * Mth.DEG_TO_RAD;
		float horizontal = Mth.cos(pitchRad);
		return new Vec3(
			-Mth.sin(yawRad) * horizontal,
			-Mth.sin(pitchRad),
			Mth.cos(yawRad) * horizontal
		).normalize();
	}

	public void setFingerOfDeathLaserTarget(Vec3 target) {
		if (!level().isClientSide() && target != null)
			entityData.set(FINGER_OF_DEATH_LASER_TARGET, new Vector3f((float) target.x, (float) target.y, (float) target.z));
	}

	public Vec3 getFingerOfDeathLaserTarget(float partialTick) {
		Vector3f target = entityData.get(FINGER_OF_DEATH_LASER_TARGET);
		return new Vec3(target.x(), target.y(), target.z());
	}

	public Vec3 getFingerOfDeathServerLaserSource() {
		float bodyYaw = yBodyRot * Mth.DEG_TO_RAD;
		Vec3 forward = new Vec3(-Mth.sin(bodyYaw), 0.0D, Mth.cos(bodyYaw)).normalize();
		Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
		Vec3 palmSource = position()
			.add(0.0D, getBbHeight() * 0.50D, 0.0D)
			.add(forward.scale(getBbWidth() * 0.48D))
			.subtract(right.scale(getBbWidth() * 0.48D));
		return palmSource.add(getFingerOfDeathLaserDirection().scale(getBbWidth() * FINGER_OF_DEATH_SOURCE_TIP_OFFSET));
	}

	public void registerFearTriggerHit(Player attacker) {
		if (level().isClientSide() || attacker == null || fearAbilityPending || level().getGameTime() < fearCooldownUntil)
			return;

		long gameTime = level().getGameTime();
		if (gameTime - lastFearHitGameTime > DebtlordFearGoal.HIT_STREAK_WINDOW_TICKS) {
			fearHitCount = 0;
		}
		lastFearHitGameTime = gameTime;
		fearHitCount++;
		if (fearHitCount >= DebtlordFearGoal.REQUIRED_HITS) {
			fearHitCount = 0;
			fearAbilityPending = true;
			fearTriggerTarget = attacker;
		}
	}

	public boolean canStartFearAbility() {
		if (fearAbilityPending && (fearTriggerTarget == null || !fearTriggerTarget.isAlive())) {
			fearAbilityPending = false;
			fearTriggerTarget = null;
		}
		return fearAbilityPending
			&& fearTriggerTarget != null
			&& isAlive()
			&& (onGround() || isTouchingWaterForBossLogic())
			&& !isUsingAbility()
			&& level().getGameTime() >= fearCooldownUntil;
	}

	public LivingEntity getFearTriggerTarget() {
		return fearTriggerTarget;
	}

	public void startFearCast(LivingEntity target, int durationTicks) {
		if (level().isClientSide() || target == null)
			return;

		cancelPendingCombatFollowups();
		fearAbilityPending = false;
		fearHitCount = 0;
		fearTriggerTarget = null;
		fearTrackingTarget = target;
		fearCooldownUntil = level().getGameTime() + DebtlordFearGoal.COOLDOWN_TICKS;
		textureBeforeFear = getTexture();
		turnToward(target);
		captureAbilityLock();
		setFearCastTicks(durationTicks);
		lockAbilityPosition();
		level().broadcastEntityEvent(this, FEAR_ANIMATION_EVENT);
	}

	public void setFearCastTicks(int ticks) {
		if (!level().isClientSide())
			entityData.set(FEAR_TICKS, Math.max(0, ticks));
	}

	public int getFearCastTicks() {
		return entityData.get(FEAR_TICKS);
	}

	public boolean isUsingFear() {
		return getFearCastTicks() > 0;
	}

	public boolean isFearAnimationActive() {
		return level().isClientSide() ? clientFearAnimationTicks > 0 : isUsingFear();
	}

	public boolean isUsingAbility() {
		return isStomping() || isUsingHorns() || isUsingFear() || isUsingDisarm() || isUsingClaws() || isUsingDesolation() || isUsingChains() || isUsingFingerOfDeath() || isAntiPillarActive();
	}

	public void beginAntiPillarAbility(int phase, int ticks) {
		if (level().isClientSide())
			return;
		captureAbilityLock();
		setAntiPillarVisualState(phase, ticks);
		getNavigation().stop();
		setDeltaMovement(Vec3.ZERO);
	}

	public void setAntiPillarVisualState(int phase, int ticks) {
		if (level().isClientSide())
			return;
		entityData.set(ANTI_PILLAR_PHASE, phase);
		entityData.set(ANTI_PILLAR_TICKS, Math.max(0, ticks));
	}

	public int getAntiPillarPhase() {
		return entityData.get(ANTI_PILLAR_PHASE);
	}

	public int getAntiPillarTicks() {
		return entityData.get(ANTI_PILLAR_TICKS);
	}

	public boolean isAntiPillarActive() {
		return getAntiPillarPhase() != DebtlordAntiPillarGoal.PHASE_NONE;
	}

	public void faceAbilityTargetInPlace(LivingEntity target) {
		if (!isUsingAbility())
			return;
		getNavigation().stop();
		setDeltaMovement(Vec3.ZERO);
		if (target != null && target.isAlive())
			turnToward(target);
		hasImpulse = true;
	}

	public void finishAntiPillarAbility() {
		if (!level().isClientSide()) {
			entityData.set(ANTI_PILLAR_PHASE, DebtlordAntiPillarGoal.PHASE_NONE);
			entityData.set(ANTI_PILLAR_TICKS, 0);
			releaseAbilityMotion();
		}
	}

	private void captureAbilityLock() {
		abilityAnchorX = getX();
		abilityAnchorZ = getZ();
		abilityLockedYRot = getYRot();
		abilityLockedXRot = getXRot();
		abilityLockedBodyYRot = yBodyRot;
		abilityLockedHeadYRot = yHeadRot;
	}

	private void turnToward(LivingEntity target) {
		double dx = target.getX() - getX();
		double dy = target.getEyeY() - getEyeY();
		double dz = target.getZ() - getZ();
		double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
		float targetYRot = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
		float targetXRot = (float) -(Mth.atan2(dy, horizontalDistance) * Mth.RAD_TO_DEG);
		setYRot(targetYRot);
		setXRot(Mth.clamp(targetXRot, -45.0F, 45.0F));
		yBodyRot = targetYRot;
		yHeadRot = targetYRot;
		getLookControl().setLookAt(target, 60.0F, 45.0F);
	}

	private void turnTowardSlowly(LivingEntity target, float maxYawStep, float maxPitchStep) {
		double dx = target.getX() - getX();
		double dy = target.getEyeY() - getEyeY();
		double dz = target.getZ() - getZ();
		double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
		float targetYRot = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
		float targetXRot = (float) -(Mth.atan2(dy, horizontalDistance) * Mth.RAD_TO_DEG);
		float nextYRot = approachDegrees(getYRot(), targetYRot, maxYawStep);
		float nextXRot = approachDegrees(getXRot(), Mth.clamp(targetXRot, -70.0F, 55.0F), maxPitchStep);
		setYRot(nextYRot);
		setXRot(nextXRot);
		yBodyRot = approachDegrees(yBodyRot, nextYRot, maxYawStep);
		yHeadRot = approachDegrees(yHeadRot, nextYRot, maxYawStep);
		getLookControl().setLookAt(target, maxYawStep, maxPitchStep);
	}

	private static float approachDegrees(float current, float target, float maxChange) {
		float delta = Mth.wrapDegrees(target - current);
		return current + Mth.clamp(delta, -maxChange, maxChange);
	}

	public void trackFearTarget() {
		if (fearTrackingTarget != null && fearTrackingTarget.isAlive()) {
			lockAbilityPositionFacing(fearTrackingTarget);
			return;
		}
		lockAbilityPosition();
	}

	public void lockAbilityPositionFacing(LivingEntity target) {
		if (!isUsingAbility())
			return;

		getNavigation().stop();
		setPos(abilityAnchorX, getY(), abilityAnchorZ);
		setDeltaMovement(0.0D, Math.min(0.0D, getDeltaMovement().y), 0.0D);
		if (target != null && target.isAlive()) {
			turnToward(target);
			abilityLockedYRot = getYRot();
			abilityLockedXRot = getXRot();
			abilityLockedBodyYRot = yBodyRot;
			abilityLockedHeadYRot = yHeadRot;
		} else {
			setYRot(abilityLockedYRot);
			setXRot(abilityLockedXRot);
			yBodyRot = abilityLockedBodyYRot;
			yHeadRot = abilityLockedHeadYRot;
		}
		hasImpulse = true;
	}

	public void lockAbilityPositionFacingSlowly(LivingEntity target, float maxYawStep, float maxPitchStep) {
		if (!isUsingAbility())
			return;

		getNavigation().stop();
		setPos(abilityAnchorX, getY(), abilityAnchorZ);
		setDeltaMovement(0.0D, Math.min(0.0D, getDeltaMovement().y), 0.0D);
		if (target != null && target.isAlive()) {
			turnTowardSlowly(target, maxYawStep, maxPitchStep);
			abilityLockedYRot = getYRot();
			abilityLockedXRot = getXRot();
			abilityLockedBodyYRot = yBodyRot;
			abilityLockedHeadYRot = yHeadRot;
		} else {
			setYRot(abilityLockedYRot);
			setXRot(abilityLockedXRot);
			yBodyRot = abilityLockedBodyYRot;
			yHeadRot = abilityLockedHeadYRot;
		}
		hasImpulse = true;
	}

	public void lockAbilityPositionFacingFingerOfDeathLaser() {
		if (!isUsingAbility())
			return;

		getNavigation().stop();
		setPos(abilityAnchorX, getY(), abilityAnchorZ);
		setDeltaMovement(0.0D, Math.min(0.0D, getDeltaMovement().y), 0.0D);
		float laserYaw = getFingerOfDeathLaserYaw();
		float laserPitch = getFingerOfDeathLaserPitch();
		setYRot(laserYaw);
		setXRot(Mth.clamp(laserPitch, -70.0F, 55.0F));
		yBodyRot = laserYaw;
		yHeadRot = laserYaw;
		abilityLockedYRot = getYRot();
		abilityLockedXRot = getXRot();
		abilityLockedBodyYRot = yBodyRot;
		abilityLockedHeadYRot = yHeadRot;
		hasImpulse = true;
	}

	public void lockAbilityPosition() {
		if (!isUsingAbility())
			return;

		getNavigation().stop();
		setPos(abilityAnchorX, getY(), abilityAnchorZ);
		setDeltaMovement(0.0D, Math.min(0.0D, getDeltaMovement().y), 0.0D);
		setYRot(abilityLockedYRot);
		setXRot(abilityLockedXRot);
		yBodyRot = abilityLockedBodyYRot;
		yHeadRot = abilityLockedHeadYRot;
		hasImpulse = true;
	}

	public void finishStompCast() {
		if (!level().isClientSide()) {
			boolean wasStomping = isStomping();
			setStompCastTicks(0);
			entityData.set(STOMP_FAST, false);
			if (wasStomping)
				level().broadcastEntityEvent(this, STOMP_STOP_ANIMATION_EVENT);
			releaseAbilityMotion();
		}
	}

	public void finishHornsCast() {
		if (!level().isClientSide()) {
			setHornsCastTicks(0);
			hornsTarget = null;
			releaseAbilityMotion();
		}
	}

	public void finishDisarmCast() {
		if (!level().isClientSide()) {
			setDisarmCastTicks(0);
			disarmTarget = null;
			releaseAbilityMotion();
		}
	}

	public void finishClawCast() {
		if (!level().isClientSide()) {
			setClawCastTicks(0);
			releaseAbilityMotion();
		}
	}

	public void finishDesolationCast() {
		if (!level().isClientSide()) {
			setDesolationCastTicks(0);
			releaseAbilityMotion();
		}
	}

	public void finishChainCast() {
		if (!level().isClientSide()) {
			setChainCastTicks(0);
			entityData.set(CHAIN_PHASE, CHAIN_PHASE_NONE);
			entityData.set(CHAIN_TARGET_ID, -1);
			chainTarget = null;
			releaseAbilityMotion();
		}
	}

	public void finishFingerOfDeathCast() {
		if (!level().isClientSide()) {
			setFingerOfDeathCastTicks(0);
			entityData.set(FINGER_OF_DEATH_PHASE, FINGER_OF_DEATH_PHASE_NONE);
			entityData.set(FINGER_OF_DEATH_TARGET_ID, -1);
			fingerOfDeathTarget = null;
			releaseAbilityMotion();
		}
	}

	public void finishFearCast() {
		if (!level().isClientSide()) {
			setFearCastTicks(0);
			fearTrackingTarget = null;
			if (textureBeforeFear != null) {
				setTexture(textureBeforeFear);
				textureBeforeFear = null;
			}
			releaseAbilityMotion();
		}
	}

	private void releaseAbilityMotion() {
		getNavigation().stop();
		setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
	}

	private void updateClientStompAnimation() {
		if (!isAlive()) {
			clientStompAnimationTicks = 0;
			clientStompNeedsTimelineAnchor = false;
			stompAnimationState.stop();
			return;
		}

		int syncedTicks = getStompCastTicks();
		if (clientStompNeedsTimelineAnchor && syncedTicks > 0) {
			int durationTicks = DebtlordStompGoal.getStompCastDurationTicks(clientStompFast);
			int elapsedTicks = Mth.clamp(durationTicks - syncedTicks, 0, durationTicks);
			clientStompAnimationTicks = syncedTicks;
			stompAnimationState.start(tickCount - elapsedTicks);
			clientStompNeedsTimelineAnchor = false;
		} else if (syncedTicks > lastSyncedStompTicks && syncedTicks > clientStompAnimationTicks + 2) {
			startClientStompAnimation(syncedTicks, isCurrentStompFast());
		}
		lastSyncedStompTicks = syncedTicks;

		if (clientStompAnimationTicks > 0) {
			stompAnimationState.startIfStopped(tickCount);
			clientStompAnimationTicks--;
		} else {
			stompAnimationState.stop();
		}
	}

	private void startClientStompAnimation(int ticks, boolean fast) {
		if (!level().isClientSide())
			return;

		stopClientChainAnimationStates();
		stopClientFingerOfDeathAnimationStates();
		clientHornsAnimationTicks = 0;
		hornsAnimationState.stop();
		clientFearAnimationTicks = 0;
		fearAnimationState.stop();
		clientDisarmAnimationTicks = 0;
		disarmAnimationState.stop();
		clientClawAnimationTicks = 0;
		clawAnimationState.stop();
		clientDesolationAnimationTicks = 0;
		desolationAnimationState.stop();
		clientStompAnimationTicks = Math.max(clientStompAnimationTicks, Math.max(1, ticks));
		clientStompFast = fast;
		stompAnimationState.start(tickCount);
	}

	private void updateClientHornsAnimation() {
		if (!isAlive()) {
			clientHornsAnimationTicks = 0;
			hornsAnimationState.stop();
			return;
		}

		int syncedTicks = getHornsCastTicks();
		if (syncedTicks > lastSyncedHornsTicks && syncedTicks > clientHornsAnimationTicks + 2) {
			startClientHornsAnimation(syncedTicks);
		}
		lastSyncedHornsTicks = syncedTicks;

		if (clientHornsAnimationTicks > 0) {
			hornsAnimationState.startIfStopped(tickCount);
			clientHornsAnimationTicks--;
		} else {
			hornsAnimationState.stop();
		}
	}

	private void startClientHornsAnimation(int ticks) {
		if (!level().isClientSide())
			return;

		stopClientChainAnimationStates();
		stopClientFingerOfDeathAnimationStates();
		clientStompAnimationTicks = 0;
		stompAnimationState.stop();
		clientFearAnimationTicks = 0;
		fearAnimationState.stop();
		clientDisarmAnimationTicks = 0;
		disarmAnimationState.stop();
		clientClawAnimationTicks = 0;
		clawAnimationState.stop();
		clientDesolationAnimationTicks = 0;
		desolationAnimationState.stop();
		clientHornsAnimationTicks = Math.max(clientHornsAnimationTicks, Math.max(1, ticks));
		hornsAnimationState.start(tickCount);
	}

	private void updateClientFearAnimation() {
		if (!isAlive()) {
			clientFearAnimationTicks = 0;
			fearAnimationState.stop();
			return;
		}

		int syncedTicks = getFearCastTicks();
		if (syncedTicks > lastSyncedFearTicks && syncedTicks > clientFearAnimationTicks + 2) {
			startClientFearAnimation(syncedTicks);
		}
		lastSyncedFearTicks = syncedTicks;

		if (clientFearAnimationTicks > 0) {
			fearAnimationState.startIfStopped(tickCount);
			clientFearAnimationTicks--;
		} else {
			fearAnimationState.stop();
		}
	}

	private void startClientFearAnimation(int ticks) {
		if (!level().isClientSide())
			return;

		stopClientChainAnimationStates();
		stopClientFingerOfDeathAnimationStates();
		clientStompAnimationTicks = 0;
		stompAnimationState.stop();
		clientHornsAnimationTicks = 0;
		hornsAnimationState.stop();
		clientDisarmAnimationTicks = 0;
		disarmAnimationState.stop();
		clientClawAnimationTicks = 0;
		clawAnimationState.stop();
		clientDesolationAnimationTicks = 0;
		desolationAnimationState.stop();
		clientFearAnimationTicks = Math.max(clientFearAnimationTicks, Math.max(1, ticks));
		fearAnimationState.start(tickCount);
	}

	private void updateClientDisarmAnimation() {
		if (!isAlive()) {
			clientDisarmAnimationTicks = 0;
			disarmAnimationState.stop();
			return;
		}

		int syncedTicks = getDisarmCastTicks();
		if (syncedTicks > lastSyncedDisarmTicks && syncedTicks > clientDisarmAnimationTicks + 2) {
			startClientDisarmAnimation(syncedTicks);
		}
		lastSyncedDisarmTicks = syncedTicks;

		if (clientDisarmAnimationTicks > 0) {
			disarmAnimationState.startIfStopped(tickCount);
			clientDisarmAnimationTicks--;
		} else {
			disarmAnimationState.stop();
		}
	}

	private void startClientDisarmAnimation(int ticks) {
		if (!level().isClientSide())
			return;

		stopClientChainAnimationStates();
		stopClientFingerOfDeathAnimationStates();
		clientStompAnimationTicks = 0;
		stompAnimationState.stop();
		clientHornsAnimationTicks = 0;
		hornsAnimationState.stop();
		clientFearAnimationTicks = 0;
		fearAnimationState.stop();
		clientClawAnimationTicks = 0;
		clawAnimationState.stop();
		clientDesolationAnimationTicks = 0;
		desolationAnimationState.stop();
		clientDisarmAnimationTicks = Math.max(clientDisarmAnimationTicks, Math.max(1, ticks));
		disarmAnimationState.start(tickCount);
	}

	private void updateClientClawAnimation() {
		if (!isAlive()) {
			clientClawAnimationTicks = 0;
			clawAnimationState.stop();
			return;
		}

		int syncedTicks = getClawCastTicks();
		if (syncedTicks > lastSyncedClawTicks && syncedTicks > clientClawAnimationTicks + 2)
			startClientClawAnimation(syncedTicks, entityData.get(CLAW_CAST_INDEX));
		lastSyncedClawTicks = syncedTicks;

		if (clientClawAnimationTicks > 0) {
			clawAnimationState.startIfStopped(tickCount);
			clientClawAnimationTicks--;
		} else {
			clawAnimationState.stop();
		}
	}

	private void startClientClawAnimation(int ticks, int castIndex) {
		if (!level().isClientSide())
			return;

		stopClientChainAnimationStates();
		stopClientFingerOfDeathAnimationStates();
		clientStompAnimationTicks = 0;
		stompAnimationState.stop();
		clientHornsAnimationTicks = 0;
		hornsAnimationState.stop();
		clientFearAnimationTicks = 0;
		fearAnimationState.stop();
		clientDisarmAnimationTicks = 0;
		disarmAnimationState.stop();
		clientDesolationAnimationTicks = 0;
		desolationAnimationState.stop();
		clientClawCastIndex = Mth.clamp(castIndex, 1, 3);
		clientClawAnimationTicks = Math.max(clientClawAnimationTicks, Math.max(1, ticks));
		clawAnimationState.start(tickCount);
	}

	private void updateClientDesolationAnimation() {
		if (!isAlive()) {
			clientDesolationAnimationTicks = 0;
			desolationAnimationState.stop();
			return;
		}

		int syncedTicks = getDesolationCastTicks();
		if (syncedTicks > lastSyncedDesolationTicks && syncedTicks > clientDesolationAnimationTicks + 2)
			startClientDesolationAnimation(syncedTicks);
		lastSyncedDesolationTicks = syncedTicks;

		if (clientDesolationAnimationTicks > 0) {
			desolationAnimationState.startIfStopped(tickCount);
			clientDesolationAnimationTicks--;
		} else {
			desolationAnimationState.stop();
		}
	}

	private void startClientDesolationAnimation(int ticks) {
		if (!level().isClientSide())
			return;

		stopClientChainAnimationStates();
		stopClientFingerOfDeathAnimationStates();
		clientStompAnimationTicks = 0;
		stompAnimationState.stop();
		clientHornsAnimationTicks = 0;
		hornsAnimationState.stop();
		clientFearAnimationTicks = 0;
		fearAnimationState.stop();
		clientDisarmAnimationTicks = 0;
		disarmAnimationState.stop();
		clientClawAnimationTicks = 0;
		clawAnimationState.stop();
		clientDesolationAnimationTicks = Math.max(clientDesolationAnimationTicks, Math.max(1, ticks));
		desolationAnimationState.start(tickCount);
	}

	private void updateClientChainAnimation() {
		if (!isAlive()) {
			stopClientChainAnimationStates();
			return;
		}

		int syncedTicks = getChainCastTicks();
		int syncedPhase = entityData.get(CHAIN_PHASE);
		if (syncedTicks > 0 && (syncedPhase != clientChainPhase || syncedTicks > lastSyncedChainTicks || syncedTicks > clientChainAnimationTicks + 2))
			startClientChainAnimation(syncedTicks, syncedPhase);
		lastSyncedChainTicks = syncedTicks;

		if (clientChainAnimationTicks > 0) {
			if (clientChainPhase == CHAIN_PHASE_SUCCESS) {
				chainSuccessAnimationState.startIfStopped(tickCount);
			} else if (clientChainPhase == CHAIN_PHASE_FAILED) {
				chainFailedAnimationState.startIfStopped(tickCount);
			} else {
				chainAnimationState.startIfStopped(tickCount);
			}
			clientChainAnimationTicks--;
		} else {
			stopClientChainAnimationStates();
		}
	}

	private void startClientChainAnimation(int ticks, int phase) {
		if (!level().isClientSide())
			return;

		stopClientFingerOfDeathAnimationStates();
		clientStompAnimationTicks = 0;
		stompAnimationState.stop();
		clientHornsAnimationTicks = 0;
		hornsAnimationState.stop();
		clientFearAnimationTicks = 0;
		fearAnimationState.stop();
		clientDisarmAnimationTicks = 0;
		disarmAnimationState.stop();
		clientClawAnimationTicks = 0;
		clawAnimationState.stop();
		clientDesolationAnimationTicks = 0;
		desolationAnimationState.stop();
		stopClientChainAnimationStates();
		clientChainPhase = phase;
		clientChainAnimationTicks = Math.max(clientChainAnimationTicks, Math.max(1, ticks));
		if (phase == CHAIN_PHASE_SUCCESS) {
			chainSuccessAnimationState.start(tickCount);
		} else if (phase == CHAIN_PHASE_FAILED) {
			chainFailedAnimationState.start(tickCount);
		} else {
			chainAnimationState.start(tickCount);
		}
	}

	private void stopClientChainAnimationStates() {
		clientChainAnimationTicks = 0;
		clientChainPhase = CHAIN_PHASE_NONE;
		chainAnimationState.stop();
		chainSuccessAnimationState.stop();
		chainFailedAnimationState.stop();
	}

	private void updateClientFingerOfDeathAnimation() {
		if (!isAlive()) {
			stopClientFingerOfDeathAnimationStates();
			return;
		}

		int syncedTicks = getFingerOfDeathCastTicks();
		int syncedPhase = entityData.get(FINGER_OF_DEATH_PHASE);
		if (syncedTicks > 0 && (syncedPhase != clientFingerOfDeathPhase || syncedTicks > lastSyncedFingerOfDeathTicks || syncedTicks > clientFingerOfDeathAnimationTicks + 2))
			startClientFingerOfDeathAnimation(syncedTicks, syncedPhase);
		lastSyncedFingerOfDeathTicks = syncedTicks;

		if (clientFingerOfDeathAnimationTicks > 0) {
			if (clientFingerOfDeathPhase == FINGER_OF_DEATH_PHASE_CHARGE) {
				fingerOfDeathChargeAnimationState.startIfStopped(tickCount);
			} else if (clientFingerOfDeathPhase == FINGER_OF_DEATH_PHASE_IDLE) {
				fingerOfDeathIdleAnimationState.startIfStopped(tickCount);
			} else if (clientFingerOfDeathPhase == FINGER_OF_DEATH_PHASE_SHOT) {
				fingerOfDeathShotAnimationState.startIfStopped(tickCount);
			}
			clientFingerOfDeathAnimationTicks--;
		} else {
			stopClientFingerOfDeathAnimationStates();
		}
	}

	private void startClientFingerOfDeathAnimation(int ticks, int phase) {
		if (!level().isClientSide())
			return;

		clientStompAnimationTicks = 0;
		stompAnimationState.stop();
		clientHornsAnimationTicks = 0;
		hornsAnimationState.stop();
		clientFearAnimationTicks = 0;
		fearAnimationState.stop();
		clientDisarmAnimationTicks = 0;
		disarmAnimationState.stop();
		clientClawAnimationTicks = 0;
		clawAnimationState.stop();
		clientDesolationAnimationTicks = 0;
		desolationAnimationState.stop();
		stopClientChainAnimationStates();
		stopClientFingerOfDeathAnimationStates();
		clientFingerOfDeathPhase = phase;
		clientFingerOfDeathAnimationTicks = Math.max(clientFingerOfDeathAnimationTicks, Math.max(1, ticks));
		if (phase == FINGER_OF_DEATH_PHASE_CHARGE) {
			fingerOfDeathChargeAnimationState.start(tickCount);
		} else if (phase == FINGER_OF_DEATH_PHASE_IDLE) {
			fingerOfDeathIdleAnimationState.start(tickCount);
		} else if (phase == FINGER_OF_DEATH_PHASE_SHOT) {
			fingerOfDeathShotAnimationState.start(tickCount);
		}
	}

	private void stopClientFingerOfDeathAnimationStates() {
		clientFingerOfDeathAnimationTicks = 0;
		clientFingerOfDeathPhase = FINGER_OF_DEATH_PHASE_NONE;
		fingerOfDeathChargeAnimationState.stop();
		fingerOfDeathIdleAnimationState.stop();
		fingerOfDeathShotAnimationState.stop();
	}

	private void syncClientFingerOfDeathBodyRotation() {
		if (!level().isClientSide() || (!isFingerOfDeathIdleAnimationActive() && !isFingerOfDeathShotAnimationActive()))
			return;

		float laserYaw = getFingerOfDeathLaserYaw();
		float laserPitch = Mth.clamp(getFingerOfDeathLaserPitch(), -70.0F, 55.0F);
		setYRot(laserYaw);
		yRotO = laserYaw;
		setXRot(laserPitch);
		xRotO = laserPitch;
		yBodyRot = laserYaw;
		yBodyRotO = laserYaw;
		yHeadRot = laserYaw;
		yHeadRotO = laserYaw;
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == STOMP_ANIMATION_EVENT || id == FAST_STOMP_ANIMATION_EVENT) {
			boolean fast = id == FAST_STOMP_ANIMATION_EVENT;
			int durationTicks = DebtlordStompGoal.getStompCastDurationTicks(fast);
			startClientStompAnimation(durationTicks, fast);
			clientStompNeedsTimelineAnchor = true;
			lastSyncedStompTicks = Math.max(lastSyncedStompTicks, durationTicks);
			return;
		}
		if (id == STOMP_STOP_ANIMATION_EVENT) {
			clientStompAnimationTicks = 0;
			clientStompFast = false;
			clientStompNeedsTimelineAnchor = false;
			lastSyncedStompTicks = Integer.MAX_VALUE;
			stompAnimationState.stop();
			return;
		}
		if (id == HORNS_ANIMATION_EVENT) {
			startClientHornsAnimation(DebtlordHornsGoal.CAST_DURATION_TICKS);
			lastSyncedHornsTicks = Math.max(lastSyncedHornsTicks, DebtlordHornsGoal.CAST_DURATION_TICKS);
			return;
		}
		if (id == FEAR_ANIMATION_EVENT) {
			startClientFearAnimation(DebtlordFearGoal.CAST_DURATION_TICKS);
			lastSyncedFearTicks = Math.max(lastSyncedFearTicks, DebtlordFearGoal.CAST_DURATION_TICKS);
			return;
		}
		if (id == DISARM_ANIMATION_EVENT) {
			startClientDisarmAnimation(DebtlordDisarmAbility.CAST_DURATION_TICKS);
			lastSyncedDisarmTicks = Math.max(lastSyncedDisarmTicks, DebtlordDisarmAbility.CAST_DURATION_TICKS);
			return;
		}
		if (id == CLAW_ANIMATION_EVENT) {
			startClientClawAnimation(DebtlordClawGoal.getCastDurationTicks(1), 1);
			lastSyncedClawTicks = Math.max(lastSyncedClawTicks, DebtlordClawGoal.CAST_DURATION_TICKS);
			return;
		}
		if (id == CLAW_SECOND_ANIMATION_EVENT) {
			startClientClawAnimation(DebtlordClawGoal.getCastDurationTicks(2), 2);
			lastSyncedClawTicks = DebtlordClawGoal.getCastDurationTicks(2);
			return;
		}
		if (id == CLAW_THIRD_ANIMATION_EVENT) {
			startClientClawAnimation(DebtlordClawGoal.getCastDurationTicks(3), 3);
			lastSyncedClawTicks = DebtlordClawGoal.getCastDurationTicks(3);
			return;
		}
		if (id == DESOLATION_ANIMATION_EVENT) {
			startClientDesolationAnimation(DebtlordDesolationGoal.CAST_DURATION_TICKS);
			lastSyncedDesolationTicks = Math.max(lastSyncedDesolationTicks, DebtlordDesolationGoal.CAST_DURATION_TICKS);
			return;
		}
		if (id == CHAIN_ANIMATION_EVENT) {
			startClientChainAnimation(DebtlordChainsGoal.CAST_DURATION_TICKS, CHAIN_PHASE_CAST);
			lastSyncedChainTicks = Math.max(lastSyncedChainTicks, DebtlordChainsGoal.CAST_DURATION_TICKS);
			return;
		}
		if (id == CHAIN_SUCCESS_ANIMATION_EVENT) {
			startClientChainAnimation(DebtlordChainsGoal.SUCCESS_DURATION_TICKS, CHAIN_PHASE_SUCCESS);
			lastSyncedChainTicks = Math.max(lastSyncedChainTicks, DebtlordChainsGoal.SUCCESS_DURATION_TICKS);
			return;
		}
		if (id == CHAIN_FAILED_ANIMATION_EVENT) {
			startClientChainAnimation(DebtlordChainsGoal.FAILED_DURATION_TICKS, CHAIN_PHASE_FAILED);
			lastSyncedChainTicks = Math.max(lastSyncedChainTicks, DebtlordChainsGoal.FAILED_DURATION_TICKS);
			return;
		}
		if (id == FINGER_OF_DEATH_CHARGE_ANIMATION_EVENT) {
			startClientFingerOfDeathAnimation(DebtlordFingerOfDeathGoal.CHARGE_DURATION_TICKS, FINGER_OF_DEATH_PHASE_CHARGE);
			lastSyncedFingerOfDeathTicks = Math.max(lastSyncedFingerOfDeathTicks, DebtlordFingerOfDeathGoal.CHARGE_DURATION_TICKS);
			return;
		}
		if (id == FINGER_OF_DEATH_IDLE_ANIMATION_EVENT) {
			startClientFingerOfDeathAnimation(DebtlordFingerOfDeathGoal.LASER_DURATION_TICKS, FINGER_OF_DEATH_PHASE_IDLE);
			lastSyncedFingerOfDeathTicks = Math.max(lastSyncedFingerOfDeathTicks, DebtlordFingerOfDeathGoal.LASER_DURATION_TICKS);
			return;
		}
		if (id == FINGER_OF_DEATH_SHOT_ANIMATION_EVENT) {
			startClientFingerOfDeathAnimation(DebtlordFingerOfDeathGoal.SHOT_DURATION_TICKS, FINGER_OF_DEATH_PHASE_SHOT);
			lastSyncedFingerOfDeathTicks = Math.max(lastSyncedFingerOfDeathTicks, DebtlordFingerOfDeathGoal.SHOT_DURATION_TICKS);
			return;
		}
		if (id == APPEARANCE_ANIMATION_EVENT) {
			startClientAppearanceAnimation(APPEARANCE_DURATION_TICKS);
			lastSyncedAppearanceTicks = Math.max(lastSyncedAppearanceTicks, APPEARANCE_DURATION_TICKS);
			return;
		}
		if (id == TALKING_ANIMATION_EVENT) {
			startClientTalkingAnimation(TALKING_DURATION_TICKS);
			lastSyncedTalkingTicks = Math.max(lastSyncedTalkingTicks, TALKING_DURATION_TICKS);
			return;
		}
		super.handleEntityEvent(id);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		DebtlordClawGoal clawGoal = new DebtlordClawGoal(this);
		this.antiPillarGoal = new DebtlordAntiPillarGoal(this, clawGoal);
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new DebtlordDesolationGoal(this));
		this.goalSelector.addGoal(2, new DebtlordFearGoal(this));
		this.goalSelector.addGoal(2, antiPillarGoal);
		this.goalSelector.addGoal(3, new DebtlordFingerOfDeathGoal(this));
		this.goalSelector.addGoal(4, new DebtlordChainsGoal(this, antiPillarGoal));
		this.goalSelector.addGoal(5, clawGoal);
		this.goalSelector.addGoal(6, new DebtlordHornsGoal(this));
		this.goalSelector.addGoal(7, new DebtlordStompGoal(this));
		this.goalSelector.addGoal(8, new DebtlordLeaveWaterGoal(this, 1.15D));
		this.goalSelector.addGoal(9, new DebtlordApproachTargetGoal(this, 1.0D));
		this.goalSelector.addGoal(10, new WaterAvoidingRandomStrollGoal(this, 1.0D));
		this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 12.0F));
		this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
			@Override
			public boolean canUse() {
				LivingEntity attacker = DebtlordEntity.this.getLastHurtByMob();
				return attacker != null
					&& !DebtlordEntity.this.isAlliedTo(attacker)
					&& super.canUse();
			}
		});
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
		this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
		this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, SnowGolem.class, true));
		this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, target -> target instanceof NeutralMob));
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		return false;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState blockState) {
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		if (shouldShowBossBar())
			bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossEvent.removePlayer(player);
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	protected float getWaterSlowDown() {
		return 0.98F;
	}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.generic.hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return TimothatysTrinketsModSounds.DEBTLORD_DEATH.get();
	}

	@Override
	public float getVoicePitch() {
		return isDeadOrDying() ? 1.0F : super.getVoicePitch();
	}

	@Override
	public boolean hurt(DamageSource damagesource, float amount) {
		if (isAltarSummoned() && getAltarSummonState() != ALTAR_SUMMON_STATE_FIGHT)
			return false;
		if (damagesource.is(DamageTypes.FALL))
			return false;
		if (damagesource.is(DamageTypes.CACTUS))
			return false;
		if (damagesource.is(DamageTypes.DRAGON_BREATH))
			return false;
		if (damagesource.is(DamageTypes.MAGIC) || damagesource.is(DamageTypes.INDIRECT_MAGIC))
			amount *= MAGIC_DAMAGE_MULTIPLIER;
		boolean wasHurt = super.hurt(damagesource, amount);
		if (wasHurt && isAlive() && !desolationAbilityUsed && isEnraged())
			desolationAbilityPending = true;
		if (wasHurt && isAlive() && damagesource.getEntity() instanceof Player player) {
			registerFearTriggerHit(player);
		}
		return wasHurt;
	}

	@Override
	public void die(DamageSource damageSource) {
		if (antiPillarGoal != null)
			antiPillarGoal.forceCancel();
		if (isAltarSummoned()) {
			if (getAltarSummonState() == ALTAR_SUMMON_STATE_FIGHT) {
				DebtlordSummonManager.completeWithVictory(this);
			} else {
				DebtlordSummonManager.releaseAltarLock(this);
				markAltarOutcomeHandled();
			}
			hideBossBar();
		}
		super.die(damageSource);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putString("Texture", isUsingFear() && textureBeforeFear != null ? textureBeforeFear : this.getTexture());
		compound.putBoolean("DesolationAbilityUsed", desolationAbilityUsed);
		compound.putBoolean("SecondPhaseDialoguePlayed", secondPhaseDialoguePlayed);
		compound.putBoolean("AltarSummoned", isAltarSummoned());
		compound.putInt("AltarSummonState", getAltarSummonState());
		compound.putInt("AltarSummonTicks", getAltarSummonTicks());
		compound.putBoolean("AltarOutcomeHandled", altarOutcomeHandled);
		if (isAltarSummoned())
			compound.putLong("AltarPos", entityData.get(ALTAR_POS).asLong());
		if (altarSummonerUuid != null)
			compound.putUUID("AltarSummoner", altarSummonerUuid);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		noPhysics = false;
		setNoGravity(false);
		entityData.set(ANTI_PILLAR_PHASE, DebtlordAntiPillarGoal.PHASE_NONE);
		entityData.set(ANTI_PILLAR_TICKS, 0);
		if (compound.contains("Texture"))
			this.setTexture(compound.getString("Texture"));
		desolationAbilityUsed = compound.getBoolean("DesolationAbilityUsed");
		secondPhaseDialoguePlayed = compound.getBoolean("SecondPhaseDialoguePlayed");
		desolationAbilityPending = !desolationAbilityUsed && isEnraged();
		if (compound.getBoolean("AltarSummoned")) {
			entityData.set(ALTAR_SUMMONED, true);
			if (compound.contains("AltarPos"))
				entityData.set(ALTAR_POS, BlockPos.of(compound.getLong("AltarPos")));
			entityData.set(ALTAR_SUMMON_STATE, compound.getInt("AltarSummonState"));
			entityData.set(ALTAR_SUMMON_TICKS, compound.getInt("AltarSummonTicks"));
			if (compound.hasUUID("AltarSummoner"))
				altarSummonerUuid = compound.getUUID("AltarSummoner");
			altarOutcomeHandled = compound.getBoolean("AltarOutcomeHandled");
			if (isAltarIntroOrDismissalActive()) {
				setNoAi(true);
				setInvulnerable(true);
				hideBossBar();
			}
		}
	}

	public static void init(RegisterSpawnPlacementsEvent event) {
		event.register(TimothatysTrinketsModEntities.DEBTLORD.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, reason, pos, random) -> (reason != MobSpawnType.NATURAL && reason != MobSpawnType.CHUNK_GENERATION && world.getDifficulty() != Difficulty.PEACEFUL && Monster.isDarkEnoughToSpawn(world, pos, random) && Mob.checkMobSpawnRules(entityType, world, reason, pos, random)),
				RegisterSpawnPlacementsEvent.Operation.REPLACE);
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.2);
		builder = builder.add(Attributes.MAX_HEALTH, 550);
		builder = builder.add(Attributes.ARMOR, 10);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 0);
		builder = builder.add(Attributes.FOLLOW_RANGE, 48);
		builder = builder.add(Attributes.STEP_HEIGHT, 0.6);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 1);
		builder = builder.add(Attributes.WATER_MOVEMENT_EFFICIENCY, 0.9);
		return builder;
	}
}
