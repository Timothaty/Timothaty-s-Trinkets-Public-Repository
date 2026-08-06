package net.timothaty.timothatystrinkets.potion;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Skeleton;

public class UndeadificationMobEffect extends MobEffect {
	private static final float TRANSFORM_CHANCE = 0.3F;
	private static final int LOOP_SOUND_INTERVAL_TICKS = 60;
	private static final float ROTTEN_CHUNK_SPAWN_CHANCE = 0.14F;
	private static final double ROTTEN_CHUNK_HORIZONTAL_OFFSET = 0.16D;
	private static final double ROTTEN_CHUNK_START_Y_OFFSET = 0.22D;
	private static final double ROTTEN_CHUNK_HORIZONTAL_SPEED = 0.05D;
	private static final int ROTTEN_CHUNK_MIN_BURST = 2;
	private static final int ROTTEN_CHUNK_MAX_BURST = 4;
	private static final float LOOP_SOUND_VOLUME = 0.4F;
	private static final float LOOP_SOUND_PITCH = 1.0F;
	private static final double START_VFX_Y_OFFSET = 0.06D;
	private static final double MOVEMENT_SPEED_REDUCTION = -0.4D;

	private static final TagKey<EntityType<?>> UNDEADIFY_RESTRICTED = TagKey.create(
		Registries.ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "undeadify_restricted")
	);

	public UndeadificationMobEffect() {
		super(MobEffectCategory.HARMFUL, -16637663);
		this.withSoundOnAdded(BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("timothatys_trinkets:undeadification_start")));
		this.addAttributeModifier(
			Attributes.MOVEMENT_SPEED,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "undeadification_movement_speed"),
			MOVEMENT_SPEED_REDUCTION,
			AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
		);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public void onEffectStarted(LivingEntity entity, int amplifier) {
		if (entity.level() instanceof ServerLevel serverLevel) {
			spawnStartVfx(serverLevel, entity);
		}
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		if (isUndeadOrRestricted(entity)) {
			return false;
		}

		if (entity.level() instanceof ServerLevel serverLevel) {
			MobEffectInstance current = entity.getEffect(TimothatysTrinketsModMobEffects.UNDEADIFICATION);
			if (current == null) {
				return super.applyEffectTick(entity, amplifier);
			}

			if (current.getDuration() <= 1) {
				handleCompletion(serverLevel, entity);
				return super.applyEffectTick(entity, amplifier);
			}

			playLoopSound(serverLevel, entity, current.getDuration());
			spawnRottenChunkParticles(serverLevel, entity);
		}

		return super.applyEffectTick(entity, amplifier);
	}

	@Override
	public void onMobRemoved(LivingEntity entity, int amplifier, Entity.RemovalReason reason) {
		if (entity.level() instanceof ServerLevel serverLevel) {
			stopLoopSound(serverLevel, entity);
		}
		super.onMobRemoved(entity, amplifier, reason);
	}

	private static boolean isUndeadOrRestricted(LivingEntity entity) {
		return entity.getType().is(EntityTypeTags.SENSITIVE_TO_SMITE)
			|| entity.getType().is(UNDEADIFY_RESTRICTED);
	}

	private static void playLoopSound(ServerLevel serverLevel, LivingEntity entity, int effectDuration) {
		if (effectDuration % LOOP_SOUND_INTERVAL_TICKS != 0) {
			return;
		}

		serverLevel.playSound(
			null,
			entity.getX(),
			entity.getY(),
			entity.getZ(),
			TimothatysTrinketsModSounds.UNDEADIFICATION_LOOP.get(),
			SoundSource.HOSTILE,
			LOOP_SOUND_VOLUME,
			LOOP_SOUND_PITCH
		);
	}

	private static void stopLoopSound(ServerLevel serverLevel, LivingEntity entity) {
		serverLevel.getServer().getPlayerList().broadcast(
			null,
			entity.getX(),
			entity.getY(),
			entity.getZ(),
			48.0D,
			serverLevel.dimension(),
			new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(
				TimothatysTrinketsModSounds.UNDEADIFICATION_LOOP.get().getLocation(),
				SoundSource.HOSTILE
			)
		);
	}

	private static void spawnStartVfx(ServerLevel serverLevel, LivingEntity entity) {
		serverLevel.sendParticles(
			TimothatysTrinketsModParticleTypes.UNDEADIFICATION_PARTICLE_VFX.get(),
			entity.getX(),
			entity.getY() + START_VFX_Y_OFFSET,
			entity.getZ(),
			1,
			0.0D,
			0.0D,
			0.0D,
			0.0D
		);
	}

	private static void spawnRottenChunkParticles(ServerLevel serverLevel, LivingEntity entity) {
		if (entity.tickCount % 4 != 0) {
			return;
		}

		if (serverLevel.random.nextFloat() >= ROTTEN_CHUNK_SPAWN_CHANCE) {
			return;
		}

		int burstCount = Mth.nextInt(serverLevel.random, ROTTEN_CHUNK_MIN_BURST, ROTTEN_CHUNK_MAX_BURST);
		double bodyRadius = Math.max(entity.getBbWidth() * 0.45D, ROTTEN_CHUNK_HORIZONTAL_OFFSET);
		double bodyHeight = Math.max(entity.getBbHeight(), 0.6D);

		for (int i = 0; i < burstCount; i++) {
			double angle = serverLevel.random.nextDouble() * (Math.PI * 2.0D);
			double radius = Mth.nextDouble(serverLevel.random, bodyRadius * 0.2D, bodyRadius);
			double spawnX = entity.getX() + Math.cos(angle) * radius;
			double spawnY = entity.getY() + Mth.nextDouble(serverLevel.random, bodyHeight * 0.15D, bodyHeight * 0.9D);
			double spawnZ = entity.getZ() + Math.sin(angle) * radius;

			double motionX = Math.cos(angle) * Mth.nextDouble(serverLevel.random, 0.01D, ROTTEN_CHUNK_HORIZONTAL_SPEED);
			double motionY = Mth.nextDouble(serverLevel.random, 0.01D, ROTTEN_CHUNK_START_Y_OFFSET);
			double motionZ = Math.sin(angle) * Mth.nextDouble(serverLevel.random, 0.01D, ROTTEN_CHUNK_HORIZONTAL_SPEED);

			serverLevel.sendParticles(
				TimothatysTrinketsModParticleTypes.ROTTEN_CHUNK.get(),
				spawnX,
				spawnY,
				spawnZ,
				0,
				motionX,
				motionY,
				motionZ,
				1.0D
			);
		}
	}

	private static void handleCompletion(ServerLevel serverLevel, LivingEntity entity) {
		stopLoopSound(serverLevel, entity);

		boolean transformed = false;
		if (serverLevel.random.nextFloat() < TRANSFORM_CHANCE) {
			Skeleton skeleton = EntityType.SKELETON.create(serverLevel);
			if (skeleton != null) {
				skeleton.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
				serverLevel.addFreshEntity(skeleton);
				entity.discard();
				transformed = true;
			}
		}

		if (!transformed) {
			entity.kill();
		}

		spawnCompletionParticles(serverLevel, entity);
		serverLevel.playSound(
			null,
			entity.blockPosition(),
			transformed ? TimothatysTrinketsModSounds.UNDEADIFICATION_SUCCESFUL.get() : TimothatysTrinketsModSounds.UNDEADIFICATION_FAILED.get(),
			SoundSource.HOSTILE,
			1.0F,
			1.0F
		);
	}

	private static void spawnCompletionParticles(ServerLevel serverLevel, LivingEntity entity) {
		serverLevel.sendParticles(
			TimothatysTrinketsModParticleTypes.BLIGHTED_DUST.get(),
			entity.getX(),
			entity.getY() + entity.getBbHeight() * 0.5D,
			entity.getZ(),
			85,
			0.35D,
			0.45D,
			0.35D,
			0.02D
		);
	}
}
